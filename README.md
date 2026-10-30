# SmartSave — Autonomous Student Savings & Financial Discipline Application

A comprehensive **Java Mini Project (CSE203: OOP using Java)** implementing the complete technical architecture combining **JavaFX**, **Core Java**, **OOP Concepts**, **Java Collections**, **Data Structures & Algorithms (Trie, MergeSort, BinarySearch, Levenshtein DP)**, **Artificial Intelligence (Intent Classification & Regression Forecasting)**, and **JDBC Persistence (MySQL & SQLite)**.

---

## 1. Common Technical Architecture

```
                      JAVA MINI PROJECT
                        /          \
                       v            v
                   FRONTEND       BACKEND
                      |              |
                      v              v
                   JavaFX        Core Java
                                  /     \
                                 v       v
                                OOP   Collections <--- Data Structures (Trie)
                                 \       /     \           |
                                  v     v       v          v
                               Application Logic ---> Algorithms (MergeSort, Regression)
                                      ^                    |
                                      |                    v
                            Artificial Intelligence --->  JDBC
                                                           |
                                                           v
                                                    MySQL / SQLite
```

### Architectural Mapping & Implementation:
* **Frontend (`JavaFX`)**: Native desktop UI matching modern mobile design with rounded cards (`16px`–`20px`), smooth transitions, interactive circular progress indicators, and instant validation banners.
* **Core Java & OOP**: Encapsulation, inheritance, abstraction, and polymorphism structured across Service, Model, and DAO layers.
* **Data Structures (`algo.Trie`)**: Prefix tree for $O(L)$ instant search and autocomplete of usernames and contacts in `PayView` and `ConnectView`.
* **Algorithms (`algo.Algorithms` & `algo.TransactionAnalytics`)**:
  * **Merge Sort**: Stable $O(n \log n)$ transaction and user ranking.
  * **Binary Search**: Logarithmic $O(\log n)$ lookup across sorted collections.
  * **Levenshtein Edit Distance**: Dynamic programming string comparison tolerating user typos in search and AI query classification.
  * **Linear Least-Squares Regression**: Evaluates user saving trajectory to compute estimated days to target.
  * **Z-score Anomaly Detection**: Detects statistically anomalous spending outflows.
* **Artificial Intelligence (`ai.IntentClassifier` & `ai.AiInsightEngine`)**: Offline intent classification and personalized conversational coach providing predictive savings ETA, study guidance, and spending insights.
* **JDBC Persistence Layer**: Full CRUD support via standard JDBC for both **MySQL** (configured as primary via `db.properties` / environment variables) with seamless auto-fallback to embedded **SQLite** for zero-setup execution.

---

## 2. Key Features

1. **Virtual Locked Savings Vault**:
   * Users set a target savings goal. Funds placed in locked savings cannot be spent or withdrawn until the target is met or authorized via emergency channels.
   * Visual gauge tracks savings progress in Indian Rupees (`₹`).

2. **5 Controlled Early-Release Channels**:
   * **Parent Approval**: Verified sign-off from registered parent authority.
   * **Close Friend Approval**: Peer verification for unexpected student needs.
   * **Medical Emergency AI Verification**: Evaluates clinical keywords (`acute`, `trauma`, `icu`, etc.) and doctor registration numbers.
   * **AI Educational Study Modules**: Complete financial/CS micro-courses with forward-skip protection.
   * **Release Coins Economy**: Earn coins by watching ads or referring peers; redeem at ₹1 per coin.

3. **Guardian & Ward Supervision Hub**:
   * Bi-directional authority: Manage permissions of wards who listed you as their guardian, and inspect your own registered guardians.
   * Guardian authorization strictly required for savings target adjustments.

4. **App-to-App P2P Payments & Payment Requests**:
   * Instant transfers between registered students.
   * Peer-to-peer payment requests with real-time polling.
   * **Live Payment & Transfer History** on both Pay and Connection screens with counterparty name resolution, direction badges, and status pills.

