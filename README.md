# FinTrack Selenium Automation Suite

![Java 21](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Selenium 4.49.0](https://img.shields.io/badge/Selenium-4.49.0-43B02A?logo=selenium&logoColor=white)
![JUnit 5.14.4](https://img.shields.io/badge/JUnit_5-5.14.4-25A162?logo=junit5&logoColor=white)
![Maven 3.9.16](https://img.shields.io/badge/Maven-3.9.16-C71A36?logo=apachemaven&logoColor=white)
![Page Object Model](https://img.shields.io/badge/Page_Object_Model-Architecture-6F42C1)
![Status](https://img.shields.io/badge/Status-In_Development-1D76DB)

A standalone Selenium end-to-end regression suite for the deployed FinTrack personal finance application.

This repository demonstrates external UI automation, positive and negative testing, reusable test architecture, and regression protection for critical user workflows.

> **Separate project:** FinTrack is the application under test. This repository contains only the external Selenium automation suite and does not contain the FinTrack application source code.

- [FinTrack application repository](https://github.com/garrib10/finance-operations-dashboard)

- [Deployed FinTrack application](https://finance-operations-dashboard.vercel.app/)

## Portfolio Story

> I built FinTrack as a full-stack application and then created a separate automated regression suite that validates critical user workflows against the deployed application.

## Tech Stack

| Area                            | Technologies                              |
| ------------------------------- | ----------------------------------------- |
| Language                        | Java 21                                   |
| UI automation                   | Selenium WebDriver 4.49.0                 |
| Test framework                  | JUnit 5.14.4                              |
| Build and dependency management | Maven 3.9.16                              |
| Browser                         | Google Chrome                             |
| Driver management               | Selenium Manager                          |
| Test architecture               | Page Object Model                         |
| Test reporting                  | Maven Surefire text and XML reports       |
| Configuration                   | Environment variables and `.env` template |
| Continuous integration          | GitHub Actions planned for Day 6          |

WebDriverManager is not currently required because Selenium Manager provides automatic browser-driver discovery and management.

## Project Highlights

- External end-to-end testing against the deployed FinTrack interface

- Page Object Model with focused page and component classes

- Centralized WebDriver creation and JUnit browser lifecycle management

- Fresh browser session for every test

- Explicit waits instead of `Thread.sleep()`

- Stable IDs, semantic elements, accessible names, and `data-testid` selectors

- Assertions kept in test classes rather than hidden inside Page Objects

- Positive and negative authentication coverage

- Parameterized protected-route testing

- Login-session persistence verification after browser refresh

- Logout and post-logout authorization verification

- Duplicate-registration and browser-validation coverage

- Authenticated income and expense transaction workflows

- Transaction creation, verification, editing, cancellation, updating, and deletion

- Description search and transaction-type filtering

- Required-field and minimum-amount validation

- Special-character boundary coverage for transaction descriptions

- Unique test data with automatic cleanup from the deployed application

- Environment-based configuration with credentials excluded from Git

- Repeatable local execution through Maven

- Human-readable and machine-readable Surefire reports

## Application Under Test

The Selenium suite interacts with FinTrack through its deployed frontend.

```mermaid
flowchart TD
    A["Selenium Automation Suite"]
    B["FinTrack Frontend — Vercel"]
    C["FinTrack Backend — Railway"]
    D["MySQL — Railway"]
    A --> B
    B --> C
    C --> D
```

Tests interact with FinTrack through the browser rather than directly calling its backend or database.

## Current Automated Scenarios

### Smoke Testing

- Deployed FinTrack login page loads successfully

### Authentication

- Registered user can log in

- Authenticated session survives browser refresh

- Invalid password displays the expected error

- User can log out

- Logged-out user cannot revisit a protected route

### Protected Routes

Unauthenticated users are redirected from:

- Dashboard

- Transactions

- Budgets

### Registration Validation

- Duplicate email registration is rejected

- Malformed email is blocked by browser-native validation

Successful registration is not part of routine regression execution because FinTrack does not currently provide account deletion. This prevents automated tests from continually creating permanent users.

### Transactions

- Create and verify an income transaction

- Create and verify an expense transaction

- Cancel an edit without changing the original transaction

- Update a transaction and verify the new values

- Delete a transaction and verify that it is removed

- Preserve apostrophes, quotation marks, and ampersands in descriptions

### Transaction Search and Filtering

- Search using a unique transaction description

- Search using a shared partial description

- Filter matching results by transaction type

- Reset filters before cleanup and subsequent operations

### Transaction Validation

- Reject an amount below the minimum value

- Reject a missing required description

- Verify invalid submissions remain on the transaction page

Transaction scenarios generate unique descriptions for every execution and remove successfully created records in cleanup blocks. This keeps tests independent while safely exercising the deployed application and persistent database.

## Testing and Quality

| Test area               |                                       Current result |
| ----------------------- | ---------------------------------------------------: |
| Smoke                   |                                       1 passing test |
| Authentication          |                                      3 passing tests |
| Protected routes        |                   3 passing parameterized executions |
| Registration validation |                                      2 passing tests |
| Transaction workflows   |                                      4 passing tests |
| Transaction filtering   |                                       1 passing test |
| Transaction validation  |                                      2 passing tests |
| **Total**               | **16 passing test executions across 7 test classes** |

The suite currently verifies:

- Application availability through the UI

- Successful and unsuccessful authentication

- JWT-backed session persistence after refresh

- Logout behavior

- Protected-route enforcement

- Duplicate-email handling

- Browser-native form validation

- Income and expense transaction persistence

- Transaction editing, cancellation, updating, and deletion

- Description search and transaction-type filtering

- Transaction form validation and special-character handling

- Unique test-data generation and post-test cleanup

- Browser setup and teardown

- Environment-based credential management

Run the complete suite:

```bash
mvn clean test
```

Run one test class:

```bash
mvn -Dtest=AuthenticationTest test
```

Surefire reports are generated at:

```text
target/surefire-reports
```

The report directory contains:

- Plain-text developer summaries

- XML reports suitable for CI processing and artifact upload

End-to-end code-coverage percentages are not reported because this suite interacts with FinTrack externally and does not instrument the application source code.

## Test Evidence and Screenshots

Portfolio evidence will be added incrementally as the suite develops.

Planned evidence includes:

- Successful local Maven regression execution

- Headed Chrome automation against deployed FinTrack

- Failure screenshots captured automatically by the test framework

- Successful GitHub Actions workflow execution

- Pull-request status checks

- Maven Surefire reports uploaded as CI artifacts

Screenshots will be stored under:

```text
docs/images
```

Actual image links will be added after the files exist so the README does not contain broken placeholders.

## Project Structure

| Path                                              | Purpose                                                                  |
| ------------------------------------------------- | ------------------------------------------------------------------------ |
| `.env.example`                                    | Safe template documenting required environment variables                 |
| `pom.xml`                                         | Maven project, dependency, Java, compiler, and test-runner configuration |
| `src/test/java/dev/portfolio/fintrack/components` | Reusable UI components shared across pages                               |
| `src/test/java/dev/portfolio/fintrack/config`     | Environment and test configuration                                       |
| `src/test/java/dev/portfolio/fintrack/core`       | WebDriver factory and JUnit test lifecycle                               |
| `src/test/java/dev/portfolio/fintrack/data`       | Unique automation test-data generation                                   |
| `src/test/java/dev/portfolio/fintrack/pages`      | Page Objects containing selectors, waits, and UI interactions            |
| `src/test/java/dev/portfolio/fintrack/tests`      | JUnit test classes containing scenarios and assertions                   |
| `target/surefire-reports`                         | Generated local test reports; excluded from Git                          |
| `docs/images`                                     | Portfolio screenshots and test evidence added later                      |

Current Java structure:

```text
src/test/java/dev/portfolio/fintrack/
├── components/
│   └── AppHeader.java
├── config/
│   └── TestConfig.java
├── core/
│   ├── AuthenticatedTest.java
│   ├── BaseTest.java
│   └── DriverFactory.java
├── data/
│   └── TestData.java
├── pages/
│   ├── BasePage.java
│   ├── DashboardPage.java
│   ├── LoginPage.java
│   ├── RegisterPage.java
│   └── TransactionsPage.java
└── tests/
    ├── AuthenticationTest.java
    ├── FinTrackSmokeTest.java
    ├── ProtectedRouteTest.java
    ├── RegistrationTest.java
    ├── TransactionFilterTest.java
    ├── TransactionTest.java
    └── TransactionValidationTest.java
```

## Automation Design

### Page Object Model

Page Objects contain:

- Element selectors

- Explicit waits

- Page-specific interactions

- Page-state accessors

Test classes contain:

- Test scenarios

- Expected outcomes

- JUnit assertions

This separation keeps tests readable while localizing UI-maintenance changes.

### WebDriver Lifecycle

`DriverFactory` creates the browser. `BaseTest` creates a new driver before every test and closes it afterward.

Each test begins with an isolated browser session and does not depend on authentication state left by another test.

### Test Independence

Tests do not depend on execution order. Authentication tests reuse one dedicated fictional account, while permanent account creation is excluded from routine execution.

Transaction tests create uniquely named records and use `finally` cleanup blocks so created data is removed even when an assertion fails. Test runs are currently serial because the suite operates against one shared deployed automation account.

### Explicit Waits

The suite waits for observable browser conditions, including:

- Expected URL paths

- Visible elements

- Clickable controls

- Authenticated navigation

- Loaded dashboard content

- Transaction form resets after successful submission

- Search and filter results

- Transaction rows appearing or disappearing after mutations

Fixed delays such as `Thread.sleep()` are not used.

### Selector Strategy

The suite prefers:

1. Stable element IDs

2. Released `data-testid` attributes

3. Semantic HTML and accessible names

4. Scoped CSS selectors

5. Short, meaning-based XPath only when Selenium lacks a suitable role locator

DOM-position-based selectors and long absolute XPath expressions are avoided.

## Local Prerequisites

- Java 21

- Maven 3.9 or later

- Google Chrome

Verify the environment:

```bash
java -version

mvn -version
```

Maven should report that it is running with Java 21.

## Environment Configuration

Copy the safe configuration template:

```bash
cp .env.example .env
```

Update `.env` with a dedicated fictional FinTrack automation account:

```bash
export FINTRACK_BASE_URL='https://finance-operations-dashboard.vercel.app'

export FINTRACK_TEST_EMAIL='replace-with-dedicated-test-email'

export FINTRACK_TEST_PASSWORD='replace-with-dedicated-test-password'

export FINTRACK_ALLOW_REGISTRATION='false'
```

Load the values into the current terminal:

```bash
source .env
```

The real `.env` file is excluded from Git and must never be committed.

## Running the Tests

Run all tests from a clean build:

```bash
source .env

mvn clean test
```

Run one test class:

```bash
mvn -Dtest=AuthenticationTest test
```

Run the protected-route tests:

```bash
mvn -Dtest=ProtectedRouteTest test
```

Run all transaction test classes:

```bash
mvn -Dtest="Transaction*" test
```

The browser currently runs visibly during local execution. Headless execution will be added before GitHub Actions integration.

## Test Reports

Maven Surefire generates results in:

```text
target/surefire-reports
```

View the readable test summaries:

```bash
cat target/surefire-reports/*.txt
```

Generated reports are excluded from Git because they are recreated during every test run. GitHub Actions will upload them as temporary workflow artifacts once CI is configured.

## Roadmap

- [x] Day 1 — Maven, Selenium, JUnit, and first browser test

- [x] Day 2 — Configuration, Page Object Model, and authentication automation

- [x] Day 3 — Transaction workflows

- [ ] Day 4 — Budget and dashboard workflows

- [ ] Day 5 — Reliability, tagging, screenshots, and headless execution

- [ ] Day 6 — GitHub Actions and automated PR checks

- [ ] Day 7 — Final documentation, evidence, and v1.0 release

## Current Status

Version `1.0.0-SNAPSHOT` is under active development.

Authentication and transaction automation are complete with 16 passing test executions across seven test classes. Budget, dashboard, reliability, CI, and final release work will be added incrementally.
