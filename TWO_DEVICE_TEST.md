# Testing app-to-app payments with two devices / two IDs

Both app instances must use the **same database**. Each user signs in with their own account
(Create New Account on the login screen).

## Option A - two phones/PCs, shared MySQL (real setup)
1. Install MySQL Server on one machine (the "server"). Note its IP, e.g. `192.168.1.20`.
2. Allow remote login for your user (MySQL: `CREATE USER 'smart'@'%' IDENTIFIED BY 'pass'; GRANT ALL ON smartsave.* TO 'smart'@'%';`)
   and open port 3306 in the firewall.
3. On **each** device run the app with:
   ```powershell
   $env:SMARTSAVE_DB_URL="jdbc:mysql://192.168.1.20:3306/smartsave?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
   $env:SMARTSAVE_DB_USER="smart"
   $env:SMARTSAVE_DB_PASSWORD="pass"
   .\gradlew.bat run
   ```
4. Device 1: create account `riya_k`. Device 2: create account `amit_s`.
5. Connect page shows "Online: shared MySQL database". Search the other ID, then Send Money or Request Money.
   Requests appear on the other device within ~4 seconds (or press the refresh button) with Pay / Decline.

## Option B - two windows on one PC (no MySQL needed)
Open two PowerShell windows in the project folder and run in both:
```powershell
$env:SMARTSAVE_DB_URL="jdbc:sqlite:C:/Users/ASUS/smartsave_shared.db"
.\gradlew.bat run
```
(The second `run` may need `--no-daemon`/a different Gradle invocation if the first is still holding the build lock.)
Create two accounts, one per window, and pay between them.
