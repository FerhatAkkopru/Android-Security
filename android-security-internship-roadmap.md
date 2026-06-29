# Android App Security — Intern Roadmap

**Duration:** ~8 weeks (flexible)
**Target:** 3rd-year CS student who can build a basic Compose/Kotlin app and call a REST API
**Framework reference:** OWASP **MASVS** (verification standard) + **MASTG** (testing guide). Make these the intern's "textbook" — almost every topic below maps to a MASVS requirement.

---

## Core idea: one app, three states

Instead of scattered exercises, the intern works on **a single demo app** and moves it through three states. This makes the learning visible and gives you a clean final demo.

> **App concept:** a small mock-banking or secure-notes app — login screen, a token-authenticated API call, and some sensitive local data (balance, notes, a "PIN"). Simple domain, but it touches every security surface.

1. **`v1-vulnerable`** — deliberately insecure. Plaintext storage, no pinning, hardcoded secrets, cleartext HTTP allowed, logs everything.
2. **`v2-hardened`** — each weakness fixed, one topic at a time, in its own commit/PR so the diff *is* the lesson.
3. **`attack`** — the intern attacks **his own** `v1` vs `v2` with Burp, jadx, Frida, and MobSF, and documents what breaks and what holds.

Keep `v1` and `v2` as git branches/flavors so they can be compared side by side in the final presentation.

---

## Phase 0 — Foundations & environment (Week 1)

**Learn**
- Android security model: app sandbox, per-app Linux UID, permission model, why `targetSdk` matters.
- The threat model: who attacks a mobile app and how (MITM, device theft, rooted device, repackaged APK, malicious app on same device).
- OWASP MASVS categories + how MASTG test cases are structured.

**Set up the toolbox**
- **Burp Suite** (Community is fine) — intercepting proxy.
- **jadx / jadx-gui** — decompile APK → readable Java/Kotlin.
- **apktool** — unpack/repack resources and smali.
- **MobSF** — automated static + dynamic analysis (great for a first scan).
- **Frida + objection** — runtime instrumentation (hook methods, bypass pinning).
- **adb** — the swiss-army knife.
- A **rooted emulator** (AVD with a non-Google-Play image) or rooted test device + **Magisk**.

**Deliverable:** a one-page threat model for the demo app + working tool setup (screenshot of Burp intercepting a request).

---

## Phase 1 — Authentication & login security (Weeks 2–3)

**Learn**
- Why login is server-authoritative; the client *never* decides "am I logged in."
- Token-based auth (access + refresh), short-lived access tokens, secure refresh flow.
- **Where to store tokens:** Android Keystore–backed encryption, *not* plain `SharedPreferences`.
- Biometric auth done right (`BiometricPrompt` + Keystore `setUserAuthenticationRequired`), and the classic mistake of using biometrics as pure UI with no crypto binding.
- Brute-force / rate-limiting (server side), account lockout, no user-enumeration in error messages.
- Logout = invalidate token server-side + wipe local secrets.

**Implement (v1 → v2)**
- v1: stores the auth token in plaintext `SharedPreferences`, "remember me" keeps the password.
- v2: token in Keystore-encrypted storage (`EncryptedSharedPreferences`/encrypted DataStore or Keystore directly), biometric gate bound to a Keystore key, no password ever persisted.

**Attack / demo**
- Pull the token off a rooted device from v1's `shared_prefs` XML; show it's plaintext.
- Show v2's file is unreadable ciphertext.

**Deliverable:** auth section of the report + before/after of the on-disk token.

---

## Phase 2 — Network call security (Weeks 3–4)

**Learn**
- TLS everywhere; ban cleartext via **Network Security Config** (`cleartextTrafficPermitted=false`).
- What a MITM proxy actually does and why a user-installed CA can read traffic by default on older targetSdks.
- Sensitive data never in URLs/query strings; correct use of headers; no secrets in logs.
- Why you can't trust the client: server-side validation/authorization regardless of what the app sends.

**Implement (v1 → v2)**
- v1: allows HTTP, logs full request/response bodies (OkHttp logging at BODY level in release), trusts user CAs.
- v2: Network Security Config locking down cleartext + restricting trust anchors, stripped logging in release builds.

**Attack / demo**
- Point the app at Burp. Show v1 traffic fully readable/modifiable.
- Modify a response in Burp (e.g., change account balance) to demonstrate why the client must not trust server responses blindly and why integrity matters.

**Deliverable:** annotated Burp captures, the Network Security Config file explained.

---

## Phase 3 — SSL / certificate pinning + expiry strategy (Weeks 4–5)

This is the part you specifically called out, so give it real weight.

**Learn**
- What pinning buys you: defeats MITM even when a malicious/extra CA is trusted.
- Pinning methods: **OkHttp `CertificatePinner`** (pin the SPKI hash), Network Security Config `<pin-set>`, and (briefly) `TrustManager` pinning. Pin the **public key (SPKI)**, not the whole leaf certificate.
- **Certificate Transparency** as a complementary defense.

**Expired-certificate / rotation strategy (the trap most people hit)**
- Pinning to a single leaf cert = the app *bricks* the moment the cert is renewed. Make sure the intern understands and solves this:
  - Pin to the **public key**, and reuse the same key pair across renewals so the pin survives rotation; **or**
  - Pin the **intermediate/CA** rather than the leaf; **and always**
  - Include a **backup pin** (the next key already provisioned).
  - Set an **expiration on the pin set** (`<pin-set expiration="...">`) so the app fails *open* to normal TLS instead of hard-failing if pins go stale.
  - Have a **remote-config / forced-update kill switch** so a bad pin can be pushed past without an emergency store release.