5. **Multi-User Authentication**:
   * Create real accounts with full name, unique username, phone number, and password.
   * Secure password storage with salted SHA-256 hashing.

---

## 3. Database Setup & Switching

Configure database connectivity in `src/main/resources/com/example/myfirstapp/db.properties`:

### MySQL (Primary):
```properties
db.driver=com.mysql.cj.jdbc.Driver
db.url=jdbc:mysql://localhost:3306/smartsave?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=root
```
*Auto-fallback:* If MySQL is unreachable, SmartSave automatically falls back to local SQLite (`smartsave.db`).

### SQLite (Zero-config):
```properties
db.driver=org.sqlite.JDBC
db.url=jdbc:sqlite:smartsave.db
```

---

## 4. How to Build, Test, and Run

### Run Full Test Suite (44 Tests):
```powershell
./gradlew test
```

### Launch the Application:
```powershell
./gradlew run
```

---

## 5. Automated Test Suite (All 44 Passed)

```
AlgorithmsAndAiTest
  ✔ Regression forecast and anomaly detection
  ✔ Merge sort and binary search work on collections
  ✔ Trie returns prefix matches
  ✔ Intent classifier routes queries, tolerating typos

AppToAppPaymentTest
  ✔ Registration validation rejects duplicates and weak input
  ✔ Two newly registered IDs: request on one side, pay from the other, request cannot be paid twice

DatabasePersistenceTest
  ✔ Data persisted via JDBC survives closing and re-opening database connection

EarlyReleaseServiceTest
  ✔ Option 1: Parent approval successfully unlocks early release
  ✔ Option 2: Close friend approval successfully unlocks early release
  ✔ Option 3: Medical emergency AI verification succeeds on genuine urgent clinical data
  ✔ Option 3: Medical emergency AI verification rejects routine or incomplete reports
  ✔ Early withdrawal is locked when target is not reached and no approvals granted
  ✔ Direct withdrawal is unconditionally unlocked when target balance is achieved
  ✔ Wards appear for guardian and guardian can grant or revoke early withdrawal permissions
  ✔ Guardian can grant and revoke target modification permission for ward

EducationalTaskServiceTest
  ✔ Study session restricts forward skipping while allowing backward seeking
  ✔ Completing educational task awards Release Coins and prevents duplicate reward

FinancialCalculatorTest
  ✔ Balance greater than Release Coins
  ✔ Target increase recalculates consistently
  ✔ Release Coins greater than Balance
  ✔ Completed and incomplete savings targets
  ✔ Insufficient available balance prevents transfer
  ✔ Duplicate redemption attempts prevented
  ✔ Partial redemption of Release Coins
  ✔ Zero balance and zero Release Coins
  ✔ Preventing negative balances and double spending

ReleaseCoinServiceTest
  ✔ Watching an eligible ad awards up to 5 Release Coins maximum
  ✔ Duplicate rewards must be prevented for the same advertisement event
  ✔ Referral awards +25 coins when code belongs to another registered user
  ✔ Redeeming Release Coins unlocks early release and deducts balance

WalletServiceTest
  ✔ Virtual deposit increases available balance and records transaction
  ✔ Target successfully updates when Guardian permission is granted
  ✔ Locked savings must not be spendable through ordinary transfers
  ✔ Virtual transfer successfully decreases sender and increases recipient balance
  ✔ Target cannot be changed without Guardian permission
  ✔ Withdrawal is strictly capped by Release Coins when target is incomplete
  ✔ Withdrawing funds spends Release Coins and updates wallet balances accurately
  ✔ Self-transfers must be prevented
  ✔ Locking funds moves them from available balance to locked savings
  ✔ Withdrawal exceeding Effective Balance must be rejected without changing coins or balance
  ✔ Negative or zero transfer amounts must be rejected
  ✔ Transfers to non-existent users must be rejected
  ✔ Withdrawal when target is completed unlocks all funds without spending coins
  ✔ Transfers exceeding sender available balance must be rejected

BUILD SUCCESSFUL in 5s — 44 Tests Passed, 0 Failed, 0 Skipped
```
