# Matrix Bridge Server Setup Guide

Complete guide to setting up a Matrix homeserver with mautrix-whatsapp bridge for the MultiWhatsApp Android app.

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Server Setup](#server-setup)
3. [Install Matrix Synapse](#install-matrix-synapse)
4. [Install PostgreSQL](#install-postgresql)
5. [Configure Synapse](#configure-synapse)
6. [Install mautrix-whatsapp Bridge](#install-mautrix-whatsapp-bridge)
7. [Configure the Bridge](#configure-the-bridge)
8. [Connect Bridge to Synapse](#connect-bridge-to-synapse)
9. [Setup Reverse Proxy (NGINX)](#setup-reverse-proxy-nginx)
10. [SSL Certificate](#ssl-certificate)
11. [Testing](#testing)
12. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Server Requirements
- **VPS or Dedicated Server** (minimum specs):
  - 2 CPU cores
  - 2GB RAM (4GB recommended)
  - 20GB disk space
  - Ubuntu 20.04/22.04 LTS or Debian 11/12

### Domain & DNS
- **Domain name** (e.g., `yourdomain.com`)
- **DNS records** configured:
  ```
  matrix.yourdomain.com    A      YOUR_SERVER_IP
  ```

### Software
- SSH access to your server
- Root or sudo privileges

---

## Server Setup

### 1. Initial Server Configuration

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install essential tools
sudo apt install -y curl wget git build-essential python3 python3-pip \
    python3-venv libpq-dev postgresql nginx certbot python3-certbot-nginx

# Install Go (required for mautrix-whatsapp)
wget https://go.dev/dl/go1.21.5.linux-amd64.tar.gz
sudo rm -rf /usr/local/go
sudo tar -C /usr/local -xzf go1.21.5.linux-amd64.tar.gz
echo 'export PATH=$PATH:/usr/local/go/bin' >> ~/.bashrc
source ~/.bashrc

# Verify Go installation
go version
```

---

## Install PostgreSQL

### 1. Install PostgreSQL

```bash
sudo apt install -y postgresql postgresql-contrib
```

### 2. Create Database for Synapse

```bash
# Switch to postgres user
sudo -u postgres psql

# In PostgreSQL prompt:
CREATE USER synapse WITH PASSWORD 'your_secure_password_here';
CREATE DATABASE synapse ENCODING 'UTF8' LC_COLLATE='C' LC_CTYPE='C' TEMPLATE=template0 OWNER synapse;
\q
```

### 3. Create Database for mautrix-whatsapp

```bash
sudo -u postgres psql

# In PostgreSQL prompt:
CREATE USER mautrix_whatsapp WITH PASSWORD 'another_secure_password';
CREATE DATABASE mautrix_whatsapp OWNER mautrix_whatsapp;
\q
```

---

## Install Matrix Synapse

### Method 1: Using APT (Recommended)

```bash
# Add Matrix repository
sudo apt install -y lsb-release wget apt-transport-https
sudo wget -O /usr/share/keyrings/matrix-org-archive-keyring.gpg https://packages.matrix.org/debian/matrix-org-archive-keyring.gpg

echo "deb [signed-by=/usr/share/keyrings/matrix-org-archive-keyring.gpg] https://packages.matrix.org/debian/ $(lsb_release -cs) main" | \
    sudo tee /etc/apt/sources.list.d/matrix-org.list

sudo apt update
sudo apt install -y matrix-synapse-py3
```

### Method 2: Using pip (Alternative)

```bash
# Create synapse user
sudo useradd -m -s /bin/bash synapse

# Create virtual environment
sudo -u synapse python3 -m venv /home/synapse/synapse-env
sudo -u synapse /home/synapse/synapse-env/bin/pip install --upgrade pip setuptools
sudo -u synapse /home/synapse/synapse-env/bin/pip install matrix-synapse[postgres]
```

---

## Configure Synapse

### 1. Generate Configuration

```bash
# If installed via APT
cd /etc/matrix-synapse
sudo python3 -m synapse.app.homeserver \
    --server-name matrix.yourdomain.com \
    --config-path homeserver.yaml \
    --generate-config \
    --report-stats=no

# If installed via pip
sudo -u synapse /home/synapse/synapse-env/bin/python -m synapse.app.homeserver \
    --server-name matrix.yourdomain.com \
    --config-path /home/synapse/homeserver.yaml \
    --generate-config \
    --report-stats=no
```

### 2. Edit homeserver.yaml

```bash
sudo nano /etc/matrix-synapse/homeserver.yaml
# or
sudo nano /home/synapse/homeserver.yaml
```

**Key configurations to update:**

```yaml
# Server name
server_name: "matrix.yourdomain.com"

# Public baseurl
public_baseurl: "https://matrix.yourdomain.com/"

# Database (replace SQLite with PostgreSQL)
database:
  name: psycopg2
  args:
    user: synapse
    password: your_secure_password_here
    database: synapse
    host: localhost
    cp_min: 5
    cp_max: 10

# Listeners
listeners:
  - port: 8008
    tls: false
    type: http
    x_forwarded: true
    bind_addresses: ['127.0.0.1']
    resources:
      - names: [client, federation]
        compress: false

# Registration (allow for initial setup, disable later)
enable_registration: true
enable_registration_without_verification: true

# App Service registration files
app_service_config_files:
  - /etc/matrix-synapse/whatsapp-registration.yaml

# Rate limiting (adjust as needed)
rc_message:
  per_second: 10
  burst_count: 50

rc_registration:
  per_second: 0.17
  burst_count: 3

rc_login:
  address:
    per_second: 0.17
    burst_count: 3
  account:
    per_second: 0.17
    burst_count: 3

# Media store
media_store_path: "/var/lib/matrix-synapse/media"

# Maximum upload size (50MB)
max_upload_size: "50M"

# Enable metrics (optional)
enable_metrics: true
metrics_port: 9000
```

### 3. Set Proper Permissions

```bash
sudo chown -R matrix-synapse:matrix-synapse /var/lib/matrix-synapse
sudo chmod 640 /etc/matrix-synapse/homeserver.yaml
```

---

## Install mautrix-whatsapp Bridge

### 1. Create Bridge User and Directory

```bash
sudo useradd -m -s /bin/bash mautrix-whatsapp
sudo mkdir -p /opt/mautrix-whatsapp
sudo chown mautrix-whatsapp:mautrix-whatsapp /opt/mautrix-whatsapp
```

### 2. Clone and Build

```bash
cd /opt/mautrix-whatsapp
sudo -u mautrix-whatsapp git clone https://github.com/mautrix/whatsapp.git .

# Build the bridge
sudo -u mautrix-whatsapp go build -o mautrix-whatsapp

# Verify build
./mautrix-whatsapp --version
```

### 3. Generate Configuration

```bash
cd /opt/mautrix-whatsapp
sudo -u mautrix-whatsapp ./mautrix-whatsapp -g
```

---

## Configure the Bridge

### 1. Edit config.yaml

```bash
sudo nano /opt/mautrix-whatsapp/config.yaml
```

**Key configurations:**

```yaml
# Homeserver details
homeserver:
    address: http://localhost:8008
    domain: matrix.yourdomain.com

# Application service host/port
appservice:
    address: http://localhost:29318
    hostname: 0.0.0.0
    port: 29318

    # Database
    database:
        type: postgres
        uri: postgres://mautrix_whatsapp:another_secure_password@localhost/mautrix_whatsapp?sslmode=disable

    # Bot and user settings
    id: whatsapp
    bot:
        username: whatsappbot
        displayname: WhatsApp Bridge Bot
        avatar: mxc://maunium.net/NeXNQarUbrlYBiPCpprYsRqr

    # AS token and HS token (auto-generated, keep them secure)
    as_token: "AUTO_GENERATED_TOKEN_HERE"
    hs_token: "AUTO_GENERATED_TOKEN_HERE"

# Bridge settings
bridge:
    # Username template for WhatsApp users
    username_template: "whatsapp_{{.}}"
    displayname_template: "{{if .BusinessName}}{{.BusinessName}}{{else if .PushName}}{{.PushName}}{{else}}{{.JID}}{{end}} (WhatsApp)"

    # Enable personal filtering (important for multi-account)
    personal_filtering_spaces: true

    # Allow user to invite bridge bot
    command_prefix: "!wa"

    # Permissions
    permissions:
        "*": relay
        "matrix.yourdomain.com": user
        "@yourusername:matrix.yourdomain.com": admin

# Logging
logging:
    min_level: info
    writers:
    - type: stdout
      format: pretty-colored
    - type: file
      format: json
      filename: /opt/mautrix-whatsapp/mautrix-whatsapp.log
      max_size: 100
      max_backups: 10
      compress: true
```

### 2. Generate Registration File

```bash
cd /opt/mautrix-whatsapp
sudo -u mautrix-whatsapp ./mautrix-whatsapp -g -r
```

This creates `registration.yaml` - copy it to Synapse:

```bash
sudo cp /opt/mautrix-whatsapp/registration.yaml /etc/matrix-synapse/whatsapp-registration.yaml
sudo chown matrix-synapse:matrix-synapse /etc/matrix-synapse/whatsapp-registration.yaml
```

---

## Connect Bridge to Synapse

### 1. Restart Synapse

```bash
sudo systemctl restart matrix-synapse
sudo systemctl status matrix-synapse
```

### 2. Create Systemd Service for Bridge

```bash
sudo nano /etc/systemd/system/mautrix-whatsapp.service
```

**Service file content:**

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
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### 3. Enable and Start Bridge

```bash
sudo systemctl daemon-reload
sudo systemctl enable mautrix-whatsapp
sudo systemctl start mautrix-whatsapp
sudo systemctl status mautrix-whatsapp
```

### 4. Check Logs

```bash
# Synapse logs
sudo journalctl -u matrix-synapse -f

# Bridge logs
sudo journalctl -u mautrix-whatsapp -f
```

---

## Setup Reverse Proxy (NGINX)

### 1. Create NGINX Configuration

```bash
sudo nano /etc/nginx/sites-available/matrix
```

**NGINX configuration:**

```nginx
server {
    listen 80;
    listen [::]:80;
    server_name matrix.yourdomain.com;

    # Redirect to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name matrix.yourdomain.com;

    # SSL certificates (will be added by certbot)
    ssl_certificate /etc/letsencrypt/live/matrix.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/matrix.yourdomain.com/privkey.pem;

    # SSL settings
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_prefer_server_ciphers on;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-GCM-SHA384;

    # Client/Federation API
    location ~ ^(/_matrix|/_synapse/client) {
        proxy_pass http://localhost:8008;
        proxy_set_header X-Forwarded-For $remote_addr;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Host $host;

        # Increase timeouts for large file uploads
        client_max_body_size 50M;
        proxy_read_timeout 600s;
        proxy_send_timeout 600s;
    }

    # Well-known files for client/server discovery
    location /.well-known/matrix/server {
        return 200 '{"m.server": "matrix.yourdomain.com:443"}';
        default_type application/json;
        add_header Access-Control-Allow-Origin *;
    }

    location /.well-known/matrix/client {
        return 200 '{"m.homeserver": {"base_url": "https://matrix.yourdomain.com"}}';
        default_type application/json;
        add_header Access-Control-Allow-Origin *;
    }
}

# Federation port (8448)
server {
    listen 8448 ssl http2;
    listen [::]:8448 ssl http2;
    server_name matrix.yourdomain.com;

    ssl_certificate /etc/letsencrypt/live/matrix.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/matrix.yourdomain.com/privkey.pem;

    location / {
        proxy_pass http://localhost:8008;
        proxy_set_header X-Forwarded-For $remote_addr;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Host $host;
    }
}
```

### 2. Enable Site

```bash
sudo ln -s /etc/nginx/sites-available/matrix /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

---

## SSL Certificate

### 1. Get Let's Encrypt Certificate

```bash
sudo certbot --nginx -d matrix.yourdomain.com
```

### 2. Auto-renewal

```bash
# Test renewal
sudo certbot renew --dry-run

# Certbot automatically sets up cron job for renewal
```

---

## Testing

### 1. Create Admin User

```bash
# Register admin user
register_new_matrix_user -c /etc/matrix-synapse/homeserver.yaml http://localhost:8008

# Follow prompts:
# Username: admin
# Password: your_password
# Make admin: yes
```

### 2. Test Matrix Connection

```bash
# Check if homeserver is accessible
curl https://matrix.yourdomain.com/_matrix/client/versions

# Should return JSON with supported versions
```

### 3. Test Bridge Bot

```bash
# Check if bridge bot is registered
curl http://localhost:8008/_matrix/client/r0/profile/@whatsappbot:matrix.yourdomain.com
```

### 4. Connect with App

1. Open MultiWhatsApp Android app
2. Enter:
   - **Homeserver**: `https://matrix.yourdomain.com`
   - **Username**: `admin` (or your username)
   - **Password**: Your password
3. Login should succeed

### 5. Link WhatsApp

1. In a Matrix client or the app, start a DM with `@whatsappbot:matrix.yourdomain.com`
2. Send: `login`
3. Bridge should respond with QR code
4. Scan with WhatsApp mobile app
5. Chats should start appearing!

---

## Troubleshooting

### Bridge Not Starting

```bash
# Check bridge logs
sudo journalctl -u mautrix-whatsapp -n 100 --no-pager

# Common issues:
# - Database connection failed: Check PostgreSQL credentials
# - Port already in use: Check if port 29318 is free
# - Registration file error: Regenerate with -g -r flags
```

### Synapse Not Starting

```bash
# Check Synapse logs
sudo journalctl -u matrix-synapse -n 100 --no-pager

# Common issues:
# - Database connection: Verify PostgreSQL credentials
# - Port conflict: Check if port 8008 is free
# - Config syntax: Validate YAML indentation
```

### Can't Login from App

```bash
# Check if homeserver is accessible
curl https://matrix.yourdomain.com/_matrix/client/versions

# Check NGINX logs
sudo tail -f /var/log/nginx/error.log

# Verify SSL certificate
openssl s_client -connect matrix.yourdomain.com:443 -servername matrix.yourdomain.com
```

### Bridge Bot Not Responding

```bash
# Check if bridge is running
sudo systemctl status mautrix-whatsapp

# Check bridge can connect to Synapse
curl http://localhost:8008/_matrix/client/versions

# Restart bridge
sudo systemctl restart mautrix-whatsapp
```

### QR Code Not Appearing

```bash
# Check bridge logs for WhatsApp connection
sudo journalctl -u mautrix-whatsapp -f

# Try sending login command again
# Make sure you're messaging the correct bridge bot user ID
```

---

## Security Hardening

### 1. Disable Registration (After Creating Users)

Edit `/etc/matrix-synapse/homeserver.yaml`:

```yaml
enable_registration: false
```

Restart: `sudo systemctl restart matrix-synapse`

### 2. Firewall Rules

```bash
# Allow SSH, HTTP, HTTPS, Matrix federation
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 8448/tcp
sudo ufw enable
```

### 3. Regular Updates

```bash
# Weekly updates
sudo apt update && sudo apt upgrade -y

# Update bridge
cd /opt/mautrix-whatsapp
sudo -u mautrix-whatsapp git pull
sudo -u mautrix-whatsapp go build -o mautrix-whatsapp
sudo systemctl restart mautrix-whatsapp
```

---

## Maintenance

### Backup Database

```bash
# Backup Synapse database
sudo -u postgres pg_dump synapse > synapse_backup_$(date +%Y%m%d).sql

# Backup bridge database
sudo -u postgres pg_dump mautrix_whatsapp > bridge_backup_$(date +%Y%m%d).sql
```

### Monitor Resources

```bash
# Check disk space
df -h

# Check memory
free -h

# Monitor processes
htop
```

### Update Matrix Synapse

```bash
sudo apt update
sudo apt install matrix-synapse-py3
sudo systemctl restart matrix-synapse
```

---

## Next Steps

1. **Create multiple Matrix accounts** (one per WhatsApp number you want to support)
2. **Update Android app** with your homeserver URL in the default value
3. **Test with real WhatsApp accounts** (use test numbers first!)
4. **Monitor logs** for any issues
5. **Set up monitoring** (Prometheus/Grafana optional)

---

## Useful Commands Reference

```bash
# Synapse
sudo systemctl status matrix-synapse
sudo systemctl restart matrix-synapse
sudo journalctl -u matrix-synapse -f

# Bridge
sudo systemctl status mautrix-whatsapp
sudo systemctl restart mautrix-whatsapp
sudo journalctl -u mautrix-whatsapp -f

# NGINX
sudo nginx -t
sudo systemctl reload nginx
sudo tail -f /var/log/nginx/access.log

# PostgreSQL
sudo -u postgres psql
# In psql: \l (list databases), \c database (connect), \dt (list tables)
```

---

## Support Resources

- **Matrix Synapse Docs**: https://matrix-org.github.io/synapse/latest/
- **mautrix-whatsapp**: https://github.com/mautrix/whatsapp
- **Matrix Community**: https://matrix.to/#/#synapse:matrix.org
- **Bridge Support**: https://matrix.to/#/#whatsapp:maunium.net

---

**🎉 Congratulations!** You now have a fully functional Matrix homeserver with WhatsApp bridge ready for the MultiWhatsApp Android app!
