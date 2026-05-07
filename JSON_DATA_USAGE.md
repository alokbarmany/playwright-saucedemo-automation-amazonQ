# JSON Data Management in Playwright Framework

## Overview
This framework uses JSON files to externalize test data from feature files. Each scenario can load its own JSON file using Cucumber tags, and data is accessed via placeholder resolution in step definitions.

---

## Directory Structure

```
src/test/resources/config/
├── dev/
│   └── data/
│       ├── loginData.json
│       ├── checkoutData.json
│       └── productData.json
├── sit/
│   └── data/
│       ├── loginData.json
│       └── ...
└── uat/
    └── data/
        ├── loginData.json
        └── ...
```

---

## JSON File Format

### Example: loginData.json
```json
[
    {
        "username": "standard_user",
        "password": "secret_sauce",
        "address": {
            "street": "3 Marshall St",
            "city": "Irvington",
            "state": "NJ",
            "zip": "07111"
        }
    },
    {
        "username": "visual_user",
        "password": "secret_sauce",
        "address": {
            "street": "123 Main St",
            "city": "Anytown",
            "state": "CA",
            "zip": "12345"
        }
    }
]
```

**Important Notes:**
- JSON must be an **array** (even for single test data)
- Framework automatically loads the **first element** (index 0)
- Nested objects are flattened with dot notation: `address.street`, `address.city`

---

## How to Use JSON Data in Scenarios

### Step 1: Add @dataFile Tag to Scenario

```gherkin
Feature: Login

  @regression @dataFile:config/{env}/data/loginData.json
  Scenario: Successful login with valid credentials
    Given the user is on the login page
    When the user logs in with username "${username}" and password "${password}"
    Then the user should be on the inventory page
```

**Tag Format:**
```
@dataFile:config/{env}/data/<filename>.json
```

- `{env}` is a placeholder that gets replaced with actual environment (dev/sit/uat)
- Environment is determined by `-Denv=sit` JVM argument or defaults to `dev`

### Step 2: Use Placeholders in Feature Steps

**Syntax:** `"${key}"`

**Examples:**
```gherkin
# Simple values
When the user logs in with username "${username}" and password "${password}"

# Nested values (use dot notation)
Then user print address information from "${address}" section of the data file
```

---

## How Data Loading Works (Internal Flow)

### 1. Before Scenario Execution
```
Hooks.setUp(Scenario scenario)
  ↓
PlaywrightManager.resetRowIndex()       → row index set to 0
  ↓
PlaywrightManager.initBrowser()         → browser opens
  ↓
Parse @dataFile tag from scenario tags
  ↓
JsonDataReader.loadTestData(path)       → full JSON array loaded
  ↓
Load full JSON array → Flatten each object → Store as List<Map<String, String>>
  ↓
JsonDataReader.setCurrentRow(row[0])    → first row set as active
```

### 2. During Step Execution
```
Step Definition receives: "${username}"
  ↓
JsonDataReader.resolvePlaceholder("${username}")
  ↓
Replace ${username} with value from currentRow Map
  ↓
Returns: "standard_user"
```

### 3. After Each Step (@AfterStep)
```
Hooks.afterStep()
  ↓
Check: currentRowIndex < allData.size() - 1 ?
  ↓ YES (more rows remain)          ↓ NO (last row)
incrementRowIndex()              do nothing
  ↓
setCurrentRow(row[nextIndex])
  ↓
PlaywrightManager.closeBrowser()
  ↓
PlaywrightManager.initBrowser()
```

### 4. After Scenario Execution
```
Hooks.tearDown()
  ↓
PlaywrightManager.closeBrowser()    → final browser close
  ↓
PlaywrightManager.resetRowIndex()   → reset row index
  ↓
JsonDataReader.clearTestData()      → remove both ThreadLocals
```

---

## Data-Driven Iteration Without Scenario Outline

### Overview
Since Scenario Outline is not used in this framework, data-driven iteration is handled
automatically by `Hooks.@AfterStep`. After each step completes, Hooks checks if more
data rows remain and restarts the browser with the next row active. Step definitions
execute once per invocation against the current active row — no loops required.

### How It Works
```
JSON Array loaded by Hooks → [row0, row1, row2, ...rowN]
              ↓
Hooks sets row[0] as currentRow, rowIndex = 0
              ↓
  ┌──────────────────────────────────────────────────────┐
  │  @Given step executes  → uses currentRow (row 0)     │
  │      ↓                                               │
  │  @AfterStep fires                                    │
  │    rowIndex(0) < size-1 → YES                        │
  │    incrementRowIndex() → rowIndex = 1                │
  │    setCurrentRow(row[1])                             │
  │    closeBrowser() → initBrowser()                    │
  └──────────────────────────────────────────────────────┘
              ↓
  ┌──────────────────────────────────────────────────────┐
  │  @When step executes   → uses currentRow (row 1)     │
  │      ↓                                               │
  │  @AfterStep fires                                    │
  │    rowIndex(1) < size-1 → NO (last row)              │
  │    do nothing                                        │
  └──────────────────────────────────────────────────────┘
              ↓
  All rows executed within single Scenario
```

