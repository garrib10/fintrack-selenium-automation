# FinTrack Selenium Automation Suite

A standalone Selenium end-to-end regression suite for the deployed FinTrack personal finance application.

## Project purpose

This repository demonstrates external UI automation and regression testing for critical FinTrack user workflows.

FinTrack and this Selenium automation suite are separate projects:

- [FinTrack application repository](https://github.com/garrib10/finance-operations-dashboard)
- [Deployed FinTrack application](https://finance-operations-dashboard.vercel.app/)

## Technology stack

- Java 21
- Selenium WebDriver
- JUnit 5
- Maven
- Google Chrome
- GitHub Actions planned for continuous integration

## Current status

Version `1.0.0-SNAPSHOT` is under active development.

Day 1 focuses on the Maven foundation, Selenium configuration, JUnit test execution, and the first browser test.

## Local prerequisites

- Java 21
- Maven 3.9 or later
- Google Chrome

## Build verification

```bash
mvn test
```
