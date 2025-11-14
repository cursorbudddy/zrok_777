# zrok_777
# Complete zrok Self-Hosting Implementation Guide

**Version:** 1.0
**Last Updated:** 2025-11-12
**Author:** Based on official zrok documentation and implementation research

---

## Table of Contents

1. [Introduction](#introduction)
2. [Prerequisites](#prerequisites)
3. [DNS Setup Guide](#dns-setup-guide)
4. [Architecture Overview](#architecture-overview)
5. [OpenZiti Infrastructure Setup](#openziti-infrastructure-setup)
6. [zrok Controller Setup](#zrok-controller-setup)
7. [zrok Frontend Configuration](#zrok-frontend-configuration)
8. [Reverse Proxy Integration (NGINX)](#reverse-proxy-integration-nginx)
9. [Docker Deployment Alternative](#docker-deployment-alternative)
10. [Metrics & Monitoring](#metrics--monitoring)
11. [Resource Limits Management](#resource-limits-management)
12. [OAuth Authentication](#oauth-authentication)
13. [Customization Features](#customization-features)
14. [Organizations Support](#organizations-support)
15. [Advanced Features](#advanced-features)
16. [Client Configuration](#client-configuration)
17. [Troubleshooting](#troubleshooting)
18. [Appendix](#appendix)

---

## Introduction

### What is zrok?

zrok is a next-generation sharing platform built on **OpenZiti**, a zero-trust networking framework. It enables secure, temporary, and permanent sharing of web services, files, TCP services, and more without complex networking configuration.

### Why Self-Host?

- **Privacy:** Complete control over your data
- **Customization:** Brand your instance with custom domains and OAuth
- **No Limits:** Set your own resource limits
- **Organizations:** Multi-user management capabilities
- **Cost:** No subscription fees for heavy usage

### Architecture Components

A self-hosted zrok instance consists of four main components:

1. **OpenZiti Controller** - Manages the secure network fabric
2. **OpenZiti Router** - Handles data plane operations
3. **zrok Controller** - Manages users, shares, and authentication
4. **zrok Frontend** - Handles public HTTP/HTTPS traffic

---

## Prerequisites

### Server Requirements

- **OS:** Linux (Ubuntu 20.04+, Debian 11+, RHEL/CentOS 8+, or similar)
- **RAM:** Minimum 2GB (4GB+ recommended for production)
- **CPU:** 2+ cores recommended
- **Storage:** 20GB+ available disk space
- **Network:** Public IP address with ports 80, 443, 1280, 3022 accessible

### Domain & DNS Requirements

- A domain name you control (e.g., `example.com`)
- Ability to create wildcard DNS records (e.g., `*.zrok.example.com`)
- DNS propagation time (15-60 minutes typically)

### Software Prerequisites

```bash
# Update system
sudo apt update && sudo apt upgrade -y  # Debian/Ubuntu
# OR
sudo dnf update -y  # RHEL/CentOS

# Install required packages
sudo apt install curl wget git openssl certbot -y  # Debian/Ubuntu
# OR
sudo dnf install curl wget git openssl certbot -y  # RHEL/CentOS
```

### Firewall Requirements

Open the following ports:

```bash
# HTTP/HTTPS for web traffic
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# OpenZiti Controller
sudo ufw allow 1280/tcp

# OpenZiti Router
sudo ufw allow 3022/tcp

# Enable firewall
sudo ufw enable
```

---

## DNS Setup Guide

### Understanding Wildcard DNS

Wildcard DNS allows any subdomain to resolve to your server IP. For zrok, you need:

- `zrok.example.com` → Your server IP
- `api.zrok.example.com` → Your server IP
- `*.zrok.example.com` → Your server IP (wildcard)

This enables dynamic share URLs like `abc123.zrok.example.com`, `share456.zrok.example.com`, etc.

### Free DNS Providers Supporting Wildcards

#### 1. Cloudflare (Recommended)

**Pros:**
- Free tier with full features
- Fast propagation (< 5 minutes)
- CDN and DDoS protection included
- Excellent API and documentation
- Perfect for production use

**Setup:**
1. Sign up at https://cloudflare.com
2. Add your domain
3. Update nameservers at your registrar
4. Add DNS records:

```
Type: A    Name: zrok             Value: YOUR_SERVER_IP    Proxy: DNS only
Type: A    Name: api.zrok         Value: YOUR_SERVER_IP    Proxy: DNS only
Type: A    Name: *.zrok           Value: YOUR_SERVER_IP    Proxy: DNS only
```

**CLI Example (using Cloudflare API):**
```bash
# Install cloudflare-cli
npm install -g cloudflare-cli

# Add records
cf add-record example.com zrok A YOUR_SERVER_IP
cf add-record example.com api.zrok A YOUR_SERVER_IP
cf add-record example.com *.zrok A YOUR_SERVER_IP
```

#### 2. DuckDNS (Easiest for Testing)

**Pros:**
- Completely free
- No domain purchase needed
- No credit card required
- GitHub login
- Up to 5 subdomains

**Cons:**
- Limited to DuckDNS subdomains (e.g., `yourname.duckdns.org`)
- Cannot issue combined root + wildcard Let's Encrypt certificates simultaneously

**Setup:**
1. Visit https://duckdns.org
2. Login with GitHub
3. Create a subdomain (e.g., `myzrok`)
4. You get: `myzrok.duckdns.org` and `*.myzrok.duckdns.org`
5. Update IP address via web interface or API

**API Update:**
```bash
# Update IP address
curl "https://www.duckdns.org/update?domains=myzrok&token=YOUR_TOKEN&ip=YOUR_SERVER_IP"

# Verify
dig myzrok.duckdns.org
dig test.myzrok.duckdns.org
```

#### 3. ClouDNS

**Pros:**
- Free tier available
- Full wildcard support (root and subdomains)
- All record types supported

**Setup:**
1. Sign up at https://cloudns.net
2. Add your domain or use their free subdomain
3. Create DNS records via web interface

```
Type: A    Host: @              Value: YOUR_SERVER_IP
Type: A    Host: api            Value: YOUR_SERVER_IP
Type: A    Host: *              Value: YOUR_SERVER_IP
```

#### 4. Other Options

| Provider | Cost | Wildcard | Let's Encrypt | Best For |
|----------|------|----------|---------------|----------|
| **Porkbun** | Free DNS | ✅ | ✅ | Developer-friendly |
| **Dynu** | Free | ✅ | ✅ | Dynamic IP |
| **AWS Route 53** | $0.50/zone | ✅ | ✅ | AWS infrastructure |
| **DigitalOcean** | Free for users | ✅ | ✅ | DO droplets |

### DNS Configuration Examples

#### Example 1: Using Cloudflare with example.com

```
# DNS Records
A     zrok.example.com          203.0.113.50    (TTL: 300)
A     api.zrok.example.com      203.0.113.50    (TTL: 300)
A     *.zrok.example.com        203.0.113.50    (TTL: 300)
```

**Result:**
- Controller: `https://api.zrok.example.com`
- Shares: `https://anything.zrok.example.com`

#### Example 2: Using DuckDNS

```
Subdomain: myzrok
IP: 203.0.113.50
```

**Result:**
- Controller: `https://myzrok.duckdns.org:18080` (or use subdomain)
- Shares: `https://anything.myzrok.duckdns.org`

### Verifying DNS Configuration

```bash
# Test DNS resolution
dig zrok.example.com
dig api.zrok.example.com
dig random123.zrok.example.com
dig test.zrok.example.com

# Using nslookup
nslookup zrok.example.com
nslookup anything.zrok.example.com

# Using host
host zrok.example.com
host test.zrok.example.com

# Check worldwide propagation
# Visit: https://www.whatsmydns.net
```

All wildcard subdomains should resolve to your server's IP address.

### DNS Propagation Time

- **Typical:** 15-30 minutes
- **Maximum:** Up to 48 hours (rare)
- **Cloudflare:** Usually < 5 minutes
- **DuckDNS:** Usually < 1 minute

---

## Architecture Overview

### Component Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Internet                              │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ Port 443 (HTTPS)
                       ▼
              ┌────────────────┐
              │ NGINX/Caddy    │
              │ Reverse Proxy  │
              │ (TLS Termination)
              └────────┬───────┘
                       │
        ┌──────────────┴──────────────┐
        │                             │
        │ Port 18080                  │ Port 8080
        ▼                             ▼
┌───────────────┐            ┌─────────────────┐
│ zrok          │            │ zrok Frontend   │
│ Controller    │◄───────────┤ (Public Access) │
│               │            │                 │
└───────┬───────┘            └─────────────────┘
        │
        │ Ziti API
        │
        ▼
┌───────────────────────────────────────┐
│      OpenZiti Network                 │
│  ┌──────────────┐  ┌───────────────┐ │
│  │   Ziti       │  │  Ziti Router  │ │
│  │ Controller   │◄─┤               │ │
│  │ Port 1280    │  │  Port 3022    │ │
│  └──────────────┘  └───────────────┘ │
└───────────────────────────────────────┘
```

### Data Flow

1. **User Access:** Client connects to `https://share123.zrok.example.com`
2. **NGINX:** Receives request, forwards to zrok frontend (port 8080)
3. **zrok Frontend:** Validates share, queries zrok controller
4. **zrok Controller:** Checks permissions, connects via OpenZiti
5. **OpenZiti:** Routes through secure fabric to backend service
6. **Backend:** Serves content through encrypted tunnel

---

## OpenZiti Infrastructure Setup

OpenZiti provides the zero-trust network fabric that zrok runs on. You need both a Controller and Router.

### OpenZiti Controller Installation

#### Method 1: Quick Installation (Recommended)

```bash
# One-liner installation
curl -sS https://get.openziti.io/install.bash | sudo bash -s openziti-controller
```

#### Method 2: Manual Repository Setup

**Debian/Ubuntu:**
```bash
# Add GPG key
curl -sS https://get.openziti.io/tun/package-repos.gpg | sudo gpg --dearmor --output /usr/share/keyrings/openziti.gpg

# Add repository
echo "deb [signed-by=/usr/share/keyrings/openziti.gpg] https://packages.openziti.org/zitipax-openziti-deb-stable debian main" | sudo tee /etc/apt/sources.list.d/openziti.list

# Update and install
sudo apt update
sudo apt install openziti-controller -y
```

**RHEL/CentOS:**
```bash
# Create repository file
sudo tee /etc/yum.repos.d/openziti.repo <<EOF
[openziti]
name=OpenZiti
baseurl=https://packages.openziti.org/zitipax-openziti-rpm-stable/redhat/\$basearch
enabled=1
gpgcheck=1
gpgkey=https://get.openziti.io/tun/package-repos.gpg
EOF

# Install
sudo dnf install openziti-controller -y
```

### OpenZiti Controller Configuration

#### Option 1: Automated Bootstrap (Easiest)

```bash
# Create bootstrap environment file
sudo tee /opt/openziti/etc/controller/bootstrap.env <<EOF
ZITI_CTRL_ADVERTISED_ADDRESS=zrok.example.com
ZITI_CTRL_ADVERTISED_PORT=1280
ZITI_USER=admin
ZITI_PWD=YourSecurePassword123!
EOF

# Run bootstrap
sudo /opt/openziti/etc/controller/bootstrap.bash
```

#### Option 2: Manual Configuration

```bash
# Generate configuration
ziti create config controller \
  --output /opt/openziti/etc/controller/controller.yaml \
  --address zrok.example.com \
  --port 1280

# Edit configuration as needed
sudo nano /opt/openziti/etc/controller/controller.yaml
```

### Start OpenZiti Controller

```bash
# Enable and start service
sudo systemctl enable --now ziti-controller.service

# Check status
sudo systemctl status ziti-controller.service

# View logs
sudo journalctl -u ziti-controller.service -f

# Verify listening port
sudo ss -tlnp | grep 1280
```

### OpenZiti Router Installation

#### Quick Installation

```bash
# Install router package
curl -sS https://get.openziti.io/install.bash | sudo bash -s openziti-router
```

### Create Router in Controller

Before configuring the router, you must create it in the controller:

```bash
# Login to controller
ziti edge login zrok.example.com:1280 -u admin -p YourSecurePassword123!

# Create router
ziti edge create edge-router zrok-router-1 \
  --role-attributes public \
  --tunneler-enabled

# Generate enrollment token (save this JWT)
ziti edge list edge-routers
ziti edge list edge-router-enrollments
```

The enrollment command will output a JWT token. Save this to a file:

```bash
# Save enrollment token
ziti edge list edge-router-enrollments -j > /tmp/router-enrollment.jwt
```

### OpenZiti Router Configuration

```bash
# Create bootstrap environment file
sudo tee /opt/openziti/etc/router/bootstrap.env <<EOF
ZITI_CTRL_ADVERTISED_ADDRESS=zrok.example.com
ZITI_CTRL_ADVERTISED_PORT=1280
ZITI_ROUTER_ADVERTISED_ADDRESS=zrok.example.com
ZITI_ROUTER_PORT=3022
ZITI_ROUTER_ENROLLMENT_TOKEN=/tmp/router-enrollment.jwt
EOF

# Run bootstrap
sudo /opt/openziti/etc/router/bootstrap.bash
```

### Start OpenZiti Router

```bash
# Enable and start service
sudo systemctl enable --now ziti-router.service

# Check status
sudo systemctl status ziti-router.service

# View logs
sudo journalctl -u ziti-router.service -f

# Verify listening port
sudo ss -tlnp | grep 3022
```

### Verify OpenZiti Network

```bash
# Check controller health
curl -sk https://zrok.example.com:1280/health-checks

# Login and verify router
ziti edge login zrok.example.com:1280 -u admin -p YourSecurePassword123!
ziti edge list edge-routers

# Should show router as "online"
```

---

## zrok Controller Setup

The zrok controller manages users, shares, and integrates with OpenZiti.

### Install zrok Binary

#### Method 1: Download Latest Release

```bash
# Download for Linux amd64
cd /tmp
ZROK_VERSION=$(curl -s https://api.github.com/repos/openziti/zrok/releases/latest | grep tag_name | cut -d '"' -f 4 | sed 's/v//')
wget https://github.com/openziti/zrok/releases/download/v${ZROK_VERSION}/zrok_${ZROK_VERSION}_linux_amd64.tar.gz

# Extract and install
tar -xzf zrok_${ZROK_VERSION}_linux_amd64.tar.gz
sudo mv zrok /usr/local/bin/
sudo chmod +x /usr/local/bin/zrok

# Verify installation
zrok version
```

#### Method 2: Package Manager (if available)

```bash
# Check if packages are available
sudo apt search zrok  # Debian/Ubuntu
# OR
sudo dnf search zrok  # RHEL/CentOS
```

### Create zrok Controller Configuration

```bash
# Create configuration directory
sudo mkdir -p /etc/zrok
sudo mkdir -p /var/lib/zrok

# Create controller configuration
sudo tee /etc/zrok/ctrl.yml <<'EOF'
# zrok Controller Configuration

# Admin configuration
admin:
  # Secret for admin API access
  secret: "ChangeThisAdminSecret123!"

  # Tou (Terms of Use) link
  tou_link: "https://zrok.example.com/terms"

# Endpoint configuration
endpoint:
  host: 0.0.0.0
  port: 18080

# Email configuration (optional, required for invites)
# email:
#   from: "noreply@zrok.example.com"
#   host: "smtp.example.com"
#   port: 587
#   username: "smtp_user"
#   password: "smtp_password"

# Invitation configuration
# invitation:
#   # Optional: require invitation tokens
#   token_strategy: "store"

# Limits configuration (optional)
# limits:
#   enforcing: false

# Metrics configuration (optional, see metrics section)
# metrics:
#   agent:
#     source:
#       type: fileSource
#       path: /tmp/fabric-usage.json

# Registration configuration
registration:
  # Allow public registration
  # public: false

# Store configuration
store:
  # Database path
  path: "/var/lib/zrok/zrok.db"
  type: "sqlite3"

# Ziti configuration
ziti:
  # OpenZiti API endpoint
  api_endpoint: "https://zrok.example.com:1280"

  # Admin credentials
  username: "admin"
  password: "YourSecurePassword123!"
EOF

# Set permissions
sudo chown -R $(whoami):$(whoami) /etc/zrok
sudo chown -R $(whoami):$(whoami) /var/lib/zrok
```

### Bootstrap zrok Controller

```bash
# Bootstrap the controller (creates database and Ziti config)
zrok admin bootstrap /etc/zrok/ctrl.yml

# Output should show:
# - Database initialization
# - Ziti identity creation
# - Service and policy creation
```

### Create systemd Service for zrok Controller

```bash
# Create systemd service file
sudo tee /etc/systemd/system/zrok-controller.service <<'EOF'
[Unit]
Description=zrok Controller
After=network.target ziti-controller.service
Wants=ziti-controller.service

[Service]
Type=simple
User=$(whoami)
WorkingDirectory=/var/lib/zrok
ExecStart=/usr/local/bin/zrok controller /etc/zrok/ctrl.yml
Restart=always
RestartSec=5
Environment="ZROK_API_ENDPOINT=http://127.0.0.1:18080"

[Install]
WantedBy=multi-user.target
EOF

# Reload systemd
sudo systemctl daemon-reload

# Enable and start service
sudo systemctl enable --now zrok-controller.service

# Check status
sudo systemctl status zrok-controller.service

# View logs
sudo journalctl -u zrok-controller.service -f
```

### Create Admin Account

```bash
# Set API endpoint environment variable
export ZROK_API_ENDPOINT=http://127.0.0.1:18080

# Create admin user account
zrok admin create account admin@example.com SecureAdminPassword123!

# Output will show account creation and token
# Save the token for later use
```

### Test zrok Controller API

```bash
# Test API endpoint
curl -X GET http://127.0.0.1:18080/api/v1/version

# Should return version information
```

---

## zrok Frontend Configuration

The zrok frontend handles incoming HTTP/HTTPS requests for public shares.

### Create Frontend Configuration

```bash
# Create frontend configuration
sudo tee /etc/zrok/http-frontend.yml <<'EOF'
# zrok Public Frontend Configuration

# Host pattern (matches your DNS wildcard)
host_match: "zrok.example.com"

# Bind address for HTTP traffic
address: "0.0.0.0:8080"

# Interstitial page configuration (optional)
# interstitial:
#   enabled: true
#   user_agent_prefixes:
#     - "Mozilla/5.0"

# OAuth configuration (optional, see OAuth section)
# oauth:
#   bind_address: "0.0.0.0:8181"
#   endpoint_url: "https://oauth.zrok.example.com"
#   cookie_name: "zrok_auth"
#   cookie_domain: ".zrok.example.com"
#   session_lifetime: "6h"
#   signing_key: "your-32-character-signing-key-here"
#   encryption_key: "your-24-character-encryption-key"
#   providers:
#     - name: "google"
#       type: "google"
#       client_id: "your-google-client-id"
#       client_secret: "your-google-client-secret"
EOF

# Set permissions
sudo chown $(whoami):$(whoami) /etc/zrok/http-frontend.yml
```

### Create systemd Service for zrok Frontend

```bash
# Create systemd service file
sudo tee /etc/systemd/system/zrok-frontend.service <<'EOF'
[Unit]
Description=zrok Public Frontend
After=network.target zrok-controller.service
Wants=zrok-controller.service

[Service]
Type=simple
User=$(whoami)
WorkingDirectory=/var/lib/zrok
ExecStart=/usr/local/bin/zrok access public /etc/zrok/http-frontend.yml
Restart=always
RestartSec=5
Environment="ZROK_API_ENDPOINT=http://127.0.0.1:18080"

[Install]
WantedBy=multi-user.target
EOF

# Reload systemd
sudo systemctl daemon-reload

# Enable and start service
sudo systemctl enable --now zrok-frontend.service

# Check status
sudo systemctl status zrok-frontend.service

# View logs
sudo journalctl -u zrok-frontend.service -f

# Verify listening port
sudo ss -tlnp | grep 8080
```

---

## Reverse Proxy Integration (NGINX)

Use NGINX to handle TLS termination and route traffic to appropriate services.

### Install NGINX

```bash
# Install NGINX
sudo apt install nginx -y  # Debian/Ubuntu
# OR
sudo dnf install nginx -y  # RHEL/CentOS

# Enable and start
sudo systemctl enable --now nginx
```

### Obtain Wildcard SSL Certificate

#### Method 1: Let's Encrypt with DNS Challenge (Recommended)

```bash
# Install certbot with DNS plugin (example for Cloudflare)
sudo apt install certbot python3-certbot-dns-cloudflare -y

# Create Cloudflare credentials file
sudo mkdir -p /root/.secrets
sudo tee /root/.secrets/cloudflare.ini <<EOF
dns_cloudflare_api_token = YOUR_CLOUDFLARE_API_TOKEN
EOF
sudo chmod 600 /root/.secrets/cloudflare.ini

# Obtain wildcard certificate
sudo certbot certonly \
  --dns-cloudflare \
  --dns-cloudflare-credentials /root/.secrets/cloudflare.ini \
  -d "zrok.example.com" \
  -d "*.zrok.example.com" \
  --email admin@example.com \
  --agree-tos \
  --non-interactive

# Certificates will be saved to:
# /etc/letsencrypt/live/zrok.example.com/fullchain.pem
# /etc/letsencrypt/live/zrok.example.com/privkey.pem
```

#### Method 2: Manual DNS Challenge

```bash
# Start manual certificate request
sudo certbot certonly --manual --preferred-challenges dns \
  -d "zrok.example.com" \
  -d "*.zrok.example.com" \
  --email admin@example.com \
  --agree-tos

# Certbot will provide TXT records to add to your DNS
# Add them, wait for propagation, then press Enter
```

#### Auto-Renewal Setup

```bash
# Test renewal
sudo certbot renew --dry-run

# Renewal is automatic via systemd timer
sudo systemctl status certbot.timer
```

### Configure NGINX

```bash
# Create NGINX configuration
sudo tee /etc/nginx/sites-available/zrok <<'EOF'
# WebSocket upgrade support
map $http_upgrade $connection_upgrade {
    default upgrade;
    '' close;
}

# zrok API/Controller
server {
    listen 443 ssl http2;
    server_name api.zrok.example.com;

    # SSL Configuration
    ssl_certificate /etc/letsencrypt/live/zrok.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/zrok.example.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # Security headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;

    # Proxy to zrok controller
    location / {
        proxy_pass http://127.0.0.1:18080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket support
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;

        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
}

# zrok Public Frontend (Wildcard)
server {
    listen 443 ssl http2;
    server_name *.zrok.example.com;

    # SSL Configuration
    ssl_certificate /etc/letsencrypt/live/zrok.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/zrok.example.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # Security headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # Increased buffer sizes for large requests
    proxy_buffers 4 512k;
    proxy_buffer_size 256k;
    proxy_busy_buffers_size 512k;

    # Proxy to zrok frontend
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket support
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;

        # Timeouts for long-running connections
        proxy_connect_timeout 300s;
        proxy_send_timeout 300s;
        proxy_read_timeout 300s;

        # Disable buffering for streaming
        proxy_buffering off;
    }
}

# HTTP to HTTPS redirect
server {
    listen 80;
    server_name api.zrok.example.com *.zrok.example.com;
    return 301 https://$host$request_uri;
}
EOF

# Enable site
sudo ln -sf /etc/nginx/sites-available/zrok /etc/nginx/sites-enabled/

# Remove default site (optional)
sudo rm -f /etc/nginx/sites-enabled/default

# Test configuration
sudo nginx -t

# Reload NGINX
sudo systemctl reload nginx
```

### Update zrok Frontend URL Template

After setting up NGINX, update the frontend to use HTTPS URLs:

```bash
# Get frontend token
zrok admin list frontends

# Update URL template (replace FRONTEND_TOKEN with actual token)
zrok admin update frontend FRONTEND_TOKEN --url-template "https://{{.Token}}.zrok.example.com"
```

### Firewall Adjustments

```bash
# Close direct access to controller and frontend
sudo ufw delete allow 18080/tcp
sudo ufw delete allow 8080/tcp

# Only NGINX needs external access on 443
# Ports 1280 and 3022 still need to be open for OpenZiti
```

---

## Docker Deployment Alternative

For those who prefer containerized deployment, zrok provides official Docker Compose files.

### Prerequisites

```bash
# Install Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
newgrp docker

# Install Docker Compose
sudo apt install docker-compose-plugin -y
```

### Download Docker Compose Files

#### Method 1: Automated Script

```bash
# Download and setup
curl https://get.openziti.io/zrok-instance/fetch.bash | bash
cd zrok-instance
```

#### Method 2: Manual Download

```bash
# Create directory
mkdir -p ~/zrok-instance
cd ~/zrok-instance

# Download compose files
wget https://raw.githubusercontent.com/openziti/zrok/main/docker/compose/zrok-instance/docker-compose.yml
wget https://raw.githubusercontent.com/openziti/zrok/main/docker/compose/zrok-instance/.env.example

# Copy environment template
cp .env.example .env
```

### Configure Environment

```bash
# Edit .env file
nano .env
```

**Essential Configuration:**

```bash
# DNS Configuration
ZROK_DNS_ZONE=zrok.example.com

# Admin Credentials
ZROK_ADMIN_EMAIL=admin@example.com
ZROK_ADMIN_PASSWORD=SecureAdminPassword123!

# Service Ports
ZROK_CTRL_PORT=18080
ZROK_FRONTEND_PORT=8080
ZROK_OAUTH_PORT=8181

# OpenZiti Configuration
ZITI_CTRL_PORT=1280
ZITI_ROUTER_PORT=3022

# Network Interface
ZROK_INTERFACE=0.0.0.0

# TLS Option (none, caddy, or traefik)
ZROK_TLS_OPTION=caddy

# For Caddy TLS
CADDY_DNS_PROVIDER=cloudflare
CADDY_DNS_PLUGIN=cloudflare
CLOUDFLARE_API_TOKEN=your_cloudflare_api_token

# For Let's Encrypt
ACME_EMAIL=admin@example.com
```

### Deployment Options

#### Option 1: Basic (No TLS - Testing Only)

```bash
# Use basic compose file
docker compose up -d

# Services will be available at:
# - Controller: http://zrok.example.com:18080
# - Frontend: http://*.zrok.example.com:8080
```

#### Option 2: With Caddy (Automatic TLS)

```bash
# Set TLS option in .env
ZROK_TLS_OPTION=caddy

# Start with Caddy override
docker compose -f docker-compose.yml -f docker-compose-caddy.yml up -d

# Caddy handles TLS automatically
# Services available at:
# - Controller: https://api.zrok.example.com
# - Frontend: https://*.zrok.example.com
```

#### Option 3: With Traefik (Advanced)

```bash
# Set TLS option in .env
ZROK_TLS_OPTION=traefik

# Configure Traefik DNS provider
nano .env  # Add Traefik-specific variables

# Start with Traefik override
docker compose -f docker-compose.yml -f docker-compose-traefik.yml up -d
```

### Create User Accounts

```bash
# Create admin account
docker compose exec zrok-controller zrok admin create account admin@example.com SecurePassword123!

# Output will show account token - save this!
```

### Verify Deployment

```bash
# Check running containers
docker compose ps

# View logs
docker compose logs -f

# Check specific service
docker compose logs zrok-controller
docker compose logs zrok-frontend
```

### Update and Maintain

```bash
# Pull latest images
docker compose pull

# Restart services
docker compose down && docker compose up -d

# Backup database
docker compose exec zrok-controller tar czf /tmp/backup.tar.gz /var/lib/zrok
docker compose cp zrok-controller:/tmp/backup.tar.gz ./backup-$(date +%Y%m%d).tar.gz
```

---

## Metrics & Monitoring

Monitor usage, bandwidth, and performance of your zrok instance.

### Architecture

```
OpenZiti Controller → Events (JSON) → Metrics Bridge → AMQP Queue → InfluxDB
                                                                      ↓
                                                                   Grafana
```

### Configure OpenZiti for Metrics

```bash
# Edit OpenZiti controller configuration
sudo nano /opt/openziti/etc/controller/controller.yaml
```

Add this section:

```yaml
events:
  jsonLogger:
    subscriptions:
      - type: fabric.usage
        version: 3
    handler:
      type: file
      format: json
      path: /tmp/fabric-usage.json

# Performance tuning (important!)
network:
  intervalAgeThreshold: 5s
  metricsReportInterval: 5s
```

**Apply to all routers too:**

```bash
# Edit each router configuration
sudo nano /opt/openziti/etc/router/router.yaml
```

Add:

```yaml
network:
  intervalAgeThreshold: 5s
  metricsReportInterval: 5s
```

Restart services:

```bash
sudo systemctl restart ziti-controller.service
sudo systemctl restart ziti-router.service
```

### Setup RabbitMQ

```bash
# Run RabbitMQ container
docker run -d \
  --name rabbitmq \
  --restart unless-stopped \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=zrok \
  -e RABBITMQ_DEFAULT_PASS=zrok_metrics_pass \
  rabbitmq:3.11-management

# Access management UI: http://your-server:15672
# Login: zrok / zrok_metrics_pass
```

### Configure Metrics Bridge

Add to zrok controller configuration (`/etc/zrok/ctrl.yml`):

```yaml
# Metrics bridge configuration
bridge:
  source:
    type: fileSource
    path: /tmp/fabric-usage.json
  sink:
    type: amqpSink
    url: amqp://zrok:zrok_metrics_pass@localhost:5672
    queue_name: zrok_events
```

### Setup InfluxDB

```bash
# Run InfluxDB 2.x container
docker run -d \
  --name influxdb \
  --restart unless-stopped \
  -p 8086:8086 \
  -v influxdb-data:/var/lib/influxdb2 \
  -e DOCKER_INFLUXDB_INIT_MODE=setup \
  -e DOCKER_INFLUXDB_INIT_USERNAME=admin \
  -e DOCKER_INFLUXDB_INIT_PASSWORD=SecureInfluxPass123! \
  -e DOCKER_INFLUXDB_INIT_ORG=zrok \
  -e DOCKER_INFLUXDB_INIT_BUCKET=zrok \
  -e DOCKER_INFLUXDB_INIT_RETENTION=30d \
  -e DOCKER_INFLUXDB_INIT_ADMIN_TOKEN=ZrokMetricsToken123456789 \
  influxdb:2

# Access UI: http://your-server:8086
# Login with credentials above
```

### Configure zrok Metrics Agent

Add to zrok controller configuration (`/etc/zrok/ctrl.yml`):

```yaml
# Metrics configuration
metrics:
  agent:
    source:
      type: amqpSource
      url: amqp://zrok:zrok_metrics_pass@localhost:5672
      queue_name: zrok_events
  influx:
    url: "http://localhost:8086"
    bucket: "zrok"
    org: "zrok"
    token: "ZrokMetricsToken123456789"
```

### Start Metrics Bridge

```bash
# Run metrics bridge
zrok ctrl metrics bridge /etc/zrok/ctrl.yml

# Or create systemd service
sudo tee /etc/systemd/system/zrok-metrics-bridge.service <<'EOF'
[Unit]
Description=zrok Metrics Bridge
After=network.target rabbitmq.service

[Service]
Type=simple
User=$(whoami)
ExecStart=/usr/local/bin/zrok ctrl metrics bridge /etc/zrok/ctrl.yml
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable --now zrok-metrics-bridge.service
```

### Setup Grafana (Optional)

```bash
# Run Grafana container
docker run -d \
  --name grafana \
  --restart unless-stopped \
  -p 3000:3000 \
  -v grafana-data:/var/lib/grafana \
  -e GF_SECURITY_ADMIN_PASSWORD=SecureGrafanaPass123! \
  grafana/grafana:latest

# Access UI: http://your-server:3000
# Default login: admin / SecureGrafanaPass123!
```

**Add InfluxDB Data Source:**

1. Go to Configuration → Data Sources → Add data source
2. Select InfluxDB
3. Configure:
   - Query Language: Flux
   - URL: http://localhost:8086
   - Organization: zrok
   - Token: ZrokMetricsToken123456789
   - Default Bucket: zrok

### Verify Metrics Collection

```bash
# Generate test traffic
zrok test loop public --iterations 100

# Check InfluxDB for data
docker exec -it influxdb influx query 'from(bucket:"zrok") |> range(start: -1h)'

# Check zrok controller logs
sudo journalctl -u zrok-controller.service -f
```

---

## Resource Limits Management

Control resource consumption per user/account.

### Enable Limits in Controller

Edit `/etc/zrok/ctrl.yml`:

```yaml
limits:
  # Enable limit enforcement
  enforcing: true

  # Enable database locking (PostgreSQL only, prevents race conditions)
  # enable_locking: true

  # Global default limits (apply to all accounts unless overridden)
  environments: 2
  shares: 5
  reserved_shares: 2
  unique_names: 2
  share_frontends: 2

  # Bandwidth limits
  bandwidth:
    # Period for bandwidth calculation
    period: 24h

    # Warning threshold (sends email notification)
    warning:
      rx: 104857600  # 100 MB received
      tx: 104857600  # 100 MB transmitted
      total: 209715200  # 200 MB total

    # Enforcement threshold (disables shares)
    limit:
      rx: 1073741824  # 1 GB received
      tx: 1073741824  # 1 GB transmitted
      total: 2147483648  # 2 GB total
```

**Note:** `-1` means unlimited.

### Limit Classes (Database-Driven)

For granular control, use limit classes stored in the database:

```sql
-- Connect to database
sqlite3 /var/lib/zrok/zrok.db

-- Create a "premium" resource limit class
INSERT INTO limit_classes (id, label, resource_count_limit)
VALUES (1, 'premium', 50);

-- Apply to specific account
INSERT INTO applied_limit_classes (account_id, limit_class_id)
VALUES ((SELECT id FROM accounts WHERE email = 'premium@example.com'), 1);

-- Create scoped bandwidth limit class (per-backend)
INSERT INTO limit_classes (id, label, backend_mode, bandwidth_limit_rx, bandwidth_limit_tx, period_minutes)
VALUES (2, 'web-heavy', 'web', 10737418240, 10737418240, 1440);

-- Apply to account
INSERT INTO applied_limit_classes (account_id, limit_class_id)
VALUES ((SELECT id FROM accounts WHERE email = 'webuser@example.com'), 2);
```

### Exempting Accounts from Limits

```sql
-- Mark account as "limitless" (bypasses all limits)
UPDATE accounts SET limitless = 1 WHERE email = 'admin@example.com';
```

### Monitoring Limits

```bash
# Check account limits via API
curl -X GET http://localhost:18080/api/v1/account/limits \
  -H "Authorization: Bearer USER_TOKEN"

# Admin: View all account resource usage
zrok admin list accounts
```

### Bandwidth Calculation

- **Rolling 24-hour window** (or configured period)
- Tracks RX (received), TX (transmitted), and Total
- When exceeded:
  - **Warning:** Sends email notification (if configured)
  - **Limit:** Removes OpenZiti dial policies, shares return 404

---

## OAuth Authentication

Secure public shares with OAuth authentication.

### Supported Providers

- **Google OAuth**
- **GitHub OAuth**
- **Generic OIDC** (Keycloak, Auth0, Okta, Azure AD, etc.)

### Frontend OAuth Configuration

Edit `/etc/zrok/http-frontend.yml`:

```yaml
oauth:
  # OAuth listener address
  bind_address: "0.0.0.0:8181"

  # Public OAuth endpoint URL
  endpoint_url: "https://oauth.zrok.example.com"

  # Cookie configuration
  cookie_name: "zrok_auth"
  cookie_domain: ".zrok.example.com"

  # Session lifetime
  session_lifetime: "6h"

  # Security keys (generate random strings)
  signing_key: "your-random-32-character-signing-key-here-minimum"
  encryption_key: "24-character-encryption"

  # OAuth providers
  providers:
    - name: "google"
      type: "google"
      client_id: "your-google-client-id.apps.googleusercontent.com"
      client_secret: "your-google-client-secret"

    - name: "github"
      type: "github"
      client_id: "your-github-client-id"
      client_secret: "your-github-client-secret"

    - name: "keycloak"
      type: "oidc"
      client_id: "zrok"
      client_secret: "your-keycloak-client-secret"
      scopes:
        - openid
        - email
        - profile
      issuer: "https://keycloak.example.com/realms/myrealm"
      supports_pkce: true
```

### Generate Security Keys

```bash
# Generate signing key (32+ characters)
openssl rand -base64 32

# Generate encryption key (24+ characters)
openssl rand -base64 24
```

### Google OAuth Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Create a new project or select existing
3. Navigate to **APIs & Services** → **Credentials**
4. Click **Create Credentials** → **OAuth client ID**
5. Configure OAuth consent screen:
   - Add authorized domain: `zrok.example.com`
   - Add scope: `../auth/userinfo.email`
6. Create OAuth 2.0 Client ID:
   - Application type: **Web application**
   - Authorized redirect URIs:
     ```
     https://oauth.zrok.example.com/google/auth/callback
     ```
7. Save Client ID and Client Secret

### GitHub OAuth Setup

1. Go to [GitHub Developer Settings](https://github.com/settings/developers)
2. Click **New OAuth App**
3. Configure:
   - Application name: `zrok Instance`
   - Homepage URL: `https://zrok.example.com`
   - Authorization callback URL:
     ```
     https://oauth.zrok.example.com/github/auth/callback
     ```
4. Save Client ID and Client Secret

### OIDC Provider Setup (Generic)

**Example: Keycloak**

1. Create a new client in Keycloak realm
2. Configure:
   - Client ID: `zrok`
   - Client Protocol: `openid-connect`
   - Access Type: `confidential`
   - Valid Redirect URIs:
     ```
     https://oauth.zrok.example.com/keycloak/auth/callback
     ```
   - Base URL: `https://zrok.example.com`
3. Enable scopes: `openid`, `email`, `profile`
4. Get credentials from **Credentials** tab

**Common OIDC Issuer URLs:**

```yaml
# Keycloak
issuer: "https://keycloak.example.com/realms/your-realm"

# Auth0
issuer: "https://your-tenant.auth0.com/"

# Okta
issuer: "https://your-domain.okta.com/oauth2/default"

# Azure AD
issuer: "https://login.microsoftonline.com/your-tenant-id/v2.0"
```

### NGINX Configuration for OAuth

Add to NGINX configuration:

```nginx
# OAuth endpoint
server {
    listen 443 ssl http2;
    server_name oauth.zrok.example.com;

    ssl_certificate /etc/letsencrypt/live/zrok.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/zrok.example.com/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:8181;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;
    }
}
```

Reload NGINX:

```bash
sudo nginx -t && sudo systemctl reload nginx
```

### Using OAuth with Shares

```bash
# Create OAuth-protected public share
zrok share public --backend-mode web \
  --oauth-provider google \
  --oauth-email-address-pattern '*@example.com' \
  /path/to/files

# Multiple email patterns
zrok share public --backend-mode web \
  --oauth-provider google \
  --oauth-email-address-pattern '*@example.com' \
  --oauth-email-address-pattern 'admin@*.org' \
  8080

# Re-check authentication every hour
zrok share public --backend-mode web \
  --oauth-provider google \
  --oauth-email-address-pattern '*@example.com' \
  --oauth-check-interval 1h \
  8080
```

### OAuth Headers Passed to Backend

Protected shares receive these headers:

```
zrok-auth-provider: google
zrok-auth-email: user@example.com
zrok-auth-expires: 2025-11-12T15:30:00Z
```

Use these in your application to identify authenticated users.

### Logout Endpoint

Each provider has a logout URL:

```
https://oauth.zrok.example.com/google/logout
https://oauth.zrok.example.com/github/logout
https://oauth.zrok.example.com/keycloak/logout
```

Optional redirect after logout:

```
https://oauth.zrok.example.com/google/logout?redirect_url=https://example.com
```

---

## Customization Features

Personalize your zrok instance's appearance and behavior.

### Custom Error Pages

Create branded error pages for 404, 401, 502, and health check responses.

#### Create Custom Template

```bash
# Create template directory
mkdir -p /etc/zrok/templates

# Create custom error page
cat > /etc/zrok/templates/error.html <<'EOF'
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{{.Title}}</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: #fff;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            margin: 0;
            padding: 20px;
        }
        .container {
            background: rgba(255, 255, 255, 0.1);
            backdrop-filter: blur(10px);
            border-radius: 20px;
            padding: 40px;
            max-width: 600px;
            text-align: center;
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
        }
        h1 {
            font-size: 3em;
            margin: 0 0 20px 0;
        }
        .banner {
            font-size: 1.5em;
            margin-bottom: 20px;
        }
        .message {
            font-size: 1.1em;
            line-height: 1.6;
            margin-bottom: 20px;
        }
        .error {
            background: rgba(255, 255, 255, 0.2);
            padding: 15px;
            border-radius: 10px;
            font-family: monospace;
            font-size: 0.9em;
            word-break: break-all;
        }
        .logo {
            margin-bottom: 30px;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="logo">
            <svg width="60" height="60" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
                <circle cx="50" cy="50" r="40" stroke="white" stroke-width="8"/>
                <path d="M30 50 L50 70 L70 30" stroke="white" stroke-width="8" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
        </div>
        <h1>{{.Title}}</h1>
        <div class="banner">{{.Banner}}</div>
        {{if .Message}}
        <div class="message">{{.Message}}</div>
        {{end}}
        {{if .Error}}
        <div class="error">{{.Error}}</div>
        {{end}}
    </div>
</body>
</html>
EOF
```

#### Configure Frontend to Use Template

Edit `/etc/zrok/http-frontend.yml`:

```yaml
host_match: "zrok.example.com"
address: "0.0.0.0:8080"

# Custom error template
template_path: "/etc/zrok/templates/error.html"
```

Or use with private access:

```bash
zrok access private --template-path /etc/zrok/templates/error.html <shareToken>
```

Restart frontend:

```bash
sudo systemctl restart zrok-frontend.service
```

### Interstitial Warning Pages

Add security warnings for public shares to combat phishing.

Edit `/etc/zrok/http-frontend.yml`:

```yaml
interstitial:
  enabled: true

  # Only show to browser user agents
  user_agent_prefixes:
    - "Mozilla/5.0"
```

**Bypassing Interstitials:**

Users can bypass by:
- Clicking button on interstitial (sets cookie for 1 week)
- Sending header: `skip_zrok_interstitial: true`
- Using non-standard User-Agent

**Account-Level Bypass (Admin):**

```sql
-- Trust specific accounts to skip interstitials
INSERT INTO skip_interstitial_grants (account_id)
VALUES ((SELECT id FROM accounts WHERE email = 'trusted@example.com'));
```

### Personalized Frontend

Users can run their own frontend while using your shared controller.

**User Setup:**

```bash
# On user's VPS/server

# Reserve shares
zrok reserve public --backend-mode web /var/www/html

# Access with custom binding
zrok access private --bind 127.0.0.1:9191 <share-token>

# Configure reverse proxy (nginx/caddy) with their own domain
# Point custom.user-domain.com → 127.0.0.1:9191
```

**Benefits:**
- Users get custom domains and TLS
- Enhanced privacy (your controller doesn't see URLs/traffic)
- Users control their own frontend configuration

---

## Organizations Support

Group and manage related accounts (available in v0.4.45+).

### Enable Organizations

Organizations require no special configuration—they're built into zrok.

### Admin Setup

Set admin token in environment:

```bash
export ZROK_ADMIN_TOKEN="your-admin-token-from-ctrl.yml"
```

### Create Organization

```bash
# Create organization
zrok admin create organization "Engineering Team" "Development team organization"

# List organizations
zrok admin list organizations
```

### Add Members

```bash
# Add member to organization
zrok admin add organization-member <org-id> user@example.com

# Add member as organization admin
zrok admin add organization-member <org-id> admin@example.com --admin
```

### List Organization Members

```bash
# List members of organization
zrok admin list organization-members <org-id>
```

### Organization Admin Commands

Members designated as org admins can:

```bash
# List organizations you're a member of
zrok organization memberships

# List members (org admin only)
zrok organization admin list-members <org-id>

# View member's overview (org admin only)
zrok organization admin overview <org-id> <account-email>
```

### Remove Members

```bash
# Remove member from organization
zrok admin remove organization-member <org-id> user@example.com
```

### Delete Organization

```bash
# Delete organization
zrok admin delete organization <org-id>
```

### Future Features

Upcoming releases will add:
- Closed-loop sharing within organizations
- Shared resource quotas
- Organization-specific limits

---

## Advanced Features

### zrok Drives (File Sharing)

Share files and folders with smart synchronization.

#### Create Drive Share

```bash
# Public drive share
zrok share public --backend-mode drive /path/to/folder

# Private drive share
zrok share private --backend-mode drive /path/to/folder
# Output: Share token like 'wkcfb58vj51l'

# Reserved drive share (persistent)
zrok reserve public --backend-mode drive /path/to/folder --unique-name my-files
```

#### Access Drive Files

```bash
# List files (private share)
zrok cp --list zrok://wkcfb58vj51l

# List files (public share)
zrok cp --list https://abc123.zrok.example.com

# Copy file from share
zrok cp zrok://wkcfb58vj51l/document.pdf ./local-document.pdf

# Copy entire folder
zrok cp --recursive zrok://wkcfb58vj51l/folder ./local-folder

# Smart sync (only modified files)
zrok cp --sync --recursive zrok://wkcfb58vj51l/folder ./local-folder
```

#### Upload to Drive

```bash
# Copy file to drive
zrok cp ./local-file.txt zrok://wkcfb58vj51l/remote-file.txt

# Copy folder
zrok cp --recursive ./local-folder zrok://wkcfb58vj51l/remote-folder
```

#### Drive-to-Drive Copy

```bash
# Copy between two drives without local storage
zrok cp zrok://source-token/file.pdf zrok://dest-token/file.pdf
```

#### Basic Authentication

```bash
# Share with password
zrok share public --backend-mode drive --basic-auth user:pass /path/to/folder

# Access with auth
export ZROK_DRIVES_BASIC_AUTH=user:pass
zrok cp --list https://abc123.zrok.example.com
```

### zrok VPN

Create point-to-point VPN connections between hosts.

#### Start VPN Server

```bash
# Default subnet (10.122.0.0/16, server gets 10.122.0.1)
sudo -E zrok share private --backend-mode vpn

# Custom subnet
sudo -E zrok share private --backend-mode vpn 10.200.0.0/24

# Reserved VPN
sudo -E zrok reserve private --backend-mode vpn --unique-name my-vpn
sudo -E zrok share private my-vpn
```

#### Connect VPN Client

```bash
# Connect to VPN (gets 10.122.0.2+)
sudo -E zrok access private <share-token>

# Verify connectivity
ping 10.122.0.1
ssh user@10.122.0.1
```

#### Platform-Specific Notes

**Windows:**
- Requires administrator privileges
- Download `wintun.dll` and place next to `zrok.exe`
- Download from: https://www.wintun.net/

**Linux:**
- Requires root or `NET_ADMIN` capability
- Use `sudo -E` to preserve environment

```bash
# Alternative: set capability (no root needed)
sudo setcap cap_net_admin=eip /usr/local/bin/zrok
zrok access private <share-token>
```

**macOS:**
- Requires root access
- Use `sudo -E` to preserve zrok config

---

## Client Configuration

Configure client machines to use your self-hosted zrok instance.

### Install zrok Client

```bash
# Download latest release
cd /tmp
ZROK_VERSION=$(curl -s https://api.github.com/repos/openziti/zrok/releases/latest | grep tag_name | cut -d '"' -f 4 | sed 's/v//')

# Linux
wget https://github.com/openziti/zrok/releases/download/v${ZROK_VERSION}/zrok_${ZROK_VERSION}_linux_amd64.tar.gz
tar -xzf zrok_${ZROK_VERSION}_linux_amd64.tar.gz
sudo mv zrok /usr/local/bin/
sudo chmod +x /usr/local/bin/zrok

# macOS
wget https://github.com/openziti/zrok/releases/download/v${ZROK_VERSION}/zrok_${ZROK_VERSION}_darwin_amd64.zip
unzip zrok_${ZROK_VERSION}_darwin_amd64.zip
sudo mv zrok /usr/local/bin/
sudo chmod +x /usr/local/bin/zrok

# Windows
# Download from: https://github.com/openziti/zrok/releases
# Extract zrok.exe to a folder in your PATH
```

### Configure API Endpoint

```bash
# Set your custom zrok instance
zrok config set apiEndpoint https://api.zrok.example.com

# Verify configuration
zrok status
# Output: Config: apiEndpoint https://api.zrok.example.com config
```

### Create Account Token

On the server (or via web UI):

```bash
# Create account for user
zrok admin create account user@example.com UserPassword123!

# Output will show account token
# Token: ey...
```

### Enable Environment

```bash
# On client machine, enable with token
zrok enable ey...

# Verify
zrok status

# Output should show:
# Environment: <environment-id>
# API Endpoint: https://api.zrok.example.com
# Account: user@example.com
```

### Test Connectivity

```bash
# Create a test share
zrok share public 8080

# In another terminal, access it
curl https://<share-token>.zrok.example.com
```

### Multiple Environments

Users can have multiple environments (different devices):

```bash
# Laptop
zrok enable <token>  # Creates laptop environment

# Desktop
zrok enable <token>  # Creates desktop environment

# List environments (on server)
zrok admin list environments
```

---

## Troubleshooting

### General Diagnostics

```bash
# Check all services
sudo systemctl status ziti-controller
sudo systemctl status ziti-router
sudo systemctl status zrok-controller
sudo systemctl status zrok-frontend
sudo systemctl status nginx

# Check logs
sudo journalctl -u ziti-controller -n 100 --no-pager
sudo journalctl -u ziti-router -n 100 --no-pager
sudo journalctl -u zrok-controller -n 100 --no-pager
sudo journalctl -u zrok-frontend -n 100 --no-pager

# Check listening ports
sudo ss -tlnp | grep -E '(1280|3022|8080|18080|443)'

# Check DNS resolution
dig api.zrok.example.com
dig test.zrok.example.com
```

### OpenZiti Controller Issues

**Problem:** Controller won't start

```bash
# Check configuration syntax
ziti controller validate /opt/openziti/etc/controller/controller.yaml

# Check certificate paths
ls -la /opt/openziti/etc/controller/*.pem

# Check permissions
sudo chown -R openziti:openziti /opt/openziti

# Reset and re-bootstrap (CAUTION: destroys data)
sudo systemctl stop ziti-controller
sudo rm -rf /var/lib/ziti-controller/*
sudo /opt/openziti/etc/controller/bootstrap.bash
```

**Problem:** Can't connect to controller API

```bash
# Check firewall
sudo ufw status | grep 1280

# Test locally
curl -k https://localhost:1280/health-checks

# Test TLS certificate
openssl s_client -connect zrok.example.com:1280 -servername zrok.example.com

# Check controller logs for certificate errors
sudo journalctl -u ziti-controller -f
```

### OpenZiti Router Issues

**Problem:** Router shows as offline

```bash
# Check router enrollment
sudo journalctl -u ziti-router | grep -i enroll

# Verify router can reach controller
curl -k https://zrok.example.com:1280/health-checks

# Check router identity
ls -la /var/lib/ziti-router/

# Re-enroll router (if needed)
sudo systemctl stop ziti-router
sudo rm -f /var/lib/ziti-router/*.json
# Get new enrollment token from controller
sudo /opt/openziti/etc/router/bootstrap.bash
```

**Problem:** Data plane connectivity issues

```bash
# Verify router port accessibility
telnet zrok.example.com 3022

# From another host
nc -zv zrok.example.com 3022

# Check firewall rules
sudo ufw status | grep 3022
sudo iptables -L -n | grep 3022
```

### zrok Controller Issues

**Problem:** Controller fails to bootstrap

```bash
# Check Ziti connectivity
curl -k https://zrok.example.com:1280/health-checks

# Verify credentials in ctrl.yml
grep -A 5 "ziti:" /etc/zrok/ctrl.yml

# Check database permissions
ls -la /var/lib/zrok/

# Enable debug logging
zrok controller --verbose /etc/zrok/ctrl.yml
```

**Problem:** API endpoint unreachable

```bash
# Test locally
curl http://localhost:18080/api/v1/version

# Check NGINX configuration
sudo nginx -t
sudo systemctl status nginx

# Test NGINX proxy
curl -k https://api.zrok.example.com/api/v1/version

# Check NGINX logs
sudo tail -f /var/log/nginx/error.log
```

### zrok Frontend Issues

**Problem:** Shares return 404

```bash
# Check frontend status
sudo systemctl status zrok-frontend

# Check frontend logs
sudo journalctl -u zrok-frontend -f

# Verify frontend registration
zrok admin list frontends

# Test frontend directly
curl http://localhost:8080

# Check DNS resolution
dig <share-token>.zrok.example.com
```

**Problem:** WebSocket connections fail

```bash
# Check NGINX WebSocket configuration
sudo grep -A 5 "upgrade" /etc/nginx/sites-available/zrok

# Test WebSocket upgrade
curl -i -N -H "Connection: Upgrade" -H "Upgrade: websocket" \
  https://test.zrok.example.com

# Check proxy buffers
sudo grep "proxy_buffer" /etc/nginx/sites-available/zrok
```

### SSL/TLS Issues

**Problem:** Certificate errors

```bash
# Check certificate validity
openssl x509 -in /etc/letsencrypt/live/zrok.example.com/fullchain.pem -text -noout

# Verify certificate chain
curl -vI https://api.zrok.example.com

# Test wildcard certificate
openssl s_client -connect anything.zrok.example.com:443 -servername anything.zrok.example.com

# Check certificate expiration
certbot certificates

# Renew certificate
sudo certbot renew --dry-run
sudo certbot renew
sudo systemctl reload nginx
```

### DNS Issues

**Problem:** Wildcard not resolving

```bash
# Test DNS from different sources
dig test123.zrok.example.com @8.8.8.8
dig test456.zrok.example.com @1.1.1.1

# Check DNS propagation worldwide
# Visit: https://www.whatsmydns.net

# Verify DNS records at provider
# (Check provider's web interface)

# Clear local DNS cache
sudo systemd-resolve --flush-caches  # Linux
dscacheutil -flushcache  # macOS
ipconfig /flushdns  # Windows
```

### Authentication Issues

**Problem:** OAuth not working

```bash
# Check OAuth configuration
grep -A 20 "oauth:" /etc/zrok/http-frontend.yml

# Test OAuth endpoint
curl https://oauth.zrok.example.com

# Check OAuth redirect URI matches provider config
# Provider must have: https://oauth.zrok.example.com/<provider>/auth/callback

# Check OAuth logs
sudo journalctl -u zrok-frontend | grep -i oauth
```

**Problem:** User can't enable environment

```bash
# Verify account exists
zrok admin list accounts

# Verify API endpoint
curl https://api.zrok.example.com/api/v1/version

# Test with verbose client
zrok --verbose enable <token>

# Check account token validity
# Tokens are single-use; generate new one if needed
zrok admin create account user@example.com NewPassword123!
```

### Performance Issues

**Problem:** Slow share performance

```bash
# Check OpenZiti metrics interval (should be ~5s)
grep -A 3 "network:" /opt/openziti/etc/controller/controller.yaml

# Check system resources
top
htop
free -h
df -h

# Check network latency
ping zrok.example.com
traceroute zrok.example.com

# Monitor connections
sudo ss -tnp | grep -E '(1280|3022|8080|18080)'
```

**Problem:** High memory usage

```bash
# Check process memory
ps aux | grep -E '(zrok|ziti)'

# Check for memory leaks in logs
sudo journalctl -u zrok-controller | grep -i "memory"
sudo journalctl -u ziti-controller | grep -i "memory"

# Restart services if needed
sudo systemctl restart ziti-controller
sudo systemctl restart ziti-router
sudo systemctl restart zrok-controller
sudo systemctl restart zrok-frontend
```

### Database Issues

**Problem:** Database corruption

```bash
# Backup database
cp /var/lib/zrok/zrok.db /var/lib/zrok/zrok.db.backup

# Check database integrity (SQLite)
sqlite3 /var/lib/zrok/zrok.db "PRAGMA integrity_check;"

# Vacuum database (reclaim space)
sqlite3 /var/lib/zrok/zrok.db "VACUUM;"

# If corrupted, restore from backup
sudo systemctl stop zrok-controller
cp /var/lib/zrok/zrok.db.backup /var/lib/zrok/zrok.db
sudo systemctl start zrok-controller
```

### Common Error Messages

**Error:** `failed to dial Ziti controller`

- **Cause:** zrok can't reach OpenZiti controller
- **Fix:** Check Ziti controller is running, firewall allows port 1280, DNS resolves

**Error:** `no registration token found`

- **Cause:** Frontend not registered with controller
- **Fix:** Restart zrok-frontend service, check bootstrap was successful

**Error:** `share not found`

- **Cause:** Share token invalid or expired
- **Fix:** Verify share is active with `zrok status`, check frontend logs

**Error:** `certificate verify failed`

- **Cause:** TLS certificate mismatch or expired
- **Fix:** Check certificate validity, ensure cert matches domain, renew if needed

**Error:** `database is locked`

- **Cause:** Multiple processes accessing SQLite database
- **Fix:** Ensure only one zrok-controller instance running, consider PostgreSQL for production

---

## Appendix

### A. Complete Configuration Files

#### `/etc/zrok/ctrl.yml` (Full Example)

```yaml
admin:
  secret: "ChangeThisAdminSecret123!"
  tou_link: "https://zrok.example.com/terms"

endpoint:
  host: 0.0.0.0
  port: 18080

email:
  from: "noreply@zrok.example.com"
  host: "smtp.gmail.com"
  port: 587
  username: "your-gmail@gmail.com"
  password: "your-app-password"

invitation:
  token_strategy: "open"

limits:
  enforcing: true
  environments: 3
  shares: 10
  reserved_shares: 5
  unique_names: 5
  share_frontends: 3
  bandwidth:
    period: 24h
    warning:
      rx: 1073741824    # 1 GB
      tx: 1073741824    # 1 GB
      total: 2147483648 # 2 GB
    limit:
      rx: 5368709120    # 5 GB
      tx: 5368709120    # 5 GB
      total: 10737418240 # 10 GB

metrics:
  agent:
    source:
      type: amqpSource
      url: amqp://zrok:zrok_metrics_pass@localhost:5672
      queue_name: zrok_events
  influx:
    url: "http://localhost:8086"
    bucket: "zrok"
    org: "zrok"
    token: "ZrokMetricsToken123456789"

registration:
  public: false

store:
  path: "/var/lib/zrok/zrok.db"
  type: "sqlite3"

ziti:
  api_endpoint: "https://zrok.example.com:1280"
  username: "admin"
  password: "YourSecurePassword123!"
```

#### `/etc/zrok/http-frontend.yml` (Full Example)

```yaml
host_match: "zrok.example.com"
address: "0.0.0.0:8080"
template_path: "/etc/zrok/templates/error.html"

interstitial:
  enabled: true
  user_agent_prefixes:
    - "Mozilla/5.0"

oauth:
  bind_address: "0.0.0.0:8181"
  endpoint_url: "https://oauth.zrok.example.com"
  cookie_name: "zrok_auth"
  cookie_domain: ".zrok.example.com"
  session_lifetime: "6h"
  signing_key: "your-random-32-character-signing-key-here-minimum"
  encryption_key: "24-character-encryption"
  providers:
    - name: "google"
      type: "google"
      client_id: "your-google-client-id.apps.googleusercontent.com"
      client_secret: "your-google-client-secret"
```

### B. Useful Commands Reference

#### Admin Commands

```bash
# Account management
zrok admin create account <email> <password>
zrok admin list accounts
zrok admin delete account <email>

# Frontend management
zrok admin list frontends
zrok admin update frontend <token> --url-template "https://{{.Token}}.zrok.example.com"

# Organization management
zrok admin create organization <name> <description>
zrok admin list organizations
zrok admin add organization-member <org-id> <email> [--admin]
zrok admin list organization-members <org-id>
zrok admin remove organization-member <org-id> <email>
zrok admin delete organization <org-id>

# Environment management
zrok admin list environments
```

#### Client Commands

```bash
# Configuration
zrok config set apiEndpoint <url>
zrok config unset apiEndpoint

# Environment
zrok enable <token>
zrok disable
zrok status [--secrets]

# Sharing
zrok share public [--backend-mode <mode>] <target>
zrok share private [--backend-mode <mode>] <target>
zrok reserve public [--unique-name <name>] [--backend-mode <mode>] <target>
zrok reserve private [--unique-name <name>] [--backend-mode <mode>] <target>
zrok release <share-token>

# Access
zrok access public <frontend-token>
zrok access private <share-token>

# Testing
zrok test loop public --iterations <n>
zrok test loop private <share-token> --iterations <n>

# Invitations
zrok invite
```

### C. Port Reference

| Port | Service | Protocol | Purpose |
|------|---------|----------|---------|
| 80 | NGINX | HTTP | Redirect to HTTPS |
| 443 | NGINX | HTTPS | Public web traffic |
| 1280 | Ziti Controller | TCP | Control plane |
| 3022 | Ziti Router | TCP | Data plane |
| 8080 | zrok Frontend | HTTP | Share traffic (internal) |
| 8181 | zrok OAuth | HTTP | OAuth flows (internal) |
| 18080 | zrok Controller | HTTP | API (internal) |
| 5672 | RabbitMQ | AMQP | Metrics queue (internal) |
| 8086 | InfluxDB | HTTP | Metrics storage (internal) |
| 3000 | Grafana | HTTP | Metrics dashboard (internal) |

### D. Backup and Restore

#### Backup

```bash
#!/bin/bash
# backup-zrok.sh

BACKUP_DIR="/backup/zrok/$(date +%Y%m%d)"
mkdir -p "$BACKUP_DIR"

# Stop services
sudo systemctl stop zrok-controller zrok-frontend

# Backup database
cp /var/lib/zrok/zrok.db "$BACKUP_DIR/"

# Backup configurations
cp -r /etc/zrok "$BACKUP_DIR/"
cp -r /opt/openziti/etc "$BACKUP_DIR/openziti-etc"

# Backup OpenZiti data
sudo cp -r /var/lib/ziti-controller "$BACKUP_DIR/"
sudo cp -r /var/lib/ziti-router "$BACKUP_DIR/"

# Backup certificates
sudo cp -r /etc/letsencrypt "$BACKUP_DIR/"

# Create archive
cd /backup/zrok
tar czf "zrok-backup-$(date +%Y%m%d-%H%M%S).tar.gz" "$(date +%Y%m%d)"

# Start services
sudo systemctl start zrok-controller zrok-frontend

echo "Backup complete: $BACKUP_DIR"
```

#### Restore

```bash
#!/bin/bash
# restore-zrok.sh

BACKUP_FILE="$1"

if [ -z "$BACKUP_FILE" ]; then
    echo "Usage: $0 <backup-file.tar.gz>"
    exit 1
fi

# Extract backup
RESTORE_DIR="/tmp/zrok-restore"
mkdir -p "$RESTORE_DIR"
tar xzf "$BACKUP_FILE" -C "$RESTORE_DIR"

# Stop services
sudo systemctl stop zrok-controller zrok-frontend ziti-controller ziti-router

# Restore database
cp "$RESTORE_DIR"/*/zrok.db /var/lib/zrok/

# Restore configurations
cp -r "$RESTORE_DIR"/*/zrok/* /etc/zrok/
sudo cp -r "$RESTORE_DIR"/*/openziti-etc/* /opt/openziti/etc/

# Restore OpenZiti data
sudo cp -r "$RESTORE_DIR"/*/ziti-controller/* /var/lib/ziti-controller/
sudo cp -r "$RESTORE_DIR"/*/ziti-router/* /var/lib/ziti-router/

# Restore certificates
sudo cp -r "$RESTORE_DIR"/*/letsencrypt/* /etc/letsencrypt/

# Fix permissions
sudo chown -R openziti:openziti /var/lib/ziti-*
sudo chown -R $(whoami):$(whoami) /var/lib/zrok

# Start services
sudo systemctl start ziti-controller ziti-router
sleep 5
sudo systemctl start zrok-controller zrok-frontend

echo "Restore complete"
```

### E. Monitoring Script

```bash
#!/bin/bash
# monitor-zrok.sh

echo "=== zrok Instance Health Check ==="
echo

# Check services
echo "Service Status:"
for service in ziti-controller ziti-router zrok-controller zrok-frontend nginx; do
    if systemctl is-active --quiet "$service"; then
        echo "  ✓ $service: running"
    else
        echo "  ✗ $service: stopped"
    fi
done
echo

# Check ports
echo "Port Status:"
for port in 443 1280 3022; do
    if sudo ss -tlnp | grep -q ":$port "; then
        echo "  ✓ Port $port: listening"
    else
        echo "  ✗ Port $port: not listening"
    fi
done
echo

# Check DNS
echo "DNS Resolution:"
for subdomain in api test123 random; do
    if host "$subdomain.zrok.example.com" > /dev/null 2>&1; then
        echo "  ✓ $subdomain.zrok.example.com: resolves"
    else
        echo "  ✗ $subdomain.zrok.example.com: failed"
    fi
done
echo

# Check API
echo "API Health:"
if curl -sf http://localhost:18080/api/v1/version > /dev/null; then
    echo "  ✓ zrok API: responding"
else
    echo "  ✗ zrok API: not responding"
fi

if curl -skf https://zrok.example.com:1280/health-checks > /dev/null; then
    echo "  ✓ Ziti API: responding"
else
    echo "  ✗ Ziti API: not responding"
fi
echo

# Check disk space
echo "Disk Usage:"
df -h / | tail -1 | awk '{print "  / partition: " $5 " used"}'
echo

# Check database size
echo "Database Size:"
du -h /var/lib/zrok/zrok.db | awk '{print "  zrok database: " $1}'
echo

# Check certificate expiration
echo "Certificate Expiration:"
CERT_FILE="/etc/letsencrypt/live/zrok.example.com/cert.pem"
if [ -f "$CERT_FILE" ]; then
    EXPIRY=$(openssl x509 -enddate -noout -in "$CERT_FILE" | cut -d= -f2)
    EXPIRY_EPOCH=$(date -d "$EXPIRY" +%s)
    NOW_EPOCH=$(date +%s)
    DAYS_LEFT=$(( ($EXPIRY_EPOCH - $NOW_EPOCH) / 86400 ))
    echo "  Certificate expires in $DAYS_LEFT days"
else
    echo "  ✗ Certificate not found"
fi
```

Make executable:

```bash
chmod +x monitor-zrok.sh
./monitor-zrok.sh
```

### F. Security Best Practices

1. **Strong Passwords:** Use 16+ character passwords with mixed case, numbers, symbols
2. **Firewall:** Only expose necessary ports (80, 443, 1280, 3022)
3. **TLS:** Always use HTTPS with valid certificates
4. **Updates:** Regularly update zrok, OpenZiti, and system packages
5. **Backups:** Automated daily backups stored off-site
6. **Monitoring:** Set up alerting for service failures
7. **Limits:** Enforce resource limits to prevent abuse
8. **OAuth:** Use OAuth for public shares when possible
9. **Logs:** Rotate and monitor logs for suspicious activity
10. **Least Privilege:** Run services with minimal permissions

### G. Production Checklist

- [ ] DNS configured with wildcard records
- [ ] Wildcard SSL certificate obtained and auto-renewal configured
- [ ] All services running and enabled at boot
- [ ] NGINX reverse proxy configured
- [ ] Firewall rules properly configured
- [ ] Resource limits enabled and configured
- [ ] Metrics and monitoring set up
- [ ] Backup script automated (daily)
- [ ] Email configured for invitations
- [ ] OAuth providers configured (if needed)
- [ ] Test account created and verified
- [ ] Test share created and accessible
- [ ] Documentation for users prepared
- [ ] Monitoring alerts configured
- [ ] Security hardening completed

### H. Resources

- **Official Documentation:** https://docs.zrok.io
- **GitHub Repository:** https://github.com/openziti/zrok
- **OpenZiti Docs:** https://netfoundry.io/docs/openziti/
- **Community Forum:** https://openziti.discourse.group
- **Discord:** https://openziti.org/discord

---

## Conclusion

You now have a complete, production-ready zrok self-hosting instance with:

- ✅ OpenZiti secure network fabric
- ✅ zrok controller and frontend
- ✅ NGINX reverse proxy with TLS
- ✅ Wildcard DNS and SSL certificates
- ✅ Metrics and monitoring
- ✅ Resource limits and quotas
- ✅ OAuth authentication
- ✅ Organizations support
- ✅ Advanced features (drives, VPN)

Your instance is ready to serve users with secure, zero-trust sharing capabilities!

For questions or issues, consult the troubleshooting section or reach out to the zrok community.

**Happy sharing!** 🚀


# zrok VPN - Complete Feature Guide

**Version:** 1.0
**Last Updated:** 2025-11-12
**Documentation Type:** Feature Guide

---

## Table of Contents

1. [Introduction](#introduction)
2. [What is zrok VPN?](#what-is-zrok-vpn)
3. [Key Features](#key-features)
4. [How zrok VPN Works](#how-zrok-vpn-works)
5. [Network Configuration](#network-configuration)
6. [Platform-Specific Setup](#platform-specific-setup)
7. [Basic Usage](#basic-usage)
8. [Use Cases & Examples](#use-cases--examples)
9. [Advanced Configuration](#advanced-configuration)
10. [Comparison with Traditional VPNs](#comparison-with-traditional-vpns)
11. [Limitations](#limitations)
12. [Troubleshooting](#troubleshooting)
13. [Best Practices](#best-practices)
14. [FAQ](#faq)

---

## Introduction

zrok VPN is a point-to-point Virtual Private Network feature built into zrok that leverages OpenZiti's zero-trust network fabric. Unlike traditional VPN solutions that require complex configuration, port forwarding, and static IPs, zrok VPN provides a simple two-command setup for secure host-to-host connectivity.

### What Makes zrok VPN Different?

- **No Port Forwarding:** Works through NAT and firewalls automatically
- **No Static IP Required:** Dynamic IPs are not a problem
- **Zero Configuration:** No config files, certificates, or keys to manage
- **Identity-Based:** Uses OpenZiti's identity framework for authentication
- **Zero-Trust:** All traffic encrypted end-to-end by default

---

## What is zrok VPN?

zrok VPN is a **point-to-point VPN** feature that creates secure network tunnels between hosts/devices. It enables two or more machines to communicate as if they were on the same local network, regardless of their physical location or network configuration.

### Core Concept

```
┌────────────────────┐                           ┌────────────────────┐
│   Server Machine   │                           │   Client Machine   │
│   (Home Desktop)   │                           │   (Laptop Abroad)  │
│                    │                           │                    │
│   IP: 10.122.0.1   │◄──[Encrypted Tunnel]────►│   IP: 10.122.0.2   │
│                    │   Through Internet        │                    │
│   Services:        │   NAT/Firewall OK         │   Can Access:      │
│   - SSH :22        │                           │   - All server     │
│   - HTTP :80       │                           │     services       │
│   - MySQL :3306    │                           │                    │
└────────────────────┘                           └────────────────────┘
```

### What It Is NOT

- ❌ Not a full internet routing VPN (like commercial VPNs for privacy/anonymity)
- ❌ Not designed to route all your traffic through another location
- ❌ Not a replacement for VPN services like NordVPN or ExpressVPN
- ❌ Not for hiding your public IP address

### What It IS

- ✅ Point-to-point secure connectivity
- ✅ Remote access to specific services
- ✅ Secure tunneling through firewalls
- ✅ Private network between trusted devices
- ✅ Alternative to port forwarding and DDNS

---

## Key Features

### 1. Host-to-Host Connectivity

Creates a virtual network layer that connects devices directly:

```
Device A ←──────[Virtual Network 10.122.0.0/16]──────→ Device B
         Encrypted through OpenZiti network fabric
```

Both devices get IP addresses on a private subnet and can communicate using standard networking tools (SSH, HTTP, RDP, etc.).

### 2. Bidirectional Access

Unlike traditional client-server VPNs, zrok VPN allows **both directions**:

**Server → Client:**
```bash
# Server can SSH into client
ssh user@10.122.0.2

# Server can access client's database
mysql -h 10.122.0.2 -u root -p
```

**Client → Server:**
```bash
# Client can SSH into server
ssh user@10.122.0.1

# Client can access server's web service
curl http://10.122.0.1:8080
```

### 3. Zero-Trust Security

Built on OpenZiti's zero-trust architecture:

- **End-to-End Encryption:** All traffic encrypted automatically
- **Identity-Based Access:** Each device authenticated via OpenZiti identity
- **No Exposed Ports:** No public ports need to be opened
- **Works Through Firewalls:** NAT traversal handled automatically
- **Mutual TLS:** Both endpoints verify each other

### 4. Virtual Network Interface (TUN Device)

Creates a virtual network adapter on each device:

**Linux:**
```bash
$ ip addr show tun0
5: tun0: <POINTOPOINT,MULTICAST,NOARP,UP,LOWER_UP>
    inet 10.122.0.2/16 scope global tun0
```

**macOS:**
```bash
$ ifconfig utun0
utun0: flags=8051<UP,POINTOPOINT,RUNNING,MULTICAST>
    inet 10.122.0.2 --> 10.122.0.1 netmask 0xffff0000
```

**Windows:**
```powershell
PS> ipconfig
Wintun Adapter:
   IPv4 Address. . . . . . . . . . . : 10.122.0.2
   Subnet Mask . . . . . . . . . . . : 255.255.0.0
```

### 5. Multiple Client Support

One server can accept multiple client connections:

```
                    ┌──────────────┐
                    │    Server    │
                    │  10.122.0.1  │
                    └──────┬───────┘
                           │
        ┏━━━━━━━━━━━━━━━━━━┻━━━━━━━━━━━━━━━━━━┓
        ┃                                      ┃
┌───────▼──────┐  ┌───────▼──────┐  ┌─────────▼────┐
│   Client 1   │  │   Client 2   │  │   Client 3   │
│ 10.122.0.2   │  │ 10.122.0.3   │  │ 10.122.0.4   │
└──────────────┘  └──────────────┘  └──────────────┘
```

All clients share the same VPN instance and can potentially communicate with each other (depending on routing configuration).

### 6. Automatic IP Assignment

No need to manually configure IP addresses:

- Server automatically gets `.1` (first IP in subnet)
- Clients receive sequential IPs (`.2`, `.3`, `.4`, etc.)
- DHCP-like behavior without DHCP complexity

### 7. Cross-Platform Support

Works on all major operating systems:

- **Linux:** Ubuntu, Debian, CentOS, Fedora, Arch, etc.
- **macOS:** 10.14+ (Mojave and newer)
- **Windows:** Windows 10, Windows 11, Windows Server

---

## How zrok VPN Works

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Complete Architecture                         │
└─────────────────────────────────────────────────────────────────────┘

Server Machine                                      Client Machine
┌──────────────────────────┐                  ┌──────────────────────────┐
│                          │                  │                          │
│  Application Layer       │                  │  Application Layer       │
│  ┌────────────────────┐  │                  │  ┌────────────────────┐  │
│  │  SSH Server :22    │  │                  │  │  SSH Client        │  │
│  │  Web Server :80    │  │                  │  │  Web Browser       │  │
│  │  Database :3306    │  │                  │  │  Database Client   │  │
│  └─────────┬──────────┘  │                  │  └─────────┬──────────┘  │
│            │              │                  │            │              │
│            ↓              │                  │            ↓              │
│  ┌────────────────────┐  │                  │  ┌────────────────────┐  │
│  │  Network Stack     │  │                  │  │  Network Stack     │  │
│  └─────────┬──────────┘  │                  │  └─────────┬──────────┘  │
│            │              │                  │            │              │
│            ↓              │                  │            ↓              │
│  ┌────────────────────┐  │                  │  ┌────────────────────┐  │
│  │  TUN Device (tun0) │  │                  │  │  TUN Device (tun0) │  │
│  │  IP: 10.122.0.1    │  │                  │  │  IP: 10.122.0.2    │  │
│  └─────────┬──────────┘  │                  │  └─────────┬──────────┘  │
│            │              │                  │            │              │
│            ↓              │                  │            ↓              │
│  ┌────────────────────┐  │                  │  ┌────────────────────┐  │
│  │  zrok share        │  │                  │  │  zrok access       │  │
│  │  --backend-mode vpn│  │                  │  │  private <token>   │  │
│  └─────────┬──────────┘  │                  │  └─────────┬──────────┘  │
│            │              │                  │            │              │
│            ↓              │                  │            ↓              │
│  ┌────────────────────┐  │                  │  ┌────────────────────┐  │
│  │  OpenZiti SDK      │  │                  │  │  OpenZiti SDK      │  │
│  └─────────┬──────────┘  │                  │  └─────────┬──────────┘  │
│            │              │                  │            │              │
└────────────┼──────────────┘                  └────────────┼──────────────┘
             │                                              │
             │                                              │
             └──────────────────┐      ┌───────────────────┘
                                │      │
                                ↓      ↓
                    ┌───────────────────────────┐
                    │   OpenZiti Network        │
                    │   (Controller + Router)   │
                    │                           │
                    │   • Encrypted Tunnels     │
                    │   • NAT Traversal         │
                    │   • Identity Management   │
                    │   • Policy Enforcement    │
                    └───────────────────────────┘
```

### Step-by-Step Connection Process

#### Phase 1: VPN Server Initialization

```bash
# User executes on server
$ sudo -E zrok share private --backend-mode vpn
```

**What happens internally:**

1. **TUN Device Creation**
   ```
   zrok → OS Kernel → Create TUN interface (tun0)
   ```
   - Allocates a virtual network device
   - Registers with kernel's network stack

2. **IP Assignment**
   ```
   Subnet: 10.122.0.0/16 (default)
   Server gets: 10.122.0.1 (first IP)
   ```

3. **Routing Configuration**
   ```bash
   # Automatic route added:
   10.122.0.0/16 dev tun0 proto kernel scope link src 10.122.0.1
   ```

4. **OpenZiti Registration**
   ```
   zrok → OpenZiti Controller → Register VPN service
   • Creates service identity
   • Sets up dial policies
   • Generates share token
   ```

5. **Share Token Generation**
   ```
   Output: Share token: abc123xyz
   ```
   - Token encodes service information
   - Used by clients to connect

**Server is now ready and listening for connections.**

#### Phase 2: VPN Client Connection

```bash
# User executes on client
$ sudo -E zrok access private abc123xyz
```

**What happens internally:**

1. **Token Validation**
   ```
   zrok → OpenZiti Controller → Validate token
   • Check token validity
   • Verify permissions
   • Retrieve service information
   ```

2. **TUN Device Creation**
   ```
   zrok → OS Kernel → Create TUN interface (tun0)
   ```

3. **IP Request & Assignment**
   ```
   Client → Server (through OpenZiti):
   "REQUEST_IP"

   Server → Client:
   "ASSIGNED_IP: 10.122.0.2"
   ```

4. **Network Configuration**
   ```bash
   # Automatic route added:
   10.122.0.0/16 dev tun0 proto kernel scope link src 10.122.0.2
   ```

5. **Tunnel Establishment**
   ```
   Client ←──[OpenZiti Encrypted Circuit]──→ Server
   • Mutual TLS authentication
   • Encrypted data channel
   • Bidirectional communication ready
   ```

**Client is now connected to VPN.**

#### Phase 3: Data Transmission

**Example: Client pings server**

```bash
client$ ping 10.122.0.1
```

**Packet flow:**

```
1. Application Layer (client)
   ping sends ICMP echo request to 10.122.0.1

2. Routing Decision (client)
   Kernel: "10.122.0.1 is on 10.122.0.0/16 network → use tun0"

3. TUN Device (client)
   Packet written to tun0 device

4. zrok Process (client)
   Reads packet from tun0
   Encapsulates: [OpenZiti Header | ICMP Packet]
   Encrypts entire payload

5. Network Transmission
   Encrypted packet → Internet → OpenZiti Router → Server

6. zrok Process (server)
   Receives encrypted packet
   Decrypts payload
   Extracts: [ICMP Packet]

7. TUN Device (server)
   Writes ICMP packet to tun0

8. Kernel Network Stack (server)
   Processes ICMP echo request
   Generates ICMP echo reply

9. Return Path
   Reply goes back through same process (steps 7→1 in reverse)

10. Application Layer (client)
    ping receives ICMP echo reply
    Output: "64 bytes from 10.122.0.1: icmp_seq=1 ttl=64 time=45.2 ms"
```

### Traffic Encryption

All data is encrypted multiple times:

```
Original Packet (e.g., SSH)
    ↓
[SSH Encryption Layer]  ← Application-level encryption
    ↓
[IP Packet]
    ↓
[zrok/OpenZiti Encapsulation]
    ↓
[Mutual TLS Encryption]  ← Transport-level encryption
    ↓
[Transmitted over Internet]
```

**Double encryption** for SSH, HTTPS, etc.
**Single encryption** for plain HTTP, databases, etc.

### Routing Mechanism

**Automatic routing table entries:**

Server side:
```bash
$ ip route show
10.122.0.0/16 dev tun0 proto kernel scope link src 10.122.0.1
```

Client side:
```bash
$ ip route show
10.122.0.0/16 dev tun0 proto kernel scope link src 10.122.0.2
```

**Meaning:**
- Any packet destined for `10.122.x.x` is routed through `tun0`
- Kernel automatically handles packet forwarding
- No manual routing configuration needed

---

## Network Configuration

### Default Network Settings

```yaml
Subnet:           10.122.0.0/16
Netmask:          255.255.0.0
Network Address:  10.122.0.0
Broadcast:        10.122.255.255
First Usable IP:  10.122.0.1 (Server)
Last Usable IP:   10.122.255.254
Total Hosts:      65,534 clients possible
```

### IP Address Allocation

**Automatic DHCP-like assignment:**

```
Connection Order    IP Address        Role
─────────────────────────────────────────────
Server              10.122.0.1        VPN Server
1st Client          10.122.0.2        VPN Client
2nd Client          10.122.0.3        VPN Client
3rd Client          10.122.0.4        VPN Client
...                 ...               ...
65,534th Client     10.122.255.254    VPN Client
```

### Custom Subnet Configuration

You can specify a custom subnet when creating the VPN:

#### Example 1: Small Network (/24)

```bash
# Server with /24 subnet (254 hosts)
sudo -E zrok share private --backend-mode vpn 10.200.0.0/24
```

**Result:**
```yaml
Subnet:           10.200.0.0/24
Netmask:          255.255.255.0
Server IP:        10.200.0.1
Client Range:     10.200.0.2 - 10.200.0.254
Total Clients:    253 maximum
```

#### Example 2: Large Network (/8)

```bash
# Server with /8 subnet (16 million hosts)
sudo -E zrok share private --backend-mode vpn 10.0.0.0/8
```

**Result:**
```yaml
Subnet:           10.0.0.0/8
Netmask:          255.0.0.0
Server IP:        10.0.0.1
Client Range:     10.0.0.2 - 10.255.255.254
Total Clients:    16,777,213 maximum
```

#### Example 3: Custom Private Network

```bash
# Using 172.16.0.0/12 range
sudo -E zrok share private --backend-mode vpn 172.16.0.0/12
```

**Result:**
```yaml
Subnet:           172.16.0.0/12
Netmask:          255.240.0.0
Server IP:        172.16.0.1
Client Range:     172.16.0.2 - 172.31.255.254
Total Clients:    1,048,574 maximum
```

### Subnet Planning Considerations

**Avoid conflicts with existing networks:**

```bash
# Check your current network
ip route show
# or
route -n  # Linux
netstat -rn  # macOS/Windows

# Common conflicts to avoid:
# - 192.168.x.x (home/office networks)
# - 172.16.x.x to 172.31.x.x (corporate networks)
# - 10.x.x.x (various private networks)
```

**Recommended subnets:**

```bash
# Good choices (less likely to conflict):
10.122.0.0/16    # zrok default
10.234.0.0/16    # Random high range
172.29.0.0/16    # Mid-range
192.168.123.0/24 # Uncommon home range
```

### DNS Configuration

zrok VPN does **NOT** provide DNS services by default. You need to configure DNS manually if needed.

**Option 1: Use public DNS**
```bash
# Already works by default (uses system DNS)
ping google.com  # Works
curl https://example.com  # Works
```

**Option 2: Custom DNS for VPN**
```bash
# Linux - add to /etc/resolv.conf
nameserver 10.122.0.1  # If server runs DNS

# Or use systemd-resolved
sudo systemd-resolve --interface tun0 --set-dns 10.122.0.1
```

**Option 3: Run DNS server on VPN server**
```bash
# Server: Install dnsmasq
sudo apt install dnsmasq

# Configure to listen on VPN interface
echo "interface=tun0" | sudo tee -a /etc/dnsmasq.conf
echo "bind-interfaces" | sudo tee -a /etc/dnsmasq.conf

# Clients: Point to server
echo "nameserver 10.122.0.1" | sudo tee -a /etc/resolv.conf
```

---

## Platform-Specific Setup

### Linux

#### Requirements

- **Kernel:** Linux 3.10+ (most modern distributions)
- **TUN Support:** Usually built-in, module: `tun.ko`
- **Privileges:** Root access OR `CAP_NET_ADMIN` capability

#### Method 1: Using sudo (Simplest)

```bash
# Server
sudo -E zrok share private --backend-mode vpn

# Client
sudo -E zrok access private <token>
```

**Note:** `-E` flag preserves environment variables (including zrok configuration path).

#### Method 2: Using Linux Capabilities (No Root Required)

Grant network admin capability to zrok binary:

```bash
# Grant capability
sudo setcap cap_net_admin=eip /usr/local/bin/zrok

# Verify
getcap /usr/local/bin/zrok
# Output: /usr/local/bin/zrok = cap_net_admin+eip

# Now run without sudo
zrok share private --backend-mode vpn
zrok access private <token>
```

**Benefits:**
- No root password needed
- More secure (limited permissions)
- Works in restricted environments

#### Verify TUN Support

```bash
# Check if TUN module is loaded
lsmod | grep tun

# If not loaded, load it
sudo modprobe tun

# Make permanent (load at boot)
echo "tun" | sudo tee -a /etc/modules

# Verify TUN device exists
ls -l /dev/net/tun
# Should show: crw-rw-rw- 1 root root 10, 200 Nov 12 10:00 /dev/net/tun
```

#### Check VPN Interface

```bash
# After starting VPN, check interface
ip addr show tun0

# Example output:
# 5: tun0: <POINTOPOINT,MULTICAST,NOARP,UP,LOWER_UP> mtu 1500 qdisc fq_codel state UNKNOWN group default qlen 500
#     link/none
#     inet 10.122.0.2/16 scope global tun0
#        valid_lft forever preferred_lft forever
```

#### View Routing Table

```bash
# Show all routes
ip route show

# Filter VPN routes
ip route show | grep tun

# Example output:
# 10.122.0.0/16 dev tun0 proto kernel scope link src 10.122.0.2
```

#### Systemd Service (Optional - Auto-Start)

Create service to auto-start VPN:

```bash
# Create service file
sudo tee /etc/systemd/system/zrok-vpn.service <<'EOF'
[Unit]
Description=zrok VPN Client
After=network.target

[Service]
Type=simple
User=root
Environment="ZROK_API_ENDPOINT=https://api.zrok.example.com"
Environment="HOME=/home/yourusername"
ExecStart=/usr/local/bin/zrok access private YOUR_SHARE_TOKEN
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# Enable and start
sudo systemctl daemon-reload
sudo systemctl enable zrok-vpn
sudo systemctl start zrok-vpn

# Check status
sudo systemctl status zrok-vpn
```

---

### macOS

#### Requirements

- **OS Version:** macOS 10.14 (Mojave) or newer
- **TUN/TAP:** Built-in support (utun interfaces)
- **Privileges:** Root access required (mandatory)

#### Usage

```bash
# Server
sudo -E zrok share private --backend-mode vpn

# Client
sudo -E zrok access private <token>
```

**Important:**
- Must use `sudo` with `-E` flag
- Cannot use capabilities like Linux

#### Check VPN Interface

```bash
# List all network interfaces
ifconfig

# Show VPN interface
ifconfig utun0
# or sometimes utun1, utun2, etc.

# Example output:
# utun0: flags=8051<UP,POINTOPOINT,RUNNING,MULTICAST> mtu 1500
#     inet 10.122.0.2 --> 10.122.0.1 netmask 0xffff0000
```

#### View Routing Table

```bash
# Show all routes
netstat -rn

# Filter VPN routes
netstat -rn | grep utun

# Example output:
# 10.122/16          10.122.0.1         UGSc       utun0
```

#### Troubleshooting macOS

**Issue: "Operation not permitted"**
```bash
# Solution: Ensure using sudo
sudo -E zrok share private --backend-mode vpn
```

**Issue: "Cannot create TUN device"**
```bash
# Check System Integrity Protection (SIP) status
csrutil status

# If needed, temporarily disable for testing (requires reboot into Recovery Mode)
# Generally not recommended for production
```

---

### Windows

#### Requirements

- **OS Version:** Windows 10, Windows 11, or Windows Server 2016+
- **Wintun Driver:** **REQUIRED** - must be installed manually
- **Privileges:** Administrator access required

#### Wintun Driver Installation

**Step 1: Download Wintun**

Visit: https://www.wintun.net/

Or direct download:
```
https://www.wintun.net/builds/wintun-0.14.1.zip
```

**Step 2: Extract Driver**

```
wintun-0.14.1.zip
├── wintun/
│   ├── bin/
│   │   ├── amd64/
│   │   │   └── wintun.dll    ← For 64-bit Windows
│   │   ├── arm/
│   │   │   └── wintun.dll
│   │   ├── arm64/
│   │   │   └── wintun.dll
│   │   └── x86/
│   │       └── wintun.dll    ← For 32-bit Windows
```

**Step 3: Install wintun.dll**

Place `wintun.dll` in the **same directory** as `zrok.exe`:

```
C:\Program Files\zrok\
├── zrok.exe
└── wintun.dll    ← Must be here!
```

Or add to system PATH:
```powershell
# Option 1: Copy to Windows\System32
Copy-Item wintun.dll C:\Windows\System32\

# Option 2: Add zrok directory to PATH
$env:PATH += ";C:\Program Files\zrok"
```

#### Usage

**Open PowerShell or Command Prompt as Administrator:**

Right-click → "Run as administrator"

```powershell
# Server
zrok share private --backend-mode vpn

# Client
zrok access private <token>
```

#### Check VPN Interface

```powershell
# View network adapters
ipconfig

# Look for "Wintun Adapter"
# Example output:
# Wintun Adapter:
#    Connection-specific DNS Suffix  . :
#    IPv4 Address. . . . . . . . . . . : 10.122.0.2
#    Subnet Mask . . . . . . . . . . . : 255.255.0.0
#    Default Gateway . . . . . . . . . :
```

#### View Routing Table

```powershell
# Show routes
route print

# Filter for VPN subnet
route print | findstr "10.122"

# Example output:
#     10.122.0.0    255.255.0.0         On-link     10.122.0.2    281
```

#### Windows Firewall

If you have issues accessing services:

```powershell
# Allow traffic on VPN interface
New-NetFirewallRule -DisplayName "zrok VPN" -Direction Inbound -InterfaceAlias "Wintun Adapter" -Action Allow

# Or temporarily disable firewall for testing
Set-NetFirewallProfile -Profile Domain,Public,Private -Enabled False

# Re-enable after testing
Set-NetFirewallProfile -Profile Domain,Public,Private -Enabled True
```

#### Troubleshooting Windows

**Issue: "wintun.dll not found"**
```
Error: Failed to create TUN device: The specified module could not be found.
```
**Solution:**
- Verify `wintun.dll` is in same folder as `zrok.exe`
- Or add to PATH
- Download correct version (amd64 for 64-bit Windows)

**Issue: "Access Denied"**
```
Error: Failed to create TUN device: Access is denied.
```
**Solution:**
- Run as Administrator
- Right-click PowerShell/CMD → "Run as administrator"

**Issue: VPN connects but can't ping**
```
Request timed out.
```
**Solution:**
- Check Windows Firewall
- Allow ICMP (ping) traffic
```powershell
New-NetFirewallRule -DisplayName "Allow ICMPv4-In" -Protocol ICMPv4 -IcmpType 8 -Enabled True -Direction Inbound -Action Allow
```

---

## Basic Usage

### Starting a VPN Server

#### Ephemeral VPN (Temporary)

```bash
# Default subnet (10.122.0.0/16)
sudo -E zrok share private --backend-mode vpn

# Custom subnet
sudo -E zrok share private --backend-mode vpn 192.168.100.0/24

# With verbose logging
sudo -E zrok share private --backend-mode vpn --verbose
```

**Output:**
```
[INFO] allocated 10.122.0.1 for the VPN instance
[INFO] network interface created
[INFO] zrok VPN server started
Share token: wk7b3m9x2q5n
```

**Save the share token!** Clients need this to connect.

#### Reserved VPN (Persistent)

For long-term use, reserve a VPN with a memorable name:

```bash
# Reserve VPN with unique name
sudo -E zrok reserve private --backend-mode vpn --unique-name my-home-vpn

# Start the reserved VPN
sudo -E zrok share private my-home-vpn

# Or use the auto-start feature
sudo -E zrok share private my-home-vpn --headless
```

**Benefits:**
- Same token/name every time
- Can restart without changing client config
- Easy to document and remember

### Connecting as VPN Client

#### Connect to Ephemeral VPN

```bash
# Using share token
sudo -E zrok access private wk7b3m9x2q5n

# With verbose logging
sudo -E zrok access private wk7b3m9x2q5n --verbose
```

**Output:**
```
[INFO] allocated 10.122.0.2
[INFO] network interface created
[INFO] connected to VPN server
```

#### Connect to Reserved VPN

```bash
# Using unique name
sudo -E zrok access private my-home-vpn
```

### Testing Connectivity

After establishing VPN connection:

```bash
# Ping the server
ping 10.122.0.1

# Ping the client (from server)
ping 10.122.0.2

# Check if VPN interface is up
ip addr show tun0  # Linux
ifconfig utun0     # macOS
ipconfig           # Windows

# Test TCP connectivity
nc -zv 10.122.0.1 22  # Test SSH port
telnet 10.122.0.1 80  # Test HTTP port
```

### Accessing Services Over VPN

#### SSH

```bash
# From client to server
ssh username@10.122.0.1

# From server to client
ssh username@10.122.0.2

# With key authentication
ssh -i ~/.ssh/id_rsa username@10.122.0.1
```

#### Web Services

```bash
# HTTP
curl http://10.122.0.1
curl http://10.122.0.1:8080

# HTTPS (if service uses HTTPS)
curl https://10.122.0.1

# Web browser
firefox http://10.122.0.1
```

#### Databases

```bash
# MySQL/MariaDB
mysql -h 10.122.0.1 -u username -p database_name

# PostgreSQL
psql -h 10.122.0.1 -U username -d database_name

# MongoDB
mongo --host 10.122.0.1:27017

# Redis
redis-cli -h 10.122.0.1
```

#### Remote Desktop

```bash
# VNC
vncviewer 10.122.0.1:5900

# RDP (Windows Remote Desktop)
mstsc /v:10.122.0.1

# Or using xfreerdp (Linux)
xfreerdp /v:10.122.0.1 /u:username
```

#### File Sharing

```bash
# SCP (Secure Copy)
scp file.txt username@10.122.0.1:/remote/path/
scp username@10.122.0.1:/remote/file.txt ./

# SFTP
sftp username@10.122.0.1

# SMB/CIFS (Windows shares)
# Linux:
sudo mount -t cifs //10.122.0.1/share /mnt/share -o user=username

# macOS:
# Finder → Go → Connect to Server → smb://10.122.0.1
```

### Stopping VPN

```bash
# Press Ctrl+C in the terminal running zrok
^C

# VPN will disconnect and TUN interface will be removed
```

### Viewing Active VPN Connections

```bash
# Check zrok status
zrok status

# View network statistics
# Linux:
ip -s link show tun0

# macOS:
netstat -I utun0

# Windows:
netsh interface ipv4 show interfaces
```

---

## Use Cases & Examples

### Use Case 1: Remote SSH Access to Home Computer

**Scenario:** Access your home computer from anywhere without port forwarding or dynamic DNS.

**Setup:**

**Home Computer (Server):**
```bash
# Start VPN server
sudo -E zrok share private --backend-mode vpn

# Output:
# Share token: abc123xyz
# Server IP: 10.122.0.1

# Note: SSH server should be running
sudo systemctl status sshd  # Check SSH is running
```

**Laptop (Client - anywhere in the world):**
```bash
# Connect to VPN
sudo -E zrok access private abc123xyz

# Output:
# Client IP: 10.122.0.2

# SSH to home computer
ssh myuser@10.122.0.1

# You're now connected!
```

**Benefits:**
- No router configuration
- No port forwarding
- No dynamic DNS
- Works from coffee shops, offices, hotels
- Encrypted connection through OpenZiti

**Persistent Setup (Recommended):**

```bash
# Home: Reserve VPN with memorable name
sudo -E zrok reserve private --backend-mode vpn --unique-name home-ssh

# Home: Start reserved VPN
sudo -E zrok share private home-ssh

# Laptop: Connect using name (same every time)
sudo -E zrok access private home-ssh
ssh myuser@10.122.0.1
```

---

### Use Case 2: Secure Database Access

**Scenario:** Access production database from laptop without exposing database to internet.

**Database Server:**
```bash
# MySQL/PostgreSQL running on localhost
# Start VPN
sudo -E zrok share private --backend-mode vpn

# Share token: db-vpn-token
# Server IP: 10.122.0.1
```

**Developer Laptop:**
```bash
# Connect to VPN
sudo -E zrok access private db-vpn-token

# Client IP: 10.122.0.2

# Connect to MySQL
mysql -h 10.122.0.1 -u dbuser -p mydatabase

# Or PostgreSQL
psql -h 10.122.0.1 -U dbuser -d mydatabase

# Use database tools
# MySQL Workbench: Connect to 10.122.0.1:3306
# pgAdmin: Connect to 10.122.0.1:5432
# DBeaver: Connect to 10.122.0.1
```

**Benefits:**
- Database never exposed to internet
- No need for database firewall rules
- Encrypted connection
- Same as local access

**With Reserved VPN:**
```bash
# Server: Reserve database VPN
sudo -E zrok reserve private --backend-mode vpn --unique-name production-db

# Server: Start VPN (can add to systemd)
sudo -E zrok share private production-db

# Developer: Connect (same command every day)
sudo -E zrok access private production-db
mysql -h 10.122.0.1 -u root -p
```

---

### Use Case 3: Remote Desktop / VNC

**Scenario:** Access your desktop computer remotely with full GUI.

**Desktop Computer (Windows/Linux):**

**Setup VNC Server (Linux example):**
```bash
# Install VNC server
sudo apt install tigervnc-standalone-server

# Start VNC server
vncserver :1

# Start zrok VPN
sudo -E zrok share private --backend-mode vpn

# Share token: desktop-vnc-789
# Server IP: 10.122.0.1
```

**Remote Computer:**
```bash
# Connect to VPN
sudo -E zrok access private desktop-vnc-789

# Client IP: 10.122.0.2

# Connect VNC client
vncviewer 10.122.0.1:5901

# Or using RealVNC
# Host: 10.122.0.1:5901
```

**For Windows RDP:**

**Windows Desktop:**
```powershell
# Ensure Remote Desktop is enabled
# Settings → System → Remote Desktop → Enable

# Start zrok VPN (as Administrator)
zrok share private --backend-mode vpn

# Share token: windows-rdp-456
# Server IP: 10.122.0.1
```

**Remote Computer:**
```bash
# Connect to VPN
sudo -E zrok access private windows-rdp-456

# Connect via RDP
mstsc /v:10.122.0.1  # Windows

# Or from Linux
xfreerdp /v:10.122.0.1 /u:username /p:password
remmina  # GUI tool, connect to 10.122.0.1
```

---

### Use Case 4: Accessing Multiple Services

**Scenario:** Connect to multiple services on remote server through single VPN.

**Remote Server:**
```bash
# Services running:
# - SSH: port 22
# - Web server: port 80 & 443
# - MySQL: port 3306
# - Redis: port 6379
# - Custom API: port 8080

# Start VPN
sudo -E zrok reserve private --backend-mode vpn --unique-name multi-service
sudo -E zrok share private multi-service

# Server IP: 10.122.0.1
```

**Your Computer:**
```bash
# Connect once
sudo -E zrok access private multi-service

# Now access everything:

# SSH
ssh admin@10.122.0.1

# Web (via browser or curl)
curl http://10.122.0.1
firefox http://10.122.0.1

# MySQL
mysql -h 10.122.0.1 -u root -p

# Redis
redis-cli -h 10.122.0.1

# Custom API
curl http://10.122.0.1:8080/api/endpoint
```

**Benefits:**
- One VPN connection for all services
- No need to expose individual ports
- All traffic encrypted
- Easy to manage

---

### Use Case 5: Team Access to Shared Resources

**Scenario:** Multiple team members need access to development server.

**Development Server:**
```bash
# Reserve VPN with team name
sudo -E zrok reserve private --backend-mode vpn 10.200.0.0/24 --unique-name team-dev-vpn

# Start VPN
sudo -E zrok share private team-dev-vpn

# Share token with team (or use unique name if they have accounts)
# Server IP: 10.200.0.1
```

**Team Member 1:**
```bash
sudo -E zrok access private team-dev-vpn
# Gets IP: 10.200.0.2

# Access server
ssh dev@10.200.0.1
```

**Team Member 2:**
```bash
sudo -E zrok access private team-dev-vpn
# Gets IP: 10.200.0.3

# Access server
ssh dev@10.200.0.1
```

**Team Member 3:**
```bash
sudo -E zrok access private team-dev-vpn
# Gets IP: 10.200.0.4

# Access server
ssh dev@10.200.0.1
```

**Network Topology:**
```
                     ┌─────────────────┐
                     │  Dev Server     │
                     │  10.200.0.1     │
                     │                 │
                     │  Services:      │
                     │  - SSH :22      │
                     │  - Web :80      │
                     │  - DB :5432     │
                     └────────┬────────┘
                              │
                              │ zrok VPN
                              │ (10.200.0.0/24)
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
┌───────▼──────┐     ┌────────▼───────┐    ┌───────▼──────┐
│  Member 1    │     │   Member 2     │    │  Member 3    │
│  10.200.0.2  │     │   10.200.0.3   │    │  10.200.0.4  │
│              │     │                │    │              │
│  Alice       │     │   Bob          │    │  Carol       │
└──────────────┘     └────────────────┘    └──────────────┘
```

---

### Use Case 6: IoT Device Access

**Scenario:** Access IoT devices (Raspberry Pi, Arduino, etc.) behind NAT.

**Raspberry Pi (at remote location):**
```bash
# Install zrok on Raspberry Pi
wget https://github.com/openziti/zrok/releases/download/vX.X.X/zrok_X.X.X_linux_arm.tar.gz
tar -xzf zrok_*.tar.gz
sudo mv zrok /usr/local/bin/

# Configure zrok
zrok config set apiEndpoint https://api.zrok.example.com
zrok enable <token>

# Start VPN
sudo -E zrok reserve private --backend-mode vpn --unique-name my-iot-devices
sudo -E zrok share private my-iot-devices

# Server IP: 10.122.0.1
```

**Your Computer:**
```bash
# Connect to IoT VPN
sudo -E zrok access private my-iot-devices

# SSH to Raspberry Pi
ssh pi@10.122.0.1

# Access web interface (if running)
firefox http://10.122.0.1

# SCP files
scp sensor_data.csv pi@10.122.0.1:/home/pi/data/
```

---

### Use Case 7: Backup Server Access

**Scenario:** Access offsite backup server securely.

**Backup Server:**
```bash
# Backup storage at remote location
# Running: rsync daemon, NFS, or SMB

# Start VPN
sudo -E zrok reserve private --backend-mode vpn --unique-name backup-server
sudo -E zrok share private backup-server

# Server IP: 10.122.0.1
```

**Main Server (needing backups):**
```bash
# Connect to VPN
sudo -E zrok access private backup-server

# Client IP: 10.122.0.2

# Perform backup via rsync
rsync -avz /data/ 10.122.0.1:/backup/main-server/

# Or mount NFS share
sudo mount -t nfs 10.122.0.1:/backup /mnt/backup

# Or SMB
sudo mount -t cifs //10.122.0.1/backup /mnt/backup -o user=backup
```

**Automated Backup Script:**
```bash
#!/bin/bash
# backup.sh

# Connect to VPN
sudo -E zrok access private backup-server &
VPN_PID=$!

# Wait for VPN to establish
sleep 10

# Perform backup
rsync -avz --delete /data/ 10.122.0.1:/backup/$(hostname)/

# Disconnect VPN
kill $VPN_PID
```

---

### Use Case 8: Gaming Server (Minecraft, etc.)

**Scenario:** Host private game server for friends.

**Game Server:**
```bash
# Minecraft server running on port 25565

# Start VPN with custom subnet
sudo -E zrok share private --backend-mode vpn 10.99.0.0/24

# Share token: game-server-xyz
# Server IP: 10.99.0.1

# Minecraft server config:
# server-ip=10.99.0.1
# server-port=25565
```

**Players:**
```bash
# Each player connects to VPN
sudo -E zrok access private game-server-xyz

# Player IPs: 10.99.0.2, 10.99.0.3, 10.99.0.4, etc.

# Connect Minecraft client to: 10.99.0.1:25565
```

**Benefits:**
- No port forwarding
- Low latency (direct connection through OpenZiti)
- Private server for friends only
- Works with any multiplayer game

---

## Advanced Configuration

### Routing Additional Networks

By default, only the VPN subnet is routed. You can add routes to access additional networks.

#### Scenario: Access Server's Local Network

**Server has:**
- VPN IP: 10.122.0.1
- Local network: 192.168.1.0/24 (home/office network)
- Eth0 connected to 192.168.1.0/24

**Goal:** Client wants to access 192.168.1.x devices through VPN.

**Server Configuration:**

```bash
# 1. Start VPN
sudo -E zrok share private --backend-mode vpn

# 2. Enable IP forwarding
sudo sysctl -w net.ipv4.ip_forward=1

# Make permanent
echo "net.ipv4.ip_forward=1" | sudo tee -a /etc/sysctl.conf

# 3. Configure NAT (masquerading)
sudo iptables -t nat -A POSTROUTING -s 10.122.0.0/16 -o eth0 -j MASQUERADE

# 4. Allow forwarding
sudo iptables -A FORWARD -i tun0 -o eth0 -j ACCEPT
sudo iptables -A FORWARD -i eth0 -o tun0 -m state --state RELATED,ESTABLISHED -j ACCEPT

# 5. Save iptables rules
sudo iptables-save | sudo tee /etc/iptables/rules.v4
```

**Client Configuration:**

```bash
# 1. Connect to VPN
sudo -E zrok access private <token>

# 2. Add route to server's local network
sudo ip route add 192.168.1.0/24 via 10.122.0.1 dev tun0

# 3. Now can access 192.168.1.x devices
ping 192.168.1.100
ssh user@192.168.1.50
http://192.168.1.10
```

#### Scenario: Client-to-Client Communication

**Enable clients to reach each other through server.**

**Server Configuration:**

```bash
# Start VPN
sudo -E zrok share private --backend-mode vpn

# Enable IP forwarding
sudo sysctl -w net.ipv4.ip_forward=1

# Allow forwarding between VPN clients
sudo iptables -A FORWARD -i tun0 -o tun0 -j ACCEPT
```

**Now clients can communicate:**

```bash
# Client 1 (10.122.0.2) can reach Client 2 (10.122.0.3)
ping 10.122.0.3
ssh user@10.122.0.3
```

### Split Tunneling

Route only specific traffic through VPN, not all traffic.

**Example: Only route 10.122.0.0/16 through VPN**

```bash
# Connect to VPN
sudo -E zrok access private <token>

# VPN automatically routes 10.122.0.0/16

# All other traffic uses default route (normal internet)
```

This is the **default behavior** of zrok VPN—it doesn't route all traffic.

**To route additional specific networks:**

```bash
# Add specific route
sudo ip route add 192.168.50.0/24 via 10.122.0.1 dev tun0

# Traffic to 192.168.50.x goes through VPN
# Everything else uses normal internet
```

### Custom DNS Through VPN

Set up DNS server on VPN server for name resolution.

**Server: Install and Configure dnsmasq**

```bash
# Install DNS server
sudo apt install dnsmasq

# Configure dnsmasq
sudo tee -a /etc/dnsmasq.conf <<EOF
# Listen only on VPN interface
interface=tun0
bind-interfaces

# Set local domain
domain=vpn.local
local=/vpn.local/

# Add DNS records
address=/server.vpn.local/10.122.0.1
address=/db.vpn.local/10.122.0.1
EOF

# Restart dnsmasq
sudo systemctl restart dnsmasq

# Start VPN after dnsmasq is configured
sudo -E zrok share private --backend-mode vpn
```

**Client: Configure DNS**

```bash
# Connect to VPN
sudo -E zrok access private <token>

# Set VPN server as DNS
echo "nameserver 10.122.0.1" | sudo tee /etc/resolv.conf

# Now can use DNS names
ping server.vpn.local
ssh user@server.vpn.local
curl http://db.vpn.local
```

**Persistent DNS Configuration (systemd-resolved):**

```bash
# Configure resolved
sudo tee /etc/systemd/resolved.conf.d/vpn.conf <<EOF
[Resolve]
DNS=10.122.0.1
Domains=~vpn.local
EOF

# Restart resolved
sudo systemctl restart systemd-resolved
```

### Traffic Monitoring and Analysis

#### Monitor VPN Traffic

```bash
# Real-time traffic monitoring
sudo tcpdump -i tun0

# Specific protocol
sudo tcpdump -i tun0 tcp
sudo tcpdump -i tun0 icmp

# Specific host
sudo tcpdump -i tun0 host 10.122.0.2

# Save to file for analysis
sudo tcpdump -i tun0 -w vpn-traffic.pcap

# Analyze with Wireshark
wireshark vpn-traffic.pcap
```

#### Bandwidth Monitoring

```bash
# Install iftop
sudo apt install iftop

# Monitor VPN bandwidth
sudo iftop -i tun0

# Alternative: nload
sudo apt install nload
sudo nload tun0
```

#### Connection Statistics

```bash
# Linux: Interface statistics
ip -s link show tun0

# Detailed statistics
netstat -i | grep tun

# Connection tracking
sudo conntrack -L | grep 10.122
```

### Firewall Configuration

#### Server-Side Firewall Rules

```bash
# Allow VPN subnet to access services
sudo ufw allow from 10.122.0.0/16

# Allow specific services only
sudo ufw allow from 10.122.0.0/16 to any port 22 proto tcp   # SSH
sudo ufw allow from 10.122.0.0/16 to any port 80 proto tcp   # HTTP
sudo ufw allow from 10.122.0.0/16 to any port 443 proto tcp  # HTTPS
sudo ufw allow from 10.122.0.0/16 to any port 3306 proto tcp # MySQL

# Block specific client
sudo ufw deny from 10.122.0.5
```

#### Client-Side Firewall

```bash
# Allow incoming from VPN subnet
sudo ufw allow from 10.122.0.0/16

# Allow specific service for VPN only
sudo ufw allow from 10.122.0.1 to any port 22 proto tcp
```

### Performance Tuning

#### MTU Optimization

```bash
# Check current MTU
ip link show tun0 | grep mtu

# Adjust MTU (if needed)
sudo ip link set dev tun0 mtu 1400

# Test optimal MTU
ping -M do -s 1372 10.122.0.1  # Start with 1372
# If works, increase by 10
# If fails, decrease by 10
# Optimal = largest size that works + 28
```

#### TCP Tuning

```bash
# Increase TCP buffer sizes
sudo sysctl -w net.core.rmem_max=16777216
sudo sysctl -w net.core.wmem_max=16777216
sudo sysctl -w net.ipv4.tcp_rmem="4096 87380 16777216"
sudo sysctl -w net.ipv4.tcp_wmem="4096 65536 16777216"

# Enable TCP window scaling
sudo sysctl -w net.ipv4.tcp_window_scaling=1

# Make permanent
sudo tee -a /etc/sysctl.conf <<EOF
net.core.rmem_max=16777216
net.core.wmem_max=16777216
net.ipv4.tcp_rmem=4096 87380 16777216
net.ipv4.tcp_wmem=4096 65536 16777216
net.ipv4.tcp_window_scaling=1
EOF
```

### Persistence and Auto-Start

#### systemd Service (Server)

```bash
# Create service
sudo tee /etc/systemd/system/zrok-vpn-server.service <<'EOF'
[Unit]
Description=zrok VPN Server
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=root
Environment="ZROK_API_ENDPOINT=https://api.zrok.example.com"
Environment="HOME=/home/yourusername"
ExecStart=/usr/local/bin/zrok share private my-vpn
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# Enable and start
sudo systemctl daemon-reload
sudo systemctl enable zrok-vpn-server
sudo systemctl start zrok-vpn-server
```

#### systemd Service (Client)

```bash
# Create service
sudo tee /etc/systemd/system/zrok-vpn-client.service <<'EOF'
[Unit]
Description=zrok VPN Client
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=root
Environment="ZROK_API_ENDPOINT=https://api.zrok.example.com"
Environment="HOME=/home/yourusername"
ExecStart=/usr/local/bin/zrok access private my-vpn
ExecStartPost=/bin/sleep 5
ExecStartPost=/usr/sbin/ip route add 192.168.1.0/24 via 10.122.0.1 dev tun0
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# Enable and start
sudo systemctl daemon-reload
sudo systemctl enable zrok-vpn-client
sudo systemctl start zrok-vpn-client
```

---

## Comparison with Traditional VPNs

### Feature Comparison Table

| Feature | zrok VPN | OpenVPN | WireGuard | IPsec |
|---------|----------|---------|-----------|-------|
| **Setup Complexity** | ⭐⭐⭐⭐⭐ Very Easy | ⭐⭐ Complex | ⭐⭐⭐⭐ Easy | ⭐ Very Complex |
| **Configuration** | 2 commands | Multiple config files | Config file + keys | Multiple config files |
| **Certificates** | Automatic (OpenZiti) | Manual PKI setup | Manual key generation | Manual PKI setup |
| **Port Forwarding** | Not needed | Required | Required | Required |
| **NAT Traversal** | Automatic | Difficult | Difficult | Very Difficult |
| **Dynamic IP** | No problem | Requires DDNS | Requires DDNS | Requires DDNS |
| **Firewall Friendly** | Yes (works through) | Sometimes blocked | Sometimes blocked | Often blocked |
| **Performance** | Good | Good | Excellent | Good |
| **Encryption** | OpenZiti (TLS) | OpenSSL/TLS | ChaCha20-Poly1305 | AES/3DES |
| **Architecture** | Zero-trust mesh | Client-server | Peer-to-peer | Tunnel mode |
| **Identity Management** | Built-in (OpenZiti) | Certificate-based | Key-based | Certificate-based |
| **Multi-client** | Yes | Yes | Yes (manual) | Yes |
| **Cross-platform** | Linux/macOS/Windows | Linux/macOS/Windows | Linux/macOS/Windows | Linux/some UNIX |
| **Mobile Support** | Limited | Yes (apps available) | Yes (apps available) | Yes |
| **Full Tunnel** | No | Yes | Yes | Yes |
| **Split Tunnel** | Default | Configurable | Configurable | Configurable |
| **Use Case** | Point-to-point access | Full VPN solution | Fast VPN solution | Enterprise VPN |

### When to Use zrok VPN

**Best for:**
- Quick remote access to specific services
- Accessing home/office computers remotely
- Database connections from developers
- IoT device access
- Small team collaboration
- Situations where port forwarding is not possible
- Environments behind restrictive firewalls
- Dynamic IP scenarios

**Example scenarios:**
```
✅ "I need to SSH to my home computer from work"
✅ "Access my office database from home"
✅ "Connect to Raspberry Pi at remote location"
✅ "Access multiple services on remote server"
✅ "Team needs access to dev server"
```

### When to Use Traditional VPN

**Better alternatives:**

**Use OpenVPN when:**
- Need full internet routing
- Require mobile app support
- Need to route all traffic through VPN
- Corporate VPN requirements
- Need fine-grained policy control

**Use WireGuard when:**
- Need maximum performance
- Want modern cryptography
- Need mobile support
- Simple peer-to-peer VPN
- Prefer minimal code footprint

**Use IPsec when:**
- Enterprise site-to-site VPN
- Compliance requirements (FIPS, etc.)
- Need hardware acceleration
- Interoperability with existing infrastructure

### Architecture Differences

#### zrok VPN Architecture
```
Client ←→ OpenZiti Network ←→ Server
      (Identity-based routing)
```
- Identity-driven
- Application-embedded
- Zero-trust by default

#### Traditional VPN Architecture
```
Client ←→ VPN Gateway ←→ Private Network
      (IP-based routing)
```
- IP-driven
- Network-level
- Trust-based on network perimeter

---

## Limitations

### 1. Not a Full VPN Solution

**What it doesn't do:**

```bash
# ❌ Cannot route ALL internet traffic through VPN
# (unlike commercial VPNs for privacy)

# ❌ Cannot be used to:
# - Change your public IP
# - Access geo-restricted content
# - Hide browsing from ISP (for all traffic)
# - Anonymize internet usage
```

**What it DOES:**

```bash
# ✅ Routes ONLY VPN subnet traffic
# ✅ Access specific remote services
# ✅ Point-to-point connectivity
# ✅ Secure service access
```

### 2. Both Ends Need zrok

**Limitation:**
- Server and client both require zrok installed
- Cannot connect standard VPN clients (OpenVPN, WireGuard clients)
- No native mobile apps

**Workaround:**
```bash
# Install zrok on all devices
# Termux on Android: possible but limited
# iOS: currently not supported
```

### 3. Platform-Specific Requirements

**Linux:**
- Requires root OR `CAP_NET_ADMIN` capability
- TUN/TAP kernel support needed

**macOS:**
- Requires root access (mandatory)
- No capability alternative

**Windows:**
- Requires Administrator privileges
- **Must** have Wintun driver installed
- Extra setup step

### 4. Performance Overhead

**Added latency:**
```
Direct connection:    5ms
Through zrok VPN:     15-30ms (additional 10-25ms overhead)
```

**Factors:**
- OpenZiti routing
- Encryption overhead
- Network path through OpenZiti infrastructure

**Not optimal for:**
- Real-time gaming (high-speed FPS games)
- Video streaming (large files)
- High-bandwidth applications
- Latency-sensitive applications

**Acceptable for:**
- SSH sessions
- Web browsing
- Database queries
- File transfers
- VNC/RDP
- Most typical use cases

### 5. Subnet Planning Required

**Issue:** Must avoid IP conflicts

```bash
# Bad: Conflicts with local network
Server network: 192.168.1.0/24
VPN subnet:     192.168.1.0/24  # ❌ Conflict!

# Good: Different subnet
Server network: 192.168.1.0/24
VPN subnet:     10.122.0.0/16   # ✅ No conflict
```

**Must check:**
- Local network subnet
- Office network subnet
- Home network subnet
- Other VPN subnets

### 6. No Built-in DNS

**Limitation:**
- zrok VPN does not include DNS server
- Must configure DNS manually
- Cannot resolve custom hostnames by default

**Workaround:**
```bash
# Option 1: Use IP addresses
ssh user@10.122.0.1  # Works

# Option 2: Edit /etc/hosts
echo "10.122.0.1 myserver" | sudo tee -a /etc/hosts
ssh user@myserver  # Now works

# Option 3: Setup dnsmasq on server (see Advanced section)
```

### 7. Client Discovery

**Issue:** Clients don't automatically know each other's IPs

```bash
# Client 1 doesn't know Client 2's IP without asking server
```

**Workaround:**
```bash
# Check logs on server to see connected clients
# Or implement out-of-band communication (chat, email, etc.)
```

### 8. No QoS or Traffic Shaping

**Limitation:**
- No built-in Quality of Service
- No bandwidth limiting per client
- No traffic prioritization

**Impact:**
- One client can saturate bandwidth
- No guarantee of service quality

### 9. IPv4 Only

**Current limitation:**
- Only IPv4 supported
- No IPv6 support (yet)

```bash
# Works
10.122.0.1  # ✅ IPv4

# Doesn't work
fd00::1     # ❌ IPv6
```

### 10. Session Persistence

**Behavior:**
- VPN disconnects if `zrok` process terminates
- No automatic reconnection
- Client IPs may change on reconnection

**Solution:**
```bash
# Use systemd service for auto-restart
# See "Persistence and Auto-Start" section
```

---

## Troubleshooting

### VPN Won't Start

#### Issue: "Permission denied" Error

```
Error: Failed to create TUN device: permission denied
```

**Solution (Linux):**

```bash
# Option 1: Use sudo
sudo -E zrok share private --backend-mode vpn

# Option 2: Grant capability
sudo setcap cap_net_admin=eip /usr/local/bin/zrok
zrok share private --backend-mode vpn
```

**Solution (macOS):**

```bash
# Must use sudo (no alternative)
sudo -E zrok share private --backend-mode vpn
```

**Solution (Windows):**

```powershell
# Run as Administrator
# Right-click PowerShell → "Run as administrator"
zrok share private --backend-mode vpn
```

#### Issue: TUN Module Not Available

```
Error: Failed to create TUN device: No such device
```

**Solution (Linux):**

```bash
# Check if TUN is available
ls -l /dev/net/tun

# If not, load module
sudo modprobe tun

# Verify
lsmod | grep tun

# Make permanent
echo "tun" | sudo tee -a /etc/modules
```

#### Issue: "wintun.dll not found" (Windows)

```
Error: Failed to create TUN device: The specified module could not be found.
```

**Solution:**

1. Download Wintun from https://www.wintun.net/
2. Extract `wintun.dll` from `bin/amd64/` (for 64-bit)
3. Place in same folder as `zrok.exe`
4. Verify:

```powershell
dir "C:\Program Files\zrok"
# Should show both zrok.exe and wintun.dll
```

### Cannot Ping Other Side

#### Issue: VPN Connected But No Connectivity

```bash
# VPN shows connected
$ zrok status
Environment: abc123

# But ping fails
$ ping 10.122.0.1
Request timeout
```

**Diagnosis:**

```bash
# Check if TUN interface exists
ip addr show tun0  # Should show interface

# Check if IP is assigned
ip addr show tun0 | grep inet
# Should show: inet 10.122.0.2/16

# Check routing
ip route | grep tun
# Should show: 10.122.0.0/16 dev tun0
```

**Solutions:**

**1. Firewall Blocking:**

```bash
# Linux: Check firewall
sudo iptables -L -n | grep tun

# Allow VPN traffic
sudo ufw allow from 10.122.0.0/16
sudo ufw allow to 10.122.0.0/16

# Or disable firewall temporarily for testing
sudo ufw disable
# (Re-enable after: sudo ufw enable)
```

**2. Routing Issue:**

```bash
# Add route manually
sudo ip route add 10.122.0.0/16 dev tun0

# Delete old route if exists
sudo ip route del 10.122.0.0/16
sudo ip route add 10.122.0.0/16 dev tun0
```

**3. Wrong Subnet:**

```bash
# Server started with: 10.122.0.0/16
# Client trying to reach: 192.168.1.1  # ❌ Wrong subnet!

# Solution: Use correct subnet (10.122.x.x)
```

### Connection Drops Frequently

#### Issue: VPN Disconnects Randomly

**Diagnosis:**

```bash
# Check zrok logs
zrok share private --backend-mode vpn --verbose

# Look for errors like:
# - "connection reset"
# - "timeout"
# - "tunnel closed"
```

**Solutions:**

**1. Network Instability:**

```bash
# Use systemd to auto-restart
sudo systemctl status zrok-vpn-client

# Check RestartSec is set
# See "Persistence and Auto-Start" section
```

**2. OpenZiti Controller Unreachable:**

```bash
# Test controller connectivity
curl -k https://api.zrok.example.com

# Check DNS resolution
dig api.zrok.example.com

# Try different network
```

**3. Keepalive Settings:**

```bash
# Enable TCP keepalive (Linux)
sudo sysctl -w net.ipv4.tcp_keepalive_time=60
sudo sysctl -w net.ipv4.tcp_keepalive_intvl=10
sudo sysctl -w net.ipv4.tcp_keepalive_probes=6
```

### High Latency or Slow Performance

#### Issue: VPN is Very Slow

**Diagnosis:**

```bash
# Test latency
ping -c 10 10.122.0.1

# Check MTU
ip link show tun0 | grep mtu

# Monitor bandwidth
sudo iftop -i tun0
```

**Solutions:**

**1. MTU Issues:**

```bash
# Try lower MTU
sudo ip link set dev tun0 mtu 1400

# Test different values
sudo ip link set dev tun0 mtu 1350
```

**2. Network Path:**

```bash
# Check route to OpenZiti
traceroute api.zrok.example.com

# Long routes = higher latency
# Consider different OpenZiti instance closer to you
```

**3. CPU Usage:**

```bash
# Check if zrok process is using too much CPU
top | grep zrok

# If high CPU, possible reasons:
# - Too much traffic
# - Encryption overhead
# - Check for other processes
```

### Cannot Access Services

#### Issue: VPN Connected But Can't Access Services

**Diagnosis:**

```bash
# Ping works
ping 10.122.0.1  # ✅ OK

# But SSH fails
ssh user@10.122.0.1  # ❌ Connection refused
```

**Solutions:**

**1. Service Not Running:**

```bash
# Check if service is actually running
# On server:
sudo systemctl status sshd  # SSH
sudo systemctl status nginx  # Web
sudo systemctl status mysql  # Database
```

**2. Service Listening on Wrong Interface:**

```bash
# Check what interfaces service listens on
sudo netstat -tlnp | grep :22

# If shows:
# 127.0.0.1:22  # ❌ Only localhost
# Should be:
# 0.0.0.0:22    # ✅ All interfaces
# or
# 10.122.0.1:22 # ✅ VPN interface

# Fix SSH example:
# Edit /etc/ssh/sshd_config
# Change: ListenAddress 127.0.0.1
# To:     ListenAddress 0.0.0.0
sudo systemctl restart sshd
```

**3. Firewall on Server:**

```bash
# Allow VPN subnet
sudo ufw allow from 10.122.0.0/16 to any port 22
sudo ufw allow from 10.122.0.0/16 to any port 80
```

**4. SELinux (RHEL/CentOS):**

```bash
# Check SELinux status
getenforce

# If Enforcing, may block VPN
# Temporarily disable for testing
sudo setenforce 0

# If that fixes it, create proper SELinux policy
# Or switch to Permissive mode
```

### IP Address Conflicts

#### Issue: "Address already in use"

```bash
Error: cannot assign requested address: 10.122.0.1
```

**Cause:** Another interface using same subnet

**Diagnosis:**

```bash
# Check all interfaces
ip addr show | grep 10.122

# Check routing table
ip route | grep 10.122
```

**Solution:**

```bash
# Option 1: Remove conflicting interface
sudo ip link set dev <conflicting-interface> down

# Option 2: Use different subnet
sudo -E zrok share private --backend-mode vpn 10.200.0.0/24

# Option 3: Change local network to avoid conflict
```

### DNS Resolution Issues

#### Issue: Cannot Resolve Hostnames

```bash
# Works
ping 10.122.0.1  # ✅ OK

# Doesn't work
ping myserver  # ❌ Name resolution failed
```

**Solution:**

```bash
# Option 1: Use IP addresses
ssh user@10.122.0.1

# Option 2: Edit /etc/hosts
echo "10.122.0.1 myserver" | sudo tee -a /etc/hosts
ssh user@myserver

# Option 3: Set up DNS server (see Advanced Configuration)
```

### Multiple Clients Issues

#### Issue: Clients Can't Reach Each Other

**Scenario:**
- Client 1: 10.122.0.2
- Client 2: 10.122.0.3
- Client 1 cannot ping Client 2

**Solution:**

**On Server:**

```bash
# Enable IP forwarding
sudo sysctl -w net.ipv4.ip_forward=1

# Allow forwarding between VPN clients
sudo iptables -A FORWARD -i tun0 -o tun0 -j ACCEPT
sudo iptables -A FORWARD -i tun0 -o tun0 -m state --state RELATED,ESTABLISHED -j ACCEPT

# Save rules
sudo iptables-save | sudo tee /etc/iptables/rules.v4
```

### Windows-Specific Issues

#### Issue: VPN Starts But No Network

**Diagnosis:**

```powershell
# Check adapter
ipconfig
# Look for "Wintun Adapter"

# Check if it has IP
# Should show: IPv4 Address . . . : 10.122.0.2
```

**Solution:**

```powershell
# Disable and re-enable adapter
netsh interface set interface "Wintun Adapter" admin=disable
netsh interface set interface "Wintun Adapter" admin=enable

# Or restart zrok
```

#### Issue: Firewall Blocking

**Solution:**

```powershell
# Allow VPN traffic
New-NetFirewallRule -DisplayName "zrok VPN" -Direction Inbound -Action Allow -InterfaceAlias "Wintun Adapter"
New-NetFirewallRule -DisplayName "zrok VPN" -Direction Outbound -Action Allow -InterfaceAlias "Wintun Adapter"
```

### Logs and Debugging

#### Enable Verbose Logging

```bash
# Server
sudo -E zrok share private --backend-mode vpn --verbose

# Client
sudo -E zrok access private <token> --verbose
```

#### Check System Logs

```bash
# Linux
sudo journalctl -xe | grep zrok
sudo dmesg | grep tun

# macOS
log show --predicate 'process == "zrok"' --last 1h

# Windows
# Event Viewer → Windows Logs → Application
# Filter for "zrok"
```

---

## Best Practices

### 1. Use Reserved VPNs for Persistent Access

```bash
# Don't rely on ephemeral tokens
# Instead, reserve with memorable names

sudo -E zrok reserve private --backend-mode vpn --unique-name my-home-vpn
sudo -E zrok share private my-home-vpn
```

**Benefits:**
- Same connection string every time
- Easy to document
- Can restart without changing client config

### 2. Plan Your Subnet Carefully

```bash
# Check existing networks first
ip route show  # Linux
netstat -rn    # macOS/Windows

# Choose non-conflicting subnet
# Good defaults:
10.122.0.0/16   # zrok default
10.234.0.0/16   # Alternative
172.29.0.0/16   # Alternative
```

### 3. Automate with systemd

```bash
# Don't rely on manual starts
# Create systemd services for auto-start and restart

# See "Advanced Configuration → Persistence and Auto-Start"
```

### 4. Document Your VPN Configuration

```bash
# Create a README for your team
cat > VPN-README.md <<EOF
# Team VPN Access

## Server
- Subnet: 10.200.0.0/24
- Server IP: 10.200.0.1
- Share name: team-dev-vpn

## Services Available
- SSH: 10.200.0.1:22
- Web: 10.200.0.1:80
- Database: 10.200.0.1:5432

## Connect
\`\`\`bash
sudo -E zrok access private team-dev-vpn
\`\`\`

## Troubleshooting
Contact: admin@example.com
EOF
```

### 5. Secure Your Share Tokens

```bash
# Don't share tokens publicly
# Use secure channels (encrypted chat, password manager)

# For teams, consider:
# - Using reserved names with account-based access
# - Rotating tokens periodically
# - Limiting who has access
```

### 6. Monitor VPN Usage

```bash
# Log connections
zrok share private --backend-mode vpn --verbose 2>&1 | tee vpn.log

# Monitor bandwidth
sudo iftop -i tun0

# Check connected clients
# (Check logs for connection messages)
```

### 7. Firewall Configuration

```bash
# Only allow VPN subnet to access sensitive services
sudo ufw default deny incoming
sudo ufw allow from 10.122.0.0/16 to any port 22
sudo ufw allow from 10.122.0.0/16 to any port 3306
sudo ufw enable
```

### 8. Regular Backups

```bash
# Backup zrok configuration
cp -r ~/.zrok ~/backups/zrok-config-$(date +%Y%m%d)

# Especially important if using reserved shares
```

### 9. Test Before Production

```bash
# Test connectivity before relying on VPN
ping 10.122.0.1
ssh -v user@10.122.0.1  # Verbose SSH for troubleshooting

# Test failover/reconnection
# Kill and restart VPN, ensure services still accessible
```

### 10. Use Appropriate Subnet Size

```bash
# Don't use /8 for 2 clients
# Don't use /30 for 10 clients

# Recommendations:
# 2-10 clients:   /28 (16 addresses)
# 10-50 clients:  /24 (256 addresses)
# 50-100 clients: /23 (512 addresses)
# 100+ clients:   /22 or larger

# Example for small team (10 people)
sudo -E zrok share private --backend-mode vpn 10.200.0.0/24
```

---

## FAQ

### Q: Can I use zrok VPN to access Netflix or bypass geo-restrictions?

**A:** No. zrok VPN is not designed for full internet routing or anonymization. It's for point-to-point access to specific services.

---

### Q: How many clients can connect to one server?

**A:** Depends on subnet size:
- `/16` subnet: 65,534 clients
- `/24` subnet: 254 clients
- `/28` subnet: 14 clients

Practical limit depends on server resources and bandwidth.

---

### Q: Can I use zrok VPN on mobile (Android/iOS)?

**A:**
- **Android:** Possible using Termux, but limited and unofficial
- **iOS:** Currently not supported (no TUN support in iOS apps without special entitlements)

---

### Q: Does zrok VPN work in China or countries with heavy censorship?

**A:** Maybe. OpenZiti uses HTTPS which may work through firewalls, but:
- Not guaranteed
- Depends on OpenZiti controller accessibility
- May be blocked if detected

---

### Q: Can I route all my traffic through zrok VPN?

**A:** No. zrok VPN only routes the VPN subnet traffic. It's not designed for full tunneling.

---

### Q: What's the performance overhead?

**A:** Typical overhead:
- Latency: +10-30ms
- Bandwidth: ~5-10% reduction due to encryption
- Not suitable for ultra-low-latency or very high-bandwidth applications

---

### Q: Is zrok VPN free?

**A:** Depends on your zrok instance:
- Self-hosted: Free (just server costs)
- zrok.io: Free tier available, check current limits
- Usage limits may apply

---

### Q: Can clients communicate with each other?

**A:** Yes, if server enables IP forwarding:

```bash
# On server
sudo sysctl -w net.ipv4.ip_forward=1
sudo iptables -A FORWARD -i tun0 -o tun0 -j ACCEPT
```

---

### Q: How secure is zrok VPN?

**A:** Very secure:
- End-to-end encryption via OpenZiti
- Mutual TLS authentication
- Zero-trust architecture
- Identity-based access control
- No public ports exposed

---

### Q: Can I use zrok VPN for gaming?

**A:** Yes, but:
- Added latency (10-30ms) may affect fast-paced games
- Good for: turn-based, strategy, co-op games
- Not ideal for: competitive FPS, fighting games

---

### Q: Does zrok VPN support IPv6?

**A:** Not currently. Only IPv4 is supported.

---

### Q: Can I change my VPN IP address?

**A:** Server always gets `.1`. Clients get sequential IPs. To change:
- Disconnect and reconnect (may get different IP)
- Or manually configure routes (advanced)

---

### Q: What happens if server restarts?

**A:**
- VPN disconnects
- Clients lose connection
- Must reconnect
- Client IPs may change

**Solution:** Use systemd for auto-restart.

---

### Q: Can I use zrok VPN with Docker containers?

**A:** Yes:

```bash
# On server with Docker
sudo -E zrok share private --backend-mode vpn

# Access containers
curl http://10.122.0.1:8080  # Container port mapped to host
```

Or run zrok inside container (requires privileged mode).

---

### Q: How do I update zrok?

**A:**

```bash
# Download latest version
wget https://github.com/openziti/zrok/releases/download/vX.X.X/zrok_X.X.X_linux_amd64.tar.gz

# Stop VPN
# (Ctrl+C or sudo systemctl stop zrok-vpn)

# Replace binary
tar -xzf zrok_*.tar.gz
sudo mv zrok /usr/local/bin/

# Restart VPN
sudo -E zrok share private my-vpn
```

---

### Q: Can I use custom domain names?

**A:** Not automatically. You need to:
- Set up DNS server on VPN (dnsmasq)
- Or edit `/etc/hosts` on each device
- Or use split-horizon DNS

See "Advanced Configuration → Custom DNS".

---

### Q: What ports does zrok VPN use?

**A:**
- No public ports needed on server/client
- Uses OpenZiti infrastructure
- OpenZiti controller typically port 1280
- OpenZiti router typically port 3022

Your firewall only needs to allow:
- Outbound HTTPS (443) to OpenZiti controller
- Outbound TCP to OpenZiti router (typically 3022)

---

### Q: Can I use zrok VPN behind corporate firewall?

**A:** Usually yes, because:
- Uses HTTPS (port 443) which is typically allowed
- NAT traversal works automatically
- No inbound connections required

But check your corporate policy first!

---

## Summary

zrok VPN provides a simple, secure way to create point-to-point VPN connections without complex configuration or port forwarding. It's ideal for:

✅ Remote access to services
✅ Database connections
✅ SSH access
✅ IoT device management
✅ Team collaboration
✅ Scenarios where port forwarding isn't possible

Key advantages:
- 2-command setup
- Works through NAT/firewalls
- Zero-trust security
- Cross-platform support
- No manual certificate/key management

**Not suitable for:**
- Full internet routing
- Hiding public IP
- Very high bandwidth needs
- Ultra-low latency requirements
- Mobile access (limited)

For most remote access and service connectivity needs, zrok VPN offers an excellent balance of simplicity, security, and functionality.

---

**End of zrok VPN Complete Guide**

For more information, visit:
- zrok Documentation: https://docs.zrok.io
- GitHub: https://github.com/openziti/zrok
- Community: https://openziti.discourse.group

# Transform zrok VPN into Full-Tunnel VPN Provider

**Guide Version:** 1.0
**Last Updated:** 2025-11-12
**Purpose:** Configure zrok VPN to route ALL client traffic (like commercial VPN providers)

---

## Table of Contents

1. [Introduction](#introduction)
2. [Architecture Overview](#architecture-overview)
3. [Prerequisites](#prerequisites)
4. [Server Configuration](#server-configuration)
5. [Client Configuration](#client-configuration)
6. [DNS Configuration](#dns-configuration)
7. [Verification & Testing](#verification--testing)
8. [Advanced Features](#advanced-features)
9. [Multi-Location Setup](#multi-location-setup)
10. [Performance Optimization](#performance-optimization)
11. [Security Hardening](#security-hardening)
12. [Troubleshooting](#troubleshooting)
13. [Comparison with Commercial VPNs](#comparison-with-commercial-vpns)

---

## Introduction

### What We're Building

Transform zrok VPN from a **point-to-point** service access tool into a **full-tunnel VPN provider** that routes all client traffic through the VPN server—just like ExpressVPN, NordVPN, or Mullvad.

### Default zrok VPN Behavior

```
Client Traffic:
├─ To 10.122.0.0/16     → Through VPN ✓
├─ To google.com        → Direct to Internet ✗
├─ To 192.168.1.1       → Direct to Internet ✗
└─ All other traffic    → Direct to Internet ✗
```

### After Our Configuration

```
Client Traffic:
├─ To 10.122.0.0/16     → Through VPN ✓
├─ To google.com        → Through VPN ✓
├─ To 192.168.1.1       → Through VPN ✓
└─ ALL traffic          → Through VPN ✓
```

### Benefits

**Privacy & Security:**
- ✅ Hide your real IP address
- ✅ Encrypt all internet traffic
- ✅ Bypass geo-restrictions
- ✅ Prevent ISP tracking
- ✅ Public WiFi security

**Flexibility:**
- ✅ Multiple server locations (if you deploy multiple servers)
- ✅ Complete control (no third-party logging)
- ✅ Custom DNS (ad-blocking, privacy DNS)
- ✅ No bandwidth limits (only your server's capacity)
- ✅ Free (just server costs)

**Use Cases:**
- 🌍 Access geo-restricted content
- 🔒 Secure browsing on public WiFi
- 🕵️ Privacy from ISP tracking
- 🚫 Bypass censorship
- 💼 Secure remote work
- 🎮 Gaming with different IP

---

## Architecture Overview

### Network Flow Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                      BEFORE Configuration                         │
└──────────────────────────────────────────────────────────────────┘

Client (10.122.0.2)
├─ VPN traffic (10.122.x.x)  ──→  zrok VPN  ──→  Server
└─ All other traffic         ──→  Direct to Internet


┌──────────────────────────────────────────────────────────────────┐
│                      AFTER Configuration                          │
└──────────────────────────────────────────────────────────────────┘

                          ┌─────────────────────┐
                          │   VPN Server        │
                          │   (Your VPS)        │
                          │   IP: 1.2.3.4       │
                          └──────────┬──────────┘
                                     │
                     ┌───────────────┼───────────────┐
                     │               │               │
              Encrypted Tunnel       │         Forward Traffic
              (OpenZiti)             │         with NAT
                     │               │               │
                     ▼               ▼               ▼
            ┌────────────────┐      │      ┌────────────────┐
            │   Your Client  │      │      │   Internet     │
            │   Real IP:     │      │      │   Sees only    │
            │   98.76.54.32  │      │      │   VPN server IP│
            └────────────────┘      │      │   1.2.3.4      │
                                    │      └────────────────┘
            All Traffic Types:      │
            ├─ Web browsing ────────┼────→ Forwarded & NATed
            ├─ Streaming ───────────┼────→ Forwarded & NATed
            ├─ Torrents ────────────┼────→ Forwarded & NATed
            ├─ DNS queries ─────────┼────→ Forwarded & NATed
            └─ Everything ──────────┴────→ Forwarded & NATed

            Client appears to be at: 1.2.3.4 (VPN server's IP)
```

### Technical Components

**Server Side:**
1. **IP Forwarding:** Enables packet forwarding between interfaces
2. **NAT/Masquerading:** Rewrites source IP to server's public IP
3. **Firewall Rules:** Allows forwarding from VPN to internet
4. **DNS Server:** Provides DNS resolution for clients (optional but recommended)

**Client Side:**
1. **Default Route Change:** Routes all traffic to VPN gateway
2. **DNS Override:** Use VPN server's DNS or custom DNS
3. **Kill Switch:** Blocks traffic if VPN disconnects (optional)

---

## Prerequisites

### Server Requirements

**VPS Specifications:**
- **Location:** Choose based on your needs (US, EU, Asia, etc.)
- **RAM:** 1GB minimum (2GB+ recommended)
- **CPU:** 1 core minimum (2+ cores for multiple clients)
- **Bandwidth:** Unmetered or high quota (your traffic goes through it)
- **OS:** Linux (Ubuntu 22.04, Debian 11+, etc.)

**Network:**
- Public IPv4 address
- Good network connectivity
- Ideally high bandwidth (1 Gbps+)
- Low latency to your location

**Recommended VPS Providers:**
- DigitalOcean (starting $6/month)
- Vultr (starting $5/month)
- Linode (starting $5/month)
- Hetzner (starting €4/month, great EU performance)
- AWS Lightsail (starting $3.50/month)

### Software Prerequisites

**Server:**
```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install required packages
sudo apt install -y \
    iptables \
    iptables-persistent \
    dnsmasq \
    curl \
    wget

# Install zrok (if not already installed)
wget https://github.com/openziti/zrok/releases/download/vX.X.X/zrok_X.X.X_linux_amd64.tar.gz
tar -xzf zrok_*.tar.gz
sudo mv zrok /usr/local/bin/
sudo chmod +x /usr/local/bin/zrok
```

**Client:**
- zrok installed
- Root/administrator access
- Basic networking tools

---

## Server Configuration

### Step 1: Basic Server Setup

```bash
# SSH into your VPS
ssh root@your-vps-ip

# Create zrok user (optional but recommended)
sudo useradd -m -s /bin/bash zrokuser
sudo usermod -aG sudo zrokuser
```

### Step 2: Enable IP Forwarding

IP forwarding allows the server to route packets between interfaces (VPN → Internet).

```bash
# Enable IP forwarding (temporary)
sudo sysctl -w net.ipv4.ip_forward=1
sudo sysctl -w net.ipv6.conf.all.forwarding=1  # IPv6 if needed

# Verify
sysctl net.ipv4.ip_forward
# Should output: net.ipv4.ip_forward = 1

# Make permanent
sudo tee -a /etc/sysctl.conf <<EOF
# Enable IP forwarding for VPN
net.ipv4.ip_forward=1
net.ipv6.conf.all.forwarding=1
EOF

# Apply immediately
sudo sysctl -p
```

### Step 3: Identify Network Interfaces

```bash
# List all network interfaces
ip addr show

# Typical output:
# 1: lo: ...
# 2: eth0: ...  ← Your public internet interface (NOTE THIS NAME!)
# 3: tun0: ...  ← This will appear after starting zrok VPN
```

**Important:** Note your public interface name (commonly `eth0`, `ens3`, `enp0s3`, or `ens5`).

### Step 4: Configure NAT/Masquerading

NAT (Network Address Translation) rewrites outgoing packets so they appear to come from the server's public IP.

```bash
# Replace 'eth0' with your actual interface name
export PUBLIC_INTERFACE="eth0"  # Change if different!
export VPN_SUBNET="10.122.0.0/16"

# Add NAT rule (masquerading)
sudo iptables -t nat -A POSTROUTING -s $VPN_SUBNET -o $PUBLIC_INTERFACE -j MASQUERADE

# Allow forwarding from VPN to internet
sudo iptables -A FORWARD -i tun0 -o $PUBLIC_INTERFACE -j ACCEPT

# Allow return traffic
sudo iptables -A FORWARD -i $PUBLIC_INTERFACE -o tun0 -m state --state RELATED,ESTABLISHED -j ACCEPT

# Allow VPN clients to communicate with each other (optional)
sudo iptables -A FORWARD -i tun0 -o tun0 -j ACCEPT

# Verify rules
sudo iptables -t nat -L -n -v
sudo iptables -L FORWARD -n -v
```

### Step 5: Save Firewall Rules

```bash
# Save iptables rules permanently
sudo netfilter-persistent save

# Or manually save
sudo iptables-save | sudo tee /etc/iptables/rules.v4

# For IPv6 (if enabled)
sudo ip6tables-save | sudo tee /etc/iptables/rules.v6
```

### Step 6: Configure DNS Server (Optional but Recommended)

Set up dnsmasq to provide DNS services to VPN clients.

```bash
# Install dnsmasq (if not already)
sudo apt install dnsmasq -y

# Backup original config
sudo cp /etc/dnsmasq.conf /etc/dnsmasq.conf.backup

# Configure dnsmasq
sudo tee /etc/dnsmasq.conf <<'EOF'
# Listen only on VPN interface
interface=tun0
bind-interfaces

# Don't read /etc/resolv.conf
no-resolv

# Use these DNS servers for upstream queries
server=1.1.1.1         # Cloudflare DNS
server=1.0.0.1         # Cloudflare DNS backup
server=8.8.8.8         # Google DNS backup

# Cache settings
cache-size=1000

# DNS privacy options (optional)
# Enable DNSSEC
# dnssec

# Logging (for troubleshooting, disable in production)
# log-queries
# log-dhcp

# Local domain (optional)
# domain=vpn.local
# local=/vpn.local/
EOF

# Don't start dnsmasq yet (wait until VPN is running)
sudo systemctl stop dnsmasq
sudo systemctl disable dnsmasq  # We'll start it manually after VPN
```

### Step 7: Start zrok VPN Server

```bash
# Configure zrok (if not already)
zrok config set apiEndpoint https://api.zrok.example.com  # Your zrok controller
zrok enable YOUR_ACCOUNT_TOKEN

# Reserve VPN with memorable name
sudo -E zrok reserve private --backend-mode vpn --unique-name full-tunnel-vpn

# Start VPN server
sudo -E zrok share private full-tunnel-vpn

# Output will show:
# [INFO] allocated 10.122.0.1 for the VPN instance
# [INFO] network interface created

# Keep this terminal open or press Ctrl+Z then 'bg' to background
```

**Or create systemd service for auto-start:**

```bash
sudo tee /etc/systemd/system/zrok-vpn-server.service <<'EOF'
[Unit]
Description=zrok VPN Server (Full Tunnel)
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=root
Environment="ZROK_API_ENDPOINT=https://api.zrok.example.com"
Environment="HOME=/root"
ExecStart=/usr/local/bin/zrok share private full-tunnel-vpn
ExecStartPost=/bin/sleep 5
ExecStartPost=/bin/systemctl start dnsmasq
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# Enable and start
sudo systemctl daemon-reload
sudo systemctl enable zrok-vpn-server
sudo systemctl start zrok-vpn-server

# Check status
sudo systemctl status zrok-vpn-server
```

### Step 8: Verify Server Configuration

```bash
# 1. Check VPN interface exists
ip addr show tun0
# Should show: inet 10.122.0.1/16

# 2. Check IP forwarding is enabled
cat /proc/sys/net/ipv4/ip_forward
# Should output: 1

# 3. Check NAT rules
sudo iptables -t nat -L -n -v | grep MASQUERADE
# Should show rule for 10.122.0.0/16

# 4. Check forwarding rules
sudo iptables -L FORWARD -n -v
# Should show ACCEPT rules for tun0

# 5. Test DNS (if configured)
sudo systemctl status dnsmasq
# Should be active (running)

# 6. Get server's public IP (this is what clients will appear as)
curl -4 ifconfig.me
# Note this IP!
```

### Step 9: Server Configuration Script (All-in-One)

For easy deployment, here's a complete setup script:

```bash
#!/bin/bash
# setup-vpn-server.sh

set -e

echo "=== zrok VPN Full-Tunnel Server Setup ==="

# Variables
PUBLIC_INTERFACE="eth0"  # Change if different!
VPN_SUBNET="10.122.0.0/16"
VPN_NAME="full-tunnel-vpn"
ZROK_API="https://api.zrok.example.com"  # Change to your controller

# Check if running as root
if [ "$EUID" -ne 0 ]; then
  echo "Please run as root (sudo)"
  exit 1
fi

# 1. Enable IP forwarding
echo "Enabling IP forwarding..."
sysctl -w net.ipv4.ip_forward=1
grep -qxF 'net.ipv4.ip_forward=1' /etc/sysctl.conf || echo 'net.ipv4.ip_forward=1' >> /etc/sysctl.conf

# 2. Install dependencies
echo "Installing dependencies..."
apt update
apt install -y iptables iptables-persistent dnsmasq

# 3. Configure firewall
echo "Configuring firewall rules..."
iptables -t nat -C POSTROUTING -s $VPN_SUBNET -o $PUBLIC_INTERFACE -j MASQUERADE 2>/dev/null || \
  iptables -t nat -A POSTROUTING -s $VPN_SUBNET -o $PUBLIC_INTERFACE -j MASQUERADE

iptables -C FORWARD -i tun0 -o $PUBLIC_INTERFACE -j ACCEPT 2>/dev/null || \
  iptables -A FORWARD -i tun0 -o $PUBLIC_INTERFACE -j ACCEPT

iptables -C FORWARD -i $PUBLIC_INTERFACE -o tun0 -m state --state RELATED,ESTABLISHED -j ACCEPT 2>/dev/null || \
  iptables -A FORWARD -i $PUBLIC_INTERFACE -o tun0 -m state --state RELATED,ESTABLISHED -j ACCEPT

iptables -C FORWARD -i tun0 -o tun0 -j ACCEPT 2>/dev/null || \
  iptables -A FORWARD -i tun0 -o tun0 -j ACCEPT

# Save rules
netfilter-persistent save

# 4. Configure dnsmasq
echo "Configuring DNS server..."
cat > /etc/dnsmasq.conf <<'DNSEOF'
interface=tun0
bind-interfaces
no-resolv
server=1.1.1.1
server=1.0.0.1
server=8.8.8.8
cache-size=1000
DNSEOF

systemctl stop dnsmasq
systemctl disable dnsmasq

# 5. Create systemd service
echo "Creating systemd service..."
cat > /etc/systemd/system/zrok-vpn-server.service <<SERVICEEOF
[Unit]
Description=zrok VPN Server (Full Tunnel)
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=root
Environment="ZROK_API_ENDPOINT=$ZROK_API"
Environment="HOME=/root"
ExecStart=/usr/local/bin/zrok share private $VPN_NAME
ExecStartPost=/bin/sleep 5
ExecStartPost=/bin/systemctl start dnsmasq
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
SERVICEEOF

systemctl daemon-reload

echo ""
echo "=== Setup Complete! ==="
echo ""
echo "Next steps:"
echo "1. Configure zrok: zrok config set apiEndpoint $ZROK_API"
echo "2. Enable zrok: zrok enable YOUR_TOKEN"
echo "3. Reserve VPN: sudo -E zrok reserve private --backend-mode vpn --unique-name $VPN_NAME"
echo "4. Start service: sudo systemctl start zrok-vpn-server"
echo "5. Check status: sudo systemctl status zrok-vpn-server"
echo ""
echo "Your public IP (clients will appear with this IP):"
curl -4 ifconfig.me
echo ""
```

Save and run:

```bash
chmod +x setup-vpn-server.sh
sudo ./setup-vpn-server.sh
```

---

## Client Configuration

### Overview

On the client side, we need to:
1. Connect to the zrok VPN
2. Change the default route to go through the VPN
3. Configure DNS to use VPN server

### Method 1: Manual Configuration (Linux)

#### Step 1: Connect to VPN

```bash
# Connect to VPN
sudo -E zrok access private full-tunnel-vpn

# Keep this running in background
# Press Ctrl+Z then type: bg
```

#### Step 2: Get VPN Gateway

```bash
# Find VPN gateway IP (should be 10.122.0.1)
ip route | grep tun0
# Output: 10.122.0.0/16 dev tun0 proto kernel scope link src 10.122.0.2

VPN_GATEWAY="10.122.0.1"
```

#### Step 3: Backup Current Routes

```bash
# Save current default route
ip route show | grep default > ~/default-route-backup.txt
cat ~/default-route-backup.txt

# Example output:
# default via 192.168.1.1 dev wlan0 proto dhcp metric 600
```

#### Step 4: Add Route to VPN Server (Prevent Routing Loop)

**CRITICAL STEP:** Before changing default route, ensure we can still reach the VPN server!

```bash
# Get VPN server's public IP from zrok controller
VPN_SERVER_IP="1.2.3.4"  # Replace with your VPS IP

# Get your current gateway
ORIGINAL_GATEWAY=$(ip route | grep default | awk '{print $3}')
echo "Original gateway: $ORIGINAL_GATEWAY"

# Add specific route to VPN server through original gateway
sudo ip route add $VPN_SERVER_IP via $ORIGINAL_GATEWAY
```

#### Step 5: Change Default Route

```bash
# Delete current default route
sudo ip route del default

# Add new default route through VPN
sudo ip route add default via $VPN_GATEWAY dev tun0

# Verify
ip route show
# Should show: default via 10.122.0.1 dev tun0
```

#### Step 6: Configure DNS

```bash
# Backup original DNS
sudo cp /etc/resolv.conf /etc/resolv.conf.backup

# Use VPN server's DNS
echo "nameserver 10.122.0.1" | sudo tee /etc/resolv.conf

# Or use public DNS
echo "nameserver 1.1.1.1" | sudo tee /etc/resolv.conf

# Prevent NetworkManager from overwriting
sudo chattr +i /etc/resolv.conf  # Make immutable
```

#### Step 7: Verify Full-Tunnel

```bash
# Check your public IP (should show VPN server's IP)
curl ifconfig.me

# Should output: 1.2.3.4 (your VPS IP, not your real IP!)

# Test DNS
nslookup google.com

# Test connectivity
ping -c 3 google.com
curl https://www.google.com
```

### Method 2: Automated Script (Linux)

```bash
#!/bin/bash
# connect-full-tunnel.sh

set -e

VPN_NAME="full-tunnel-vpn"
VPN_GATEWAY="10.122.0.1"
VPN_SERVER_IP="1.2.3.4"  # Replace with your VPS public IP!

echo "=== Connecting to Full-Tunnel VPN ==="

# Check if running as root
if [ "$EUID" -ne 0 ]; then
  echo "Please run as root (sudo)"
  exit 1
fi

# 1. Start VPN in background
echo "Starting VPN connection..."
zrok access private $VPN_NAME &
VPN_PID=$!
sleep 10  # Wait for VPN to establish

# 2. Check if VPN is up
if ! ip addr show tun0 &>/dev/null; then
  echo "ERROR: VPN interface not created"
  kill $VPN_PID
  exit 1
fi

echo "VPN interface created successfully"

# 3. Backup current route
echo "Backing up current routes..."
ip route show > /tmp/routes-backup-$(date +%s).txt

# 4. Get original gateway
ORIGINAL_GATEWAY=$(ip route | grep default | awk '{print $3}')
echo "Original gateway: $ORIGINAL_GATEWAY"

# 5. Add route to VPN server
echo "Adding route to VPN server..."
ip route add $VPN_SERVER_IP via $ORIGINAL_GATEWAY

# 6. Change default route
echo "Changing default route to VPN..."
ip route del default
ip route add default via $VPN_GATEWAY dev tun0

# 7. Configure DNS
echo "Configuring DNS..."
cp /etc/resolv.conf /etc/resolv.conf.backup
echo "nameserver $VPN_GATEWAY" > /etc/resolv.conf
chattr +i /etc/resolv.conf

# 8. Verify
echo ""
echo "=== VPN Connected! ==="
echo "Your IP address:"
curl -s ifconfig.me
echo ""
echo ""
echo "To disconnect, run: sudo ./disconnect-full-tunnel.sh"
echo "VPN PID: $VPN_PID (save this!)"
echo $VPN_PID > /tmp/zrok-vpn.pid
```

**Disconnect script:**

```bash
#!/bin/bash
# disconnect-full-tunnel.sh

set -e

echo "=== Disconnecting from VPN ==="

if [ "$EUID" -ne 0 ]; then
  echo "Please run as root (sudo)"
  exit 1
fi

# Kill VPN process
if [ -f /tmp/zrok-vpn.pid ]; then
  VPN_PID=$(cat /tmp/zrok-vpn.pid)
  echo "Killing VPN process (PID: $VPN_PID)..."
  kill $VPN_PID 2>/dev/null || true
  rm /tmp/zrok-vpn.pid
fi

# Kill any remaining zrok processes
pkill -f "zrok access" || true

# Restore DNS
echo "Restoring DNS..."
chattr -i /etc/resolv.conf 2>/dev/null || true
if [ -f /etc/resolv.conf.backup ]; then
  cp /etc/resolv.conf.backup /etc/resolv.conf
fi

# Restore routes (manual - NetworkManager will do this automatically on most systems)
echo "Routes will be restored by NetworkManager/systemd-networkd"
echo "Or restart networking: sudo systemctl restart NetworkManager"

# Verify
echo ""
echo "=== VPN Disconnected ==="
echo "Your IP address:"
curl -s ifconfig.me
echo ""
```

### Method 3: Persistent Client Configuration (systemd)

For always-on VPN connection:

```bash
sudo tee /etc/systemd/system/zrok-vpn-client.service <<'EOF'
[Unit]
Description=zrok VPN Client (Full Tunnel)
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=root
Environment="ZROK_API_ENDPOINT=https://api.zrok.example.com"
Environment="HOME=/root"
ExecStart=/usr/local/bin/zrok access private full-tunnel-vpn
ExecStartPost=/usr/local/bin/configure-vpn-routes.sh
ExecStopPost=/usr/local/bin/restore-routes.sh
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# Create route configuration script
sudo tee /usr/local/bin/configure-vpn-routes.sh <<'EOF'
#!/bin/bash
sleep 10  # Wait for VPN to establish

VPN_GATEWAY="10.122.0.1"
VPN_SERVER_IP="1.2.3.4"  # Your VPS IP

# Get original gateway
ORIGINAL_GATEWAY=$(ip route | grep default | awk '{print $3}' | head -1)

# Add route to VPN server
ip route add $VPN_SERVER_IP via $ORIGINAL_GATEWAY

# Change default route
ip route del default
ip route add default via $VPN_GATEWAY dev tun0

# Configure DNS
cp /etc/resolv.conf /etc/resolv.conf.backup
echo "nameserver $VPN_GATEWAY" > /etc/resolv.conf
chattr +i /etc/resolv.conf
EOF

sudo chmod +x /usr/local/bin/configure-vpn-routes.sh

# Create restoration script
sudo tee /usr/local/bin/restore-routes.sh <<'EOF'
#!/bin/bash
chattr -i /etc/resolv.conf 2>/dev/null || true
[ -f /etc/resolv.conf.backup ] && cp /etc/resolv.conf.backup /etc/resolv.conf
systemctl restart NetworkManager
EOF

sudo chmod +x /usr/local/bin/restore-routes.sh

# Enable and start
sudo systemctl daemon-reload
sudo systemctl enable zrok-vpn-client
sudo systemctl start zrok-vpn-client
```

### Method 4: macOS Configuration

```bash
#!/bin/bash
# connect-vpn-macos.sh

VPN_NAME="full-tunnel-vpn"
VPN_GATEWAY="10.122.0.1"
VPN_SERVER_IP="1.2.3.4"  # Your VPS IP

echo "=== Connecting to VPN (macOS) ==="

# Must run as root
if [ "$EUID" -ne 0 ]; then
  echo "Please run with sudo"
  exit 1
fi

# Start VPN
sudo -E zrok access private $VPN_NAME &
VPN_PID=$!
sleep 15

# Get interface name (utun0, utun1, etc.)
VPN_INTERFACE=$(ifconfig | grep -B 1 "inet 10.122.0" | head -1 | awk '{print $1}' | tr -d ':')
echo "VPN interface: $VPN_INTERFACE"

# Get original gateway
ORIGINAL_GATEWAY=$(netstat -rn -f inet | grep default | awk '{print $2}' | head -1)
echo "Original gateway: $ORIGINAL_GATEWAY"

# Add route to VPN server
route add $VPN_SERVER_IP $ORIGINAL_GATEWAY

# Change default route
route delete default
route add default $VPN_GATEWAY

# Configure DNS
networksetup -setdnsservers Wi-Fi $VPN_GATEWAY
# Or: networksetup -setdnsservers Ethernet $VPN_GATEWAY

echo "VPN Connected!"
echo "Your IP:"
curl -s ifconfig.me
echo ""
echo "VPN PID: $VPN_PID"
```

### Method 5: Windows Configuration

```powershell
# connect-vpn.ps1
# Run as Administrator

$VPN_NAME = "full-tunnel-vpn"
$VPN_GATEWAY = "10.122.0.1"
$VPN_SERVER_IP = "1.2.3.4"  # Your VPS IP

Write-Host "=== Connecting to VPN (Windows) ==="

# Start VPN
Start-Process -FilePath "zrok" -ArgumentList "access","private",$VPN_NAME -WindowStyle Hidden

# Wait for VPN to establish
Start-Sleep -Seconds 15

# Get VPN interface index
$VPNInterface = Get-NetAdapter | Where-Object {$_.InterfaceDescription -like "*Wintun*"}
$VPNIfIndex = $VPNInterface.ifIndex

Write-Host "VPN Interface Index: $VPNIfIndex"

# Get original default gateway
$OriginalRoute = Get-NetRoute -DestinationPrefix "0.0.0.0/0" | Select-Object -First 1
$OriginalGateway = $OriginalRoute.NextHop
$OriginalIfIndex = $OriginalRoute.ifIndex

Write-Host "Original Gateway: $OriginalGateway"

# Add route to VPN server through original gateway
New-NetRoute -DestinationPrefix "$VPN_SERVER_IP/32" -NextHop $OriginalGateway -InterfaceIndex $OriginalIfIndex

# Change default route
Remove-NetRoute -DestinationPrefix "0.0.0.0/0" -Confirm:$false
New-NetRoute -DestinationPrefix "0.0.0.0/0" -NextHop $VPN_GATEWAY -InterfaceIndex $VPNIfIndex

# Configure DNS
Set-DnsClientServerAddress -InterfaceIndex $VPNIfIndex -ServerAddresses $VPN_GATEWAY

Write-Host "VPN Connected!"
Write-Host "Your IP:"
(Invoke-WebRequest -Uri "https://ifconfig.me").Content
```

---

## DNS Configuration

### Option 1: Use VPN Server's DNS (Recommended)

Already configured in server setup (dnsmasq).

**Client configuration:**
```bash
echo "nameserver 10.122.0.1" | sudo tee /etc/resolv.conf
```

### Option 2: Use Public DNS Servers

**Privacy-focused DNS:**
```bash
# Cloudflare DNS (fast, privacy-focused)
echo "nameserver 1.1.1.1" | sudo tee /etc/resolv.conf
echo "nameserver 1.0.0.1" | sudo tee -a /etc/resolv.conf

# Quad9 (security & privacy)
echo "nameserver 9.9.9.9" | sudo tee /etc/resolv.conf
echo "nameserver 149.112.112.112" | sudo tee -a /etc/resolv.conf

# Mullvad DNS (max privacy)
echo "nameserver 194.242.2.2" | sudo tee /etc/resolv.conf
```

### Option 3: Ad-Blocking DNS

Configure server with Pi-hole or AdGuard:

**Install Pi-hole on VPN server:**
```bash
curl -sSL https://install.pi-hole.net | bash

# During installation:
# - Select tun0 as interface
# - Set upstream DNS to your preference

# After installation, clients get ad-blocking automatically!
```

### Option 4: DNS-over-HTTPS (DoH)

For maximum privacy, use DoH on client:

```bash
# Install cloudflared
wget https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64.deb
sudo dpkg -i cloudflared-linux-amd64.deb

# Configure DoH
sudo cloudflared proxy-dns --port 53 --upstream https://1.1.1.1/dns-query &

# Use localhost as DNS
echo "nameserver 127.0.0.1" | sudo tee /etc/resolv.conf
```

---

## Verification & Testing

### Test 1: Check Your Public IP

```bash
# Should show VPN server's IP, not your real IP
curl ifconfig.me
curl ipinfo.io/ip
curl icanhazip.com

# More detailed info
curl ipinfo.io
# Should show VPN server's location
```

### Test 2: DNS Leak Test

```bash
# Check which DNS servers are being used
curl https://www.dnsleaktest.com/

# Or use command line
dig +short myip.opendns.com @resolver1.opendns.com
# Should show VPN server IP
```

### Test 3: WebRTC Leak Test

Visit: https://browserleaks.com/webrtc

Your local IP should NOT be visible.

**Fix WebRTC leaks in browser:**

**Firefox:**
```
about:config
media.peerconnection.enabled = false
```

**Chrome:**
Install extension: "WebRTC Leak Prevent"

### Test 4: Torrent IP Test

1. Visit: https://ipleak.net/
2. Use their torrent test
3. Should show only VPN server IP

### Test 5: Comprehensive Leak Test

```bash
# Check all routing
ip route show

# Should show:
# default via 10.122.0.1 dev tun0
# 10.122.0.0/16 dev tun0 proto kernel scope link src 10.122.0.2
# 1.2.3.4 via 192.168.1.1 dev wlan0  ← VPN server specific route
```

### Test 6: Kill Switch Test

```bash
# Kill VPN connection
sudo pkill -f "zrok access"

# Try to access internet
curl ifconfig.me

# Should FAIL or show your real IP (means kill switch needed - see Advanced section)
```

### Test 7: Speed Test

```bash
# Without VPN
curl ifconfig.me  # Note your real IP
speedtest-cli

# With VPN
curl ifconfig.me  # Should be VPN IP
speedtest-cli

# Compare speeds
```

### Test 8: Geo-Location Test

```bash
# Check your apparent location
curl ipinfo.io

# Should show VPN server's location:
{
  "ip": "1.2.3.4",
  "city": "Amsterdam",  ← VPN server location
  "region": "North Holland",
  "country": "NL",
  ...
}
```

---

## Advanced Features

### Feature 1: Kill Switch (Prevent Leaks)

Blocks all traffic if VPN disconnects.

```bash
# Create kill switch script
sudo tee /usr/local/bin/vpn-kill-switch.sh <<'EOF'
#!/bin/bash

VPN_INTERFACE="tun0"
VPN_SUBNET="10.122.0.0/16"

# Flush all existing rules
iptables -F
iptables -X

# Default policy: DROP everything
iptables -P INPUT DROP
iptables -P FORWARD DROP
iptables -P OUTPUT DROP

# Allow loopback
iptables -A INPUT -i lo -j ACCEPT
iptables -A OUTPUT -o lo -j ACCEPT

# Allow VPN traffic
iptables -A INPUT -i $VPN_INTERFACE -j ACCEPT
iptables -A OUTPUT -o $VPN_INTERFACE -j ACCEPT

# Allow established connections
iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT
iptables -A OUTPUT -m state --state ESTABLISHED,RELATED -j ACCEPT

# Allow DNS only through VPN
iptables -A OUTPUT -o $VPN_INTERFACE -p udp --dport 53 -j ACCEPT

# Allow local network (optional)
# iptables -A INPUT -s 192.168.1.0/24 -j ACCEPT
# iptables -A OUTPUT -d 192.168.1.0/24 -j ACCEPT

echo "Kill switch activated!"
EOF

sudo chmod +x /usr/local/bin/vpn-kill-switch.sh
```

**Activate kill switch:**
```bash
sudo /usr/local/bin/vpn-kill-switch.sh
```

**Disable kill switch:**
```bash
sudo iptables -P INPUT ACCEPT
sudo iptables -P FORWARD ACCEPT
sudo iptables -P OUTPUT ACCEPT
sudo iptables -F
```

### Feature 2: Split Tunneling (Exclude Specific Traffic)

Route some traffic directly, rest through VPN.

**Example: Exclude local network from VPN:**

```bash
# After connecting to VPN, add exceptions
sudo ip route add 192.168.1.0/24 via 192.168.1.1 dev wlan0

# Now 192.168.1.x traffic goes direct, everything else through VPN
```

**Example: Only route specific sites through VPN:**

```bash
# Default route: direct
# Specific site: through VPN

# Add route for specific IP through VPN
sudo ip route add 93.184.216.34 via 10.122.0.1 dev tun0

# Now only traffic to that IP goes through VPN
```

### Feature 3: IPv6 Support

If your VPS has IPv6:

```bash
# Server: Enable IPv6 forwarding
sudo sysctl -w net.ipv6.conf.all.forwarding=1

# Server: Add IPv6 NAT rule
sudo ip6tables -t nat -A POSTROUTING -s fd00::/8 -o eth0 -j MASQUERADE

# Use IPv6 subnet for VPN
sudo -E zrok share private --backend-mode vpn fd00:1122::/64
```

### Feature 4: Multi-Hop VPN

Chain multiple VPN servers for extra privacy.

```
You → VPN Server 1 → VPN Server 2 → Internet
```

**Setup:**

**Server 1:**
```bash
# Normal full-tunnel setup
# Clients connect to Server 1
```

**Server 2:**
```bash
# Install zrok client on Server 2
# Connect Server 2 to Server 1's VPN
sudo -E zrok access private server1-vpn

# Configure Server 2 as exit node
sudo sysctl -w net.ipv4.ip_forward=1
sudo iptables -t nat -A POSTROUTING -s 10.123.0.0/16 -o eth0 -j MASQUERADE

# Start Server 2's VPN for clients
sudo -E zrok share private --backend-mode vpn 10.123.0.0/16
```

**Your Client:**
```bash
# Connect to Server 2
sudo -E zrok access private server2-vpn

# Configure full-tunnel

# Now: You → Server 2 → Server 1 → Internet
```

### Feature 5: Port Forwarding Through VPN

Forward ports from VPN server to your client.

**Example: Host web server on your laptop, accessible via VPN server IP:**

**On VPN Server:**
```bash
# Forward port 8080 on server to client's 10.122.0.2:80
sudo iptables -t nat -A PREROUTING -i eth0 -p tcp --dport 8080 -j DNAT --to-destination 10.122.0.2:80

# Allow forwarding
sudo iptables -A FORWARD -p tcp -d 10.122.0.2 --dport 80 -j ACCEPT

# Save rules
sudo netfilter-persistent save
```

Now `http://VPS-IP:8080` → Your laptop's web server!

### Feature 6: Bandwidth Monitoring

Track VPN usage:

```bash
# Install vnstat
sudo apt install vnstat

# Monitor tun0 interface
vnstat -i tun0 -l  # Live traffic
vnstat -i tun0 -d  # Daily stats
vnstat -i tun0 -m  # Monthly stats
```

### Feature 7: Load Balancing (Multiple Exit Servers)

Use multiple VPN servers and balance traffic:

```bash
# Setup multiple VPN connections
sudo -E zrok access private vpn-server-us &
sudo -E zrok access private vpn-server-eu &

# Use policy routing to balance
# (Advanced - requires careful routing table manipulation)
```

---

## Multi-Location Setup

Deploy VPN servers in multiple locations for geo-diversity.

### Architecture

```
                        ┌─────────────────┐
                        │   Your Client   │
                        └────────┬────────┘
                                 │
                   ┏─────────────┼─────────────┓
                   ▼             ▼             ▼
          ┌────────────┐ ┌────────────┐ ┌────────────┐
          │  VPN USA   │ │  VPN EU    │ │ VPN Asia   │
          │  (Server1) │ │ (Server2)  │ │ (Server3)  │
          └────────────┘ └────────────┘ └────────────┘
               ▼               ▼               ▼
          US Internet    EU Internet    Asia Internet
```

### Setup Process

**Deploy VPS in multiple regions:**

1. **USA Server:**
   - Provider: DigitalOcean NYC
   - IP: 1.2.3.4
   - Name: vpn-usa

2. **EU Server:**
   - Provider: Hetzner Frankfurt
   - IP: 5.6.7.8
   - Name: vpn-eu

3. **Asia Server:**
   - Provider: Vultr Tokyo
   - IP: 9.10.11.12
   - Name: vpn-asia

**Configure each server:**
```bash
# On each VPS, run the setup script
./setup-vpn-server.sh

# Reserve with unique names
sudo -E zrok reserve private --backend-mode vpn --unique-name vpn-usa
sudo -E zrok reserve private --backend-mode vpn --unique-name vpn-eu
sudo -E zrok reserve private --backend-mode vpn --unique-name vpn-asia
```

**Client-side script to switch servers:**

```bash
#!/bin/bash
# switch-vpn-location.sh

case "$1" in
  usa)
    VPN_NAME="vpn-usa"
    VPN_SERVER_IP="1.2.3.4"
    ;;
  eu)
    VPN_NAME="vpn-eu"
    VPN_SERVER_IP="5.6.7.8"
    ;;
  asia)
    VPN_NAME="vpn-asia"
    VPN_SERVER_IP="9.10.11.12"
    ;;
  *)
    echo "Usage: $0 {usa|eu|asia}"
    exit 1
    ;;
esac

echo "Connecting to $VPN_NAME..."

# Disconnect current VPN if any
sudo ./disconnect-full-tunnel.sh

# Connect to new location
export VPN_SERVER_IP
./connect-full-tunnel.sh $VPN_NAME

echo "Connected to $VPN_NAME!"
curl ifconfig.me
```

**Usage:**
```bash
# Connect to US server
./switch-vpn-location.sh usa

# Switch to EU
./switch-vpn-location.sh eu

# Switch to Asia
./switch-vpn-location.sh asia
```

---

## Performance Optimization

### 1. Choose Server Location Wisely

```bash
# Test latency to different providers
ping -c 10 digitalocean-nyc.example.com
ping -c 10 hetzner-fsn.example.com
ping -c 10 vultr-tokyo.example.com

# Choose lowest latency
```

### 2. MTU Optimization

```bash
# Test optimal MTU
ping -M do -s 1472 10.122.0.1  # Start with 1472
# If works, try 1482, 1492, etc.
# If fails, try 1462, 1452, etc.

# Set optimal MTU
sudo ip link set dev tun0 mtu 1400  # Adjust based on test
```

### 3. TCP Optimization (Server)

```bash
# On VPN server
sudo tee -a /etc/sysctl.conf <<EOF
# TCP optimization for VPN
net.core.rmem_max = 67108864
net.core.wmem_max = 67108864
net.ipv4.tcp_rmem = 4096 87380 33554432
net.ipv4.tcp_wmem = 4096 65536 33554432
net.core.netdev_max_backlog = 5000
net.ipv4.tcp_congestion_control = bbr
net.ipv4.tcp_window_scaling = 1
net.ipv4.tcp_timestamps = 1
net.ipv4.tcp_sack = 1
net.ipv4.tcp_no_metrics_save = 1
EOF

sudo sysctl -p
```

### 4. Use Faster DNS

```bash
# On client, use fast DNS even through VPN
echo "nameserver 1.1.1.1" | sudo tee /etc/resolv.conf
```

### 5. Compression (Optional)

Not natively supported by zrok, but you can use:

```bash
# Install and configure squid proxy on VPN server
sudo apt install squid
# Configure compression in /etc/squid/squid.conf
```

### 6. Upgrade VPS

```bash
# Choose VPS with:
# - High CPU single-thread performance
# - 10 Gbps network
# - NVMe storage
# - Latest CPU architecture
```

### 7. Reduce Encryption Overhead

OpenZiti already uses efficient encryption, but ensure:
```bash
# Server has AES-NI CPU instructions
grep aes /proc/cpuinfo

# If available, encryption is hardware-accelerated
```

---

## Security Hardening

### 1. Server Firewall (UFW)

```bash
# On VPN server
sudo ufw default deny incoming
sudo ufw default allow outgoing

# Allow SSH (change port if using non-standard)
sudo ufw allow 22/tcp

# Allow OpenZiti ports
sudo ufw allow 1280/tcp
sudo ufw allow 3022/tcp

# Enable firewall
sudo ufw enable
```

### 2. Fail2Ban (Prevent Brute Force)

```bash
sudo apt install fail2ban

# Configure for SSH
sudo tee /etc/fail2ban/jail.local <<EOF
[sshd]
enabled = true
port = 22
filter = sshd
logpath = /var/log/auth.log
maxretry = 3
bantime = 3600
EOF

sudo systemctl restart fail2ban
```

### 3. Automatic Updates

```bash
sudo apt install unattended-upgrades
sudo dpkg-reconfigure -plow unattended-upgrades
```

### 4. Disable IPv6 (If Not Used)

```bash
sudo sysctl -w net.ipv6.conf.all.disable_ipv6=1
sudo sysctl -w net.ipv6.conf.default.disable_ipv6=1
```

### 5. Logging and Monitoring

```bash
# Monitor VPN connections
sudo journalctl -u zrok-vpn-server -f

# Monitor bandwidth
vnstat -i tun0 -l

# Alert on high CPU
# Install monitoring tool like netdata
```

### 6. Regular Backups

```bash
# Backup zrok configuration
cp -r ~/.zrok ~/backups/

# Backup iptables rules
sudo iptables-save > ~/backups/iptables-rules-$(date +%Y%m%d)

# Backup server config
tar czf ~/backups/vpn-config-$(date +%Y%m%d).tar.gz /etc/zrok /etc/dnsmasq.conf
```

### 7. Use SSH Keys (Not Passwords)

```bash
# Generate key on your machine
ssh-keygen -t ed25519

# Copy to server
ssh-copy-id root@vpn-server

# Disable password auth
sudo sed -i 's/PasswordAuthentication yes/PasswordAuthentication no/' /etc/ssh/sshd_config
sudo systemctl restart sshd
```

---

## Troubleshooting

### Issue 1: Can't Access Internet After Connecting

**Symptoms:**
```bash
curl ifconfig.me
# Hangs or times out
```

**Diagnosis:**
```bash
# Check if default route is correct
ip route show
# Should show: default via 10.122.0.1 dev tun0

# Check if you can ping VPN server
ping 10.122.0.1
```

**Solution:**
```bash
# Check NAT rules on server
sudo iptables -t nat -L -n -v

# Ensure MASQUERADE rule exists
# If missing, add:
sudo iptables -t nat -A POSTROUTING -s 10.122.0.0/16 -o eth0 -j MASQUERADE
sudo netfilter-persistent save
```

### Issue 2: DNS Not Working

**Symptoms:**
```bash
ping 8.8.8.8  # Works
ping google.com  # Fails: Name resolution failed
```

**Solution:**
```bash
# Check DNS configuration
cat /etc/resolv.conf

# Should show: nameserver 10.122.0.1 (or public DNS)

# Test DNS manually
nslookup google.com 10.122.0.1

# If server DNS not working, use public DNS
echo "nameserver 1.1.1.1" | sudo tee /etc/resolv.conf
sudo chattr +i /etc/resolv.conf
```

### Issue 3: Slow Performance

**Diagnosis:**
```bash
# Check latency
ping -c 10 10.122.0.1

# Check bandwidth
speedtest-cli

# Monitor server CPU
ssh vpn-server "top"
```

**Solutions:**
- Choose VPS closer to your location
- Upgrade VPS (more CPU, bandwidth)
- Optimize MTU (see Performance section)
- Check if server bandwidth is throttled

### Issue 4: Can't Reconnect After Disconnect

**Symptoms:**
```bash
sudo -E zrok access private full-tunnel-vpn
# Error: already in use
```

**Solution:**
```bash
# Kill existing zrok processes
sudo pkill -f "zrok access"

# Remove any lingering tun interface
sudo ip link delete tun0

# Reconnect
sudo -E zrok access private full-tunnel-vpn
```

### Issue 5: Traffic Still Leaking (Real IP Visible)

**Diagnosis:**
```bash
# Check routes
ip route show

# Check for default route outside VPN
# Bad: default via 192.168.1.1 dev wlan0  ← This should not exist!
```

**Solution:**
```bash
# Delete non-VPN default routes
sudo ip route del default via 192.168.1.1

# Ensure only VPN default route exists
sudo ip route add default via 10.122.0.1 dev tun0

# Implement kill switch (see Advanced section)
```

### Issue 6: WebRTC Leaking Local IP

**Solution:**
- Disable WebRTC in browser (see Verification section)
- Or use browser extensions

### Issue 7: Server Running Out of Resources

**Diagnosis:**
```bash
# On server
free -h  # Check memory
df -h    # Check disk
top      # Check CPU
```

**Solution:**
- Upgrade VPS
- Limit number of concurrent clients
- Optimize server (see Performance section)

---

## Comparison with Commercial VPNs

### Feature Comparison

| Feature | zrok Full-Tunnel VPN | ExpressVPN/NordVPN/Mullvad |
|---------|---------------------|---------------------------|
| **Cost** | Server cost only ($3-6/mo) | $5-12/month subscription |
| **Privacy** | You control everything | Trust provider's policy |
| **Logs** | You choose (none by default) | "No-log" claims (trust-based) |
| **Speed** | Depends on your VPS | Usually fast (dedicated infrastructure) |
| **Locations** | Deploy where you want | Pre-defined locations |
| **Setup** | Technical setup required | Click to connect |
| **Mobile** | Limited (requires work) | Native apps available |
| **Simultaneous Devices** | Unlimited (your server) | Usually 5-10 devices |
| **Kill Switch** | DIY (see guide) | Built-in |
| **Split Tunneling** | Manual configuration | Built-in |
| **Customer Support** | Self-support | 24/7 support |
| **Reliability** | Depends on VPS provider | Usually very reliable |
| **Bandwidth** | VPS limits | Usually unlimited |
| **Censorship Bypass** | Depends on OpenZiti detection | Specialized obfuscation |

### Advantages of zrok VPN

✅ **Complete Control:**
- No third-party logging
- You choose server location
- You control DNS
- You set the rules

✅ **Cost-Effective:**
- $3-6/month for single VPS
- Unlimited devices
- No subscription fees

✅ **Learning Experience:**
- Understand how VPNs work
- Full transparency
- Customizable

✅ **No Trust Required:**
- Open source (zrok + OpenZiti)
- You can audit everything
- No mysterious "no-log" policies

### Disadvantages vs Commercial VPNs

❌ **Technical Complexity:**
- Requires Linux knowledge
- Manual setup and maintenance
- Troubleshooting on your own

❌ **Limited Locations:**
- One location per VPS
- Must deploy and manage multiple servers
- No instant switching

❌ **No Mobile Apps:**
- Requires manual configuration
- Limited iOS support
- Not user-friendly

❌ **Maintenance:**
- You handle updates
- You fix issues
- You monitor uptime

❌ **Single Point of Failure:**
- If your VPS goes down, no backup
- No automatic failover
- You handle uptime

### When to Use zrok Full-Tunnel VPN

**Best for:**
- Privacy enthusiasts who want full control
- Developers who want to learn
- Users who distrust commercial VPNs
- Budget-conscious users
- Users who need specific server locations
- Those who want unlimited devices

**Not ideal for:**
- Non-technical users
- Mobile-primary users
- Those needing 99.99% uptime
- Users wanting zero maintenance
- Those needing multiple locations instantly

### When to Use Commercial VPN

**Best for:**
- Non-technical users
- Mobile users
- Users needing instant location switching
- Those wanting zero maintenance
- Bypassing sophisticated censorship
- 24/7 support requirements

---

## Conclusion

You've now transformed zrok VPN into a full-featured VPN provider! 🎉

### What You've Achieved

✅ **Full-tunnel VPN** routing all traffic through your server
✅ **Privacy protection** hiding your real IP address
✅ **DNS privacy** with custom DNS configuration
✅ **Complete control** over your VPN infrastructure
✅ **Zero trust** of third parties
✅ **Cost-effective** solution ($3-6/month)

### Next Steps

1. **Test thoroughly** - Verify no leaks
2. **Deploy multiple locations** - Global presence
3. **Implement kill switch** - Prevent leaks
4. **Optimize performance** - Fast speeds
5. **Monitor regularly** - Ensure uptime
6. **Share with friends/family** - Multiple clients welcome!

### Resources

- **zrok Documentation:** https://docs.zrok.io
- **OpenZiti Docs:** https://openziti.io
- **This Guide:** Keep for reference!

### Support

- **Questions:** Open issue on GitHub
- **Community:** Join OpenZiti Discourse
- **Updates:** Watch zrok releases

---

**Happy Private Browsing!** 🔒🌍

*Remember: With great power comes great responsibility. Use your VPN ethically and legally.*

---

## Appendix: Quick Reference Commands

### Server Commands
```bash
# Start VPN server
sudo -E zrok share private full-tunnel-vpn

# Check server status
sudo systemctl status zrok-vpn-server

# View server IP
curl ifconfig.me

# Monitor connections
sudo tcpdump -i tun0

# Check NAT rules
sudo iptables -t nat -L -n -v
```

### Client Commands
```bash
# Connect to VPN
sudo -E zrok access private full-tunnel-vpn

# Check your IP (should be VPN server's)
curl ifconfig.me

# Check routes
ip route show

# Check DNS
cat /etc/resolv.conf

# Disconnect
sudo pkill -f "zrok access"
```

### Troubleshooting Commands
```bash
# Server side
sudo journalctl -u zrok-vpn-server -f
sudo iptables -t nat -L -n -v
sudo systemctl status dnsmasq
ip addr show tun0

# Client side
ip route show
cat /etc/resolv.conf
ping 10.122.0.1
curl ifconfig.me
nslookup google.com
```

---

**End of Guide**