### Key API Methods for Iteration

| Method | Purpose |
|--------|---------|
| `getAllTestData()` | Returns full `List<Map<String, String>>` of all JSON array objects |
| `setCurrentRow(Map<String, String> row)` | Sets the active data row for placeholder resolution |
| `getTestData()` | Returns the current active row Map |
| `resolvePlaceholder(String text)` | Resolves `${key}` against the current active row |

### Example: Step Definition (No Loop Required)
```java
@When("the user logs in with username {string} and password {string}")
public void theUserLogsIn(String username, String password) {
    loginPage = new LoginPage(PlaywrightManager.getPage());         // fresh page object
    String resolvedUsername = JsonDataReader.resolvePlaceholder(username);
    String resolvedPassword = JsonDataReader.resolvePlaceholder(password);
    loginPage.login(resolvedUsername, resolvedPassword);            // executes for current row
    // Hooks.@AfterStep automatically advances to next row + restarts browser
}
```

---

## Browser Lifecycle Per Data Row

### Overview
Browser lifecycle is fully managed by `Hooks`. Each data row gets a fresh browser
instance. `@AfterStep` restarts the browser between rows. `@After` handles the
final close. Step definitions have zero browser lifecycle responsibility.

### Full Lifecycle Flow
```
┌─────────────────────────────────────────────────────────────┐
│  Hooks.setUp()                                              │
│    PlaywrightManager.resetRowIndex()  → rowIndex = 0        │
│    PlaywrightManager.initBrowser()    → browser opens       │
│    JsonDataReader.loadTestData()      → all rows loaded      │
│    JsonDataReader.setCurrentRow(row[0]) → row 0 active      │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│  @Given step executes using row 0                           │
│  @AfterStep: rowIndex(0) < size-1 → YES                     │
│    incrementRowIndex()  → rowIndex = 1                      │
│    setCurrentRow(row[1])                                    │
│    PlaywrightManager.closeBrowser()   ← close row 0 browser │
│    PlaywrightManager.initBrowser()    ← open row 1 browser  │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│  @When step executes using row 1                            │
│  @AfterStep: rowIndex(1) < size-1 → NO (last row)           │
│    do nothing                                               │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│  @Then steps execute using row 1 (last active row)          │
│  @AfterStep: rowIndex(1) < size-1 → NO for each @Then       │
│    do nothing                                               │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│  Hooks.tearDown()                                           │
│    PlaywrightManager.closeBrowser()   → final browser close │
│    PlaywrightManager.resetRowIndex()  → reset index         │
│    JsonDataReader.clearTestData()     → ThreadLocal cleanup  │
└─────────────────────────────────────────────────────────────┘
```

### Where It Is Implemented

| What | Where | File |
|------|-------|------|
| Browser open on scenario start | `Hooks.setUp()` | `src/test/java/com/saucedemo/hooks/Hooks.java` |
| Row index reset | `Hooks.setUp()` | `src/test/java/com/saucedemo/hooks/Hooks.java` |
| Browser restart + row advance between steps | `Hooks.afterStep()` | `src/test/java/com/saucedemo/hooks/Hooks.java` |
| Row index tracking | `PlaywrightManager` | `src/test/java/com/saucedemo/context/PlaywrightManager.java` |
| Final browser close | `Hooks.tearDown()` | `src/test/java/com/saucedemo/hooks/Hooks.java` |
| ThreadLocal cleanup | `Hooks.tearDown()` | `src/test/java/com/saucedemo/hooks/Hooks.java` |

### How @AfterStep Works
```java
// Located in: src/test/java/com/saucedemo/hooks/Hooks.java
@AfterStep
public void afterStep() {
    List<Map<String, String>> allData = JsonDataReader.getAllTestData();
    if (allData == null) return;

    int currentIndex = PlaywrightManager.getCurrentRowIndex();
    if (currentIndex < allData.size() - 1) {          // more rows remain
        PlaywrightManager.incrementRowIndex();         // advance row pointer
        JsonDataReader.setCurrentRow(allData.get(PlaywrightManager.getCurrentRowIndex()));
        PlaywrightManager.closeBrowser();             // close current browser
        PlaywrightManager.initBrowser();              // open fresh browser
    }
}
```