- Discuss the operational playbook: how a backend team rotates certs without a coordinated app release.

**Implement (v1 → v2)**
- v2: OkHttp `CertificatePinner` with a primary + backup SPKI pin and a documented rotation plan.

**Attack / demo**
- With v2 pinned, point at Burp again → connection fails (good).
- Then use **Frida/objection** (`android sslpinning disable`) on a rooted device to bypass the pin, and discuss the honest limitation: **pinning stops network attackers, not an attacker who fully controls the device.** That nuance is exactly what separates a junior from someone who gets it.

**Deliverable:** pinning implementation + a written cert-rotation runbook.

---

## Phase 4 — Local data security (Weeks 5–6)

**Learn**
- Threat = lost/stolen device, rooted device, device backups, other apps.
- Android **Keystore** (hardware-backed where available), `EncryptedSharedPreferences`, encrypted **DataStore**, **SQLCipher** for databases.
- `android:allowBackup` and what leaks via cloud/adb backup.
- `FLAG_SECURE` to block screenshots/recents-preview for sensitive screens.
- Clipboard hygiene, no sensitive data in logs, no secrets in cache.
- **No hardcoded secrets** (API keys, etc.) — and why moving them to the NDK only raises the bar slightly, never makes them safe. Secrets belong on the server.

**Implement (v1 → v2)**
- v1: Room DB in plaintext, `allowBackup=true`, PIN in a string resource, sensitive screen screenshot-able.
- v2: SQLCipher (or encrypted store), `allowBackup=false` / proper backup rules, `FLAG_SECURE`, secrets removed from the client.

**Attack / demo**
- `adb backup` / pull the DB from v1 and read it. Show v2's DB is encrypted.
- Decompile v1 with jadx and grep out the hardcoded PIN/key; show it's gone in v2.

**Deliverable:** local-storage hardening writeup + extracted-secret screenshot from v1.

---

## Phase 5 — App hardening & integrity (Weeks 6–7)

**Learn**
- **R8/ProGuard** obfuscation + shrinking — what it does and doesn't protect.
- **Root detection** and **tamper/repackaging detection** (signature check), and their limits on a determined attacker.
- **Play Integrity API** — the modern, server-verified way to check device/app/account integrity (replaces SafetyNet). Stress that the verdict must be **verified server-side**, never trusted on-device.
- WebView security: disable JS unless needed, `addJavascriptInterface` dangers, no loading untrusted URLs.
- **Deep link / intent security:** verified App Links, don't trust intent extras, exported components hygiene.
- Anti-instrumentation awareness (Frida detection) — note it's an arms race, not a wall.

**Implement (v1 → v2)**
- v2: R8 enabled with a sane keep-rules set, basic root/tamper check wired to a server-verified integrity signal, WebView and exported components locked down.

**Attack / demo**
- Decompile v1 (no obfuscation) vs v2 (obfuscated) in jadx — show how much harder v2 is to read.
- Repackage v1 with apktool to demonstrate why signature/integrity checks matter.

**Deliverable:** hardening section + obfuscated-vs-not decompile comparison.

---

## Phase 6 — Pen-test his own app + report (Weeks 7–8)

Now he turns attacker against `v1` vs `v2` systematically.

**Do**
- Run **MobSF** on both builds; compare the static-analysis scores.
- Walk the **MASTG** test cases relevant to each topic above and mark pass/fail for v1 and v2.
- Use **Burp + Frida + jadx + apktool** to attempt each attack and record outcome.

**Final deliverables**
1. **The app** — `v1-vulnerable`, `v2-hardened`, side by side, in a clean git history where each hardening step is its own PR/commit.
2. **A security report** — per topic: the risk, the MASVS requirement, the v1 weakness, the v2 fix, the attack attempt, and the result. Honest about residual risk (e.g., "pinning bypassable on a rooted device").
3. **A 20–30 min demo/presentation** — live MITM on v1, blocked on v2; live secret extraction from v1, encrypted in v2.

---

## Tool reference (one line each)

| Tool | Use |
|---|---|
| **Burp Suite** | Intercept/modify HTTPS traffic (MITM proxy) |
| **jadx-gui** | Decompile APK to readable code; hunt secrets |
| **apktool** | Unpack/repack/resign APK (repackaging attacks) |
| **MobSF** | Automated static + dynamic security scan |
| **Frida / objection** | Runtime hooking, SSL-pinning bypass, root checks bypass |
| **adb** | Pull files, backups, logs, install builds |
| **Magisk** | Root the test device/emulator |

---

## How to evaluate the intern

- **Depth over checklist:** does he understand *why* (threat model), or just copy fixes?
- **Honesty about limits:** the best sign of understanding is saying "this defense is bypassable on a rooted device, here's what it actually protects against."
- **Reproducibility:** can someone follow his report and reproduce both the attack and the fix?
- **Cert rotation:** does the pinning solution survive a renewed certificate? (The single most common real-world mistake.)

---

## Reference resources

- **OWASP MASVS** — the requirements he's verifying against.
- **OWASP MASTG** — concrete test cases and how-tos for each topic; this is the practical companion.
- Android developer docs: **Network Security Config**, **Android Keystore**, **BiometricPrompt**, **Play Integrity API**, **App Links**.

> Tip: have him keep a running log mapping each thing he implements to a MASVS ID. It turns the project into something that reads like a real security audit — which is exactly the skill you want him to walk away with.
