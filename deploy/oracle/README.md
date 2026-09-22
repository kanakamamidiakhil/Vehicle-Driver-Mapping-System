# Free deployment on Oracle Cloud (Always Free VM)

Oracle Cloud's Always Free tier includes an Ampere A1 VM with 4 CPUs and 24 GB RAM, free with no time
limit. That is enough to run the whole app on one server, including the open-source LLM
(Qwen 2.5 3B via Ollama), with no external AI API.

What runs on the VM (Docker Compose, `deploy/oracle/docker-compose.yml`):

| Container | Purpose | Public? |
|-----------|---------|---------|
| `frontend` | nginx serving the Angular UI and proxying `/api` | Yes, port 80 |
| `backend` | Spring Boot API | No |
| `postgres` | PostgreSQL 16 (data kept in a Docker volume) | No |
| `ollama` | Local LLM server | No |

---

## Step 1: Create an Oracle Cloud account (about 10 minutes)

1. Go to https://signup.cloud.oracle.com and sign up.
2. Choose a **Home Region** close to you, for example *India West (Mumbai)* or *India South (Hyderabad)*.
   You cannot change it later.
3. Oracle asks for a credit or debit card **only to verify your identity**. Always Free resources are
   not charged. Don't click "Upgrade to Pay As You Go" if you want to stay free.

## Step 2: Create the free VM

1. In the Oracle Cloud console, open the menu **☰ → Compute → Instances → Create instance**.
2. **Name:** `vehicle-mapping`.
3. **Image and shape:** click **Edit**.
   - **Change image** → **Ubuntu** → select **Canonical Ubuntu 24.04** (not the "Minimal" one).
   - **Change shape** → **Ampere** → **VM.Standard.A1.Flex**. Set **4 OCPUs** and **24 GB memory**.
     The "Always Free-eligible" label should show.
4. **Networking:** leave the defaults (create a new virtual cloud network and a **public subnet**).
   Make sure **Assign a public IPv4 address** is ticked.
5. **Add SSH keys:** choose **Generate a key pair for me**, then click **Save private key**. Keep this
   file safe, because it's your only way to log in.
6. Click **Create**. After about a minute the status turns **Running**. Copy the **Public IP address**
   from the instance page.

> **"Out of capacity" error?** Free Ampere VMs are popular. Try another *Availability domain* (in the
> Placement section), try again later, or use a smaller shape like 2 OCPU / 12 GB (still works, but AI
> answers are slower).

## Step 3: Open port 80 in Oracle's network settings

1. On the instance page, click the **subnet** link (under *Primary VNIC*).
2. Open **Security Lists → Default Security List for …**.
3. Click **Add Ingress Rules** and fill in:
   - **Source CIDR:** `0.0.0.0/0`
   - **IP Protocol:** TCP
   - **Destination Port Range:** `80`
4. Click **Add Ingress Rules**.

## Step 4: Connect to the VM

On **Windows** (PowerShell), **macOS** or **Linux**, in the folder where you saved the key:

```sh
ssh -i ssh-key-XXXX.key ubuntu@YOUR_PUBLIC_IP
```

- macOS/Linux: if it says the key permissions are too open, run `chmod 600 ssh-key-XXXX.key` first.
- Windows: if it complains about permissions, run this once in PowerShell and try again:
  `icacls .\ssh-key-XXXX.key /inheritance:r /grant:r "$($env:USERNAME):(R)"`
- Type `yes` when asked whether to trust the host.

## Step 5: Install and start the app (one command)

In the SSH session, run:

```sh
curl -fsSL https://raw.githubusercontent.com/kanakamamidiakhil/Vehicle-Driver-Mapping-System/main/deploy/oracle/setup.sh | bash
```

The script:
1. opens port 80 in the VM's own firewall,
2. installs Docker,
3. downloads the code from GitHub,
4. creates a random database password (saved only on the VM, in `deploy/oracle/.env`),
5. builds and starts all containers, and downloads the AI model (about 2 GB).

The first run takes about **15–25 minutes**, mostly building and downloading. When it finishes it
prints:

```
==> Done! Open http://YOUR_PUBLIC_IP in your browser.
```

Open that address. Try **✨ Ask AI → "Who is driving the Alto TS 09 AB 1234?"**. Demo driver logins:
`ravi@fleet.com` / `driver123`.

---

## Updating after code changes

Run the same command again on the VM. It pulls the latest `main` and rebuilds:

```sh
curl -fsSL https://raw.githubusercontent.com/kanakamamidiakhil/Vehicle-Driver-Mapping-System/main/deploy/oracle/setup.sh | bash
```

Your data is kept, because the database lives in a Docker volume.

## Useful commands (on the VM)

```sh
cd ~/Vehicle-Driver-Mapping-System/deploy/oracle
sudo docker compose ps                        # what is running
sudo docker compose logs --tail=100 backend   # backend logs
sudo docker compose restart backend           # restart one service
sudo docker compose down                      # stop everything (data is kept)
```

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| Browser can't reach `http://IP` | Check the ingress rule for port 80 (step 3) and that you used `http://`, not `https://`. |
| Page loads but shows "Cannot reach the server" | The backend is still starting. Wait a minute, or check `docker compose logs backend`. |
| AI page shows "model offline" | The model is still downloading. Check `sudo docker compose logs ollama-pull`. |
| AI answers are slow | Normal on CPU (roughly 15–40 s). Keep the shape at 4 OCPU / 24 GB. |

## Notes

- **HTTPS:** the app is served over plain HTTP on the VM's IP. For a domain name with HTTPS, point a
  domain at the IP and put a reverse proxy such as Caddy in front. Ask if you want this set up.
- **Security:** as in the original project, the admin pages have no login. Anyone who knows the IP can
  use them. Add authentication before sharing the address widely.
- **Idle reclaim:** Oracle may reclaim Always Free VMs that stay almost completely idle for 7 days.
  Normal use of the app prevents this.