### Why Page Objects Are Re-instantiated Per Step
Since `@AfterStep` opens a new browser after each step, page objects must be
re-created at the start of each step to bind to the fresh `Page` instance.

```java
@Given("the user is on the login page")
public void theUserIsOnTheLoginPage() {
    loginPage = new LoginPage(PlaywrightManager.getPage());  // always fresh Page
    loginPage.navigate(ConfigReader.get("base.url"));
}
```

```
After @Given  → @AfterStep restarts browser → new Page created
After @When   → @AfterStep restarts browser → new Page created
Step defs re-instantiate page objects using PlaywrightManager.getPage()
```

### Why @AfterStep Does Nothing on the Last Row
The condition `currentIndex < allData.size() - 1` ensures the browser is only
restarted between rows. On the last row, `@AfterStep` skips the restart and
`Hooks.tearDown()` handles the final close.

```
Row 0 done → @AfterStep restarts browser   ✅ (more rows remain)
Row 1 done → @AfterStep does nothing       ✅ (last row, tearDown() will close)
```

### Console Output Example (2 rows in JSON)
```
--- Data Row [1/2] ---
Username: standard_user
Login successful for: standard_user

--- Data Row [2/2] ---
Username: visual_user
Login successful for: visual_user

=== Address Information - Row [1] ===
Street: 3 Marshall St
City: Irvington
State: NJ
Zip: 07111
=====================================

=== Address Information - Row [2] ===
Street: 123 Main St
City: Anytown
State: CA
Zip: 12345
=====================================
```

### Adding More Test Data
Simply add more objects to the JSON array — no code changes required:
```json
[
    { "username": "standard_user", "password": "secret_sauce", ... },
    { "username": "visual_user",   "password": "secret_sauce", ... },
    { "username": "problem_user",  "password": "secret_sauce", ... }
]
```
The loop automatically picks up the new row and executes the scenario steps for it.

---

## JSON Flattening Logic

**Original JSON:**
```json
{
    "username": "standard_user",
    "password": "secret_sauce",
    "address": {
        "street": "3 Marshall St",
        "city": "Irvington",
        "state": "NJ",
        "zip": "07111"
    }
}
```

**Flattened Map:**
```
username       → "standard_user"
password       → "secret_sauce"
address.street → "3 Marshall St"
address.city   → "Irvington"
address.state  → "NJ"
address.zip    → "07111"
```

---

## Accessing Data in Step Definitions

### Method 1: Placeholder Resolution (Recommended)
```java
@When("the user logs in with username {string} and password {string}")
public void theUserLogsIn(String username, String password) {
    String resolvedUsername = JsonDataReader.resolvePlaceholder(username);
    String resolvedPassword = JsonDataReader.resolvePlaceholder(password);
    loginPage.login(resolvedUsername, resolvedPassword);
}
```

### Method 2: Direct Map Access
```java
private final Map<String, String> testData = JsonDataReader.getTestData();

@Then("user print address information from {string} section of the data file")
public void userPrintAddressInformation(String section) {
    String resolvedSection = JsonDataReader.resolvePlaceholder(section);
    System.out.println("Street: " + testData.get(resolvedSection + ".street"));
    System.out.println("City: " + testData.get(resolvedSection + ".city"));
    System.out.println("State: " + testData.get(resolvedSection + ".state"));
    System.out.println("Zip: " + testData.get(resolvedSection + ".zip"));
}
```

### Method 3: Direct Key Access
```java
String username = JsonDataReader.get("username");
String city = JsonDataReader.get("address.city");
```

---

## Multiple Scenarios with Different JSON Files

```gherkin
Feature: E-Commerce Tests

  @dataFile:config/{env}/data/loginData.json
  Scenario: User login
    Given the user is on the login page
    When the user logs in with username "${username}" and password "${password}"
    Then the user should be logged in

  @dataFile:config/{env}/data/checkoutData.json
  Scenario: Checkout process
    Given the user adds product "${productName}" to cart
    When the user proceeds to checkout with card "${cardNumber}"
    Then order should be placed successfully

  @dataFile:config/{env}/data/inventoryData.json
  Scenario: Inventory validation
    Given the user is on inventory page
    Then user should see "${itemCount}" products
```

Each scenario loads its own JSON file independently.

---

## Running Tests with Different Environments

```bash
# Default environment (dev)
mvn test

# SIT environment
mvn test -Denv=sit

# UAT environment
mvn test -Denv=uat
```

