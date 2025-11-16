# Quick Start Guide - Matrix Bridge Server

**TL;DR** - Fast track setup for experienced users.

## Prerequisites
- Ubuntu 20.04/22.04 server
- Domain: `matrix.yourdomain.com` → Your server IP
- Min: 2 CPU, 2GB RAM, 20GB disk

---

## 1. Install Dependencies (5 min)

```bash
# System updates
sudo apt update && sudo apt upgrade -y

# Install essentials
sudo apt install -y postgresql nginx certbot python3-certbot-nginx git build-essential

# Install Go 1.21+
wget https://go.dev/dl/go1.21.5.linux-amd64.tar.gz
sudo tar -C /usr/local -xzf go1.21.5.linux-amd64.tar.gz
echo 'export PATH=$PATH:/usr/local/go/bin' >> ~/.bashrc
source ~/.bashrc
```

---

## 2. Setup PostgreSQL (3 min)

```bash
sudo -u postgres psql << EOF
CREATE USER synapse WITH PASSWORD 'CHANGE_ME_1';
CREATE DATABASE synapse ENCODING 'UTF8' LC_COLLATE='C' LC_CTYPE='C' template=template0 OWNER synapse;
CREATE USER mautrix_whatsapp WITH PASSWORD 'CHANGE_ME_2';
CREATE DATABASE mautrix_whatsapp OWNER mautrix_whatsapp;
\q
EOF
```

---

## 3. Install Matrix Synapse (5 min)

```bash
# Add Matrix repo
sudo wget -O /usr/share/keyrings/matrix-org-archive-keyring.gpg \
  https://packages.matrix.org/debian/matrix-org-archive-keyring.gpg

echo "deb [signed-by=/usr/share/keyrings/matrix-org-archive-keyring.gpg] https://packages.matrix.org/debian/ $(lsb_release -cs) main" | \
  sudo tee /etc/apt/sources.list.d/matrix-org.list

# Install
sudo apt update
sudo apt install -y matrix-synapse-py3

# Generate config
cd /etc/matrix-synapse
sudo python3 -m synapse.app.homeserver \
  --server-name matrix.yourdomain.com \
  --config-path homeserver.yaml \
  --generate-config \
  --report-stats=no
```

**Edit config:**
```bash
sudo nano /etc/matrix-synapse/homeserver.yaml
```

**Key changes:**
```yaml
server_name: "matrix.yourdomain.com"
public_baseurl: "https://matrix.yourdomain.com/"

database:
  name: psycopg2
  args:
    user: synapse
    password: CHANGE_ME_1
    database: synapse
    host: localhost

enable_registration: true  # Disable after creating users

app_service_config_files:
  - /etc/matrix-synapse/whatsapp-registration.yaml
```

---

## 4. Install mautrix-whatsapp (5 min)

```bash
# Create user and directory
sudo useradd -m -s /bin/bash mautrix-whatsapp
sudo mkdir -p /opt/mautrix-whatsapp
sudo chown mautrix-whatsapp:mautrix-whatsapp /opt/mautrix-whatsapp

# Clone and build
cd /opt/mautrix-whatsapp
sudo -u mautrix-whatsapp git clone https://github.com/mautrix/whatsapp.git .
sudo -u mautrix-whatsapp go build -o mautrix-whatsapp

# Generate config
sudo -u mautrix-whatsapp ./mautrix-whatsapp -g
```

**Edit config:**
```bash
sudo nano /opt/mautrix-whatsapp/config.yaml
```

**Key changes:**
```yaml
homeserver:
    address: http://localhost:8008
    domain: matrix.yourdomain.com

appservice:
    address: http://localhost:29318
    port: 29318
    database:
        type: postgres
        uri: postgres://mautrix_whatsapp:CHANGE_ME_2@localhost/mautrix_whatsapp?sslmode=disable
    id: whatsapp
    bot:
        username: whatsappbot

bridge:
    username_template: "whatsapp_{{.}}"
    permissions:
        "matrix.yourdomain.com": user
        "@admin:matrix.yourdomain.com": admin
```

**Generate registration:**
```bash
sudo -u mautrix-whatsapp ./mautrix-whatsapp -g -r
sudo cp registration.yaml /etc/matrix-synapse/whatsapp-registration.yaml
sudo chown matrix-synapse:matrix-synapse /etc/matrix-synapse/whatsapp-registration.yaml
```