The framework automatically loads the correct JSON file based on environment:
- `mvn test` → `config/dev/data/loginData.json`
- `mvn test -Denv=sit` → `config/sit/data/loginData.json`
- `mvn test -Denv=uat` → `config/uat/data/loginData.json`

---

## Best Practices

### 1. JSON File Naming Convention
- Use descriptive names: `loginData.json`, `checkoutData.json`, `productData.json`
- Keep one JSON file per feature/functionality
- Maintain same file names across all environments (dev/sit/uat)

### 2. JSON Structure
- Always use JSON array format `[{...}]`
- Keep nested objects shallow (max 2-3 levels)
- Use consistent key names across environments

### 3. Placeholder Usage
- Always use `"${key}"` format in feature files
- Use meaningful key names: `${username}` not `${u1}`
- For nested data, reference parent: `"${address}"` then access `address.street`

### 4. Data Isolation
- Each scenario should have its own @dataFile tag
- Don't share JSON files between unrelated scenarios
- Use ThreadLocal to ensure parallel execution safety

---

## Troubleshooting

### Issue: "Data file not found"
**Cause:** Incorrect path in @dataFile tag
**Solution:** Verify path matches actual file location in resources folder

### Issue: "NullPointerException when accessing data"
**Cause:** Missing @dataFile tag on scenario
**Solution:** Add @dataFile tag before scenario

### Issue: "Placeholder not resolved"
**Cause:** Key doesn't exist in JSON or typo in placeholder
**Solution:** Check JSON file has the key and placeholder syntax is correct `${key}`

### Issue: "Wrong environment data loaded"
**Cause:** Environment not specified or incorrect
**Solution:** Run with `-Denv=<environment>` or check default.env in config.properties

---

## API Reference

### PlaywrightManager Methods

| Method | Description |
|--------|-------------|
| `initBrowser()` | Launch browser and create new Page |
| `getPage()` | Get current active Page instance |
| `closeBrowser()` | Close Page, Browser and Playwright |
| `getCurrentRowIndex()` | Get current data row index |
| `incrementRowIndex()` | Advance to next row index |
| `resetRowIndex()` | Reset row index to 0 |

### JsonDataReader Methods

| Method | Description | Example |
|--------|-------------|---------|
| `loadTestData(String path)` | Load JSON file, store all rows as List | `loadTestData("config/dev/data/loginData.json")` |
| `getAllTestData()` | Get all rows as List of Maps | `List<Map<String, String>> rows = getAllTestData()` |
| `setCurrentRow(Map<String, String> row)` | Set active row for resolution | `setCurrentRow(allData.get(i))` |
| `getTestData()` | Get current active row as Map | `Map<String, String> data = getTestData()` |
| `get(String key)` | Get value by key from current row | `String user = get("username")` |
| `resolvePlaceholder(String text)` | Replace ${key} with value from current row | `resolvePlaceholder("${username}")` |
| `clearTestData()` | Clear both ThreadLocals | `clearTestData()` |

---

## Example: Complete Scenario Flow

**Feature File:**
```gherkin
@dataFile:config/{env}/data/loginData.json
Scenario: Login and verify address
  Given the user is on the login page
  When the user logs in with username "${username}" and password "${password}"
  Then user print address information from "${address}" section of the data file
```

**JSON File (config/dev/data/loginData.json):**
```json
[
    {
        "username": "standard_user",
        "password": "secret_sauce",
        "address": {
            "street": "3 Marshall St",
            "city": "Irvington",
            "state": "NJ",
            "zip": "07111"
        }
    }
]
```

**Step Definition:**
```java
@When("the user logs in with username {string} and password {string}")
public void theUserLogsIn(String username, String password) {
    String resolvedUsername = JsonDataReader.resolvePlaceholder(username);
    String resolvedPassword = JsonDataReader.resolvePlaceholder(password);
    loginPage.login(resolvedUsername, resolvedPassword);
}
```

**Console Output:**
```
=== Address Information ===
Street: 3 Marshall St
City: Irvington
State: NJ
Zip: 07111
==========================
```

---

## Summary

✅ Use `@dataFile:config/{env}/data/<file>.json` tag on scenarios
✅ Reference data with `"${key}"` placeholders in feature steps
✅ Access nested data with dot notation: `address.street`
✅ Each scenario can have its own JSON file
✅ Environment-specific data loaded automatically
✅ All JSON array rows iterated within a single Scenario (no Scenario Outline)
✅ `Hooks.@AfterStep` drives browser restart and row advancement automatically
✅ Step definitions have zero browser lifecycle responsibility
✅ Page objects re-instantiated per step using `PlaywrightManager.getPage()`
✅ Adding rows to JSON automatically increases iteration count
✅ ThreadLocal ensures parallel execution safety