---

## 5. Create Systemd Service (2 min)

```bash
sudo nano /etc/systemd/system/mautrix-whatsapp.service
```

```ini
[Unit]
Description=mautrix-whatsapp bridge
After=network.target matrix-synapse.service

[Service]
Type=simple
User=mautrix-whatsapp
WorkingDirectory=/opt/mautrix-whatsapp
ExecStart=/opt/mautrix-whatsapp/mautrix-whatsapp
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

**Start services:**
```bash
sudo systemctl daemon-reload
sudo systemctl restart matrix-synapse
sudo systemctl enable --now mautrix-whatsapp
```

---

## 6. Setup NGINX + SSL (5 min)

```bash
sudo nano /etc/nginx/sites-available/matrix
```

**Minimal config:**
```nginx
server {
    listen 80;
    server_name matrix.yourdomain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    listen 8448 ssl http2;
    server_name matrix.yourdomain.com;

    ssl_certificate /etc/letsencrypt/live/matrix.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/matrix.yourdomain.com/privkey.pem;

    location ~ ^(/_matrix|/_synapse/client) {
        proxy_pass http://localhost:8008;
        proxy_set_header X-Forwarded-For $remote_addr;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Host $host;
        client_max_body_size 50M;
    }
}
```

**Enable and get SSL:**
```bash
sudo ln -s /etc/nginx/sites-available/matrix /etc/nginx/sites-enabled/
sudo certbot --nginx -d matrix.yourdomain.com
sudo nginx -t && sudo systemctl reload nginx
```

---

## 7. Create Admin User (2 min)

```bash
register_new_matrix_user -c /etc/matrix-synapse/homeserver.yaml http://localhost:8008
```

**Follow prompts:**
- Username: `admin`
- Password: `your_secure_password`
- Make admin: `yes`

---

## 8. Test Everything (3 min)

```bash
# Test homeserver
curl https://matrix.yourdomain.com/_matrix/client/versions

# Test bridge bot
curl http://localhost:8008/_matrix/client/r0/profile/@whatsappbot:matrix.yourdomain.com

# Check logs
sudo journalctl -u matrix-synapse -f
sudo journalctl -u mautrix-whatsapp -f
```

---

## 9. Link WhatsApp via Android App

1. **Open MultiWhatsApp app**
2. **Login:**
   - Homeserver: `https://matrix.yourdomain.com`
   - Username: `admin`
   - Password: Your password
3. **QR code appears** - scan with WhatsApp
4. **Done!** Chats appear automatically

---

## Common Issues

### Bridge not starting?
```bash
sudo journalctl -u mautrix-whatsapp -n 50
# Check database credentials in config.yaml
```

### Can't login from app?
```bash
curl https://matrix.yourdomain.com/_matrix/client/versions
# Should return JSON. If not, check NGINX/SSL
```

### QR code not appearing?
```bash
# Message bridge bot: @whatsappbot:matrix.yourdomain.com
# Send: login
# Check logs: sudo journalctl -u mautrix-whatsapp -f
```

---

## Firewall Setup

```bash
sudo ufw allow 22/tcp   # SSH
sudo ufw allow 80/tcp   # HTTP
sudo ufw allow 443/tcp  # HTTPS
sudo ufw allow 8448/tcp # Matrix Federation
sudo ufw enable
```

---

## Maintenance Commands

```bash
# Restart services
sudo systemctl restart matrix-synapse
sudo systemctl restart mautrix-whatsapp

# View logs
sudo journalctl -u matrix-synapse -f
sudo journalctl -u mautrix-whatsapp -f

# Update bridge
cd /opt/mautrix-whatsapp
sudo -u mautrix-whatsapp git pull
sudo -u mautrix-whatsapp go build -o mautrix-whatsapp
sudo systemctl restart mautrix-whatsapp
```

---

## Security Checklist

- [ ] Strong passwords for database users
- [ ] SSL certificate installed and auto-renewing
- [ ] Firewall enabled (ufw)
- [ ] Registration disabled after creating users
- [ ] Regular backups scheduled
- [ ] Monitoring set up (optional)

---

**Total Setup Time: ~30 minutes**

For detailed explanations, see [SERVER_SETUP.md](SERVER_SETUP.md)
