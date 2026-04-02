# openDoJa

`openDoJa` is a desktop-focused clean-room reimplementation of the DoJa 5.1 runtime and related APIs, aimed at running i-appli Java games on modern computers.

## Build

```bash
mvn -q -DskipTests package
```

## GitHub Actions

- Every push to `master` rebuilds the rolling `nightly` GitHub release and replaces its attached JAR.
- A GitHub release is created when the `pom.xml` version changes.

## Download

- Latest version: https://github.com/GrenderG/openDoJa/releases/latest
- Nightly version: https://github.com/GrenderG/openDoJa/releases/download/nightly/opendoja-nightly.jar

## Run

Open the desktop launcher UI:

```bash
java -jar target/opendoja-{version}.jar
```

Launch a specific JAM directly through the packaged launcher:

```bash
java -jar target/opendoja-{version}.jar --run-jam <game.jam>
```

## Reporting Broken Games

If a game does not work, please open a GitHub issue using the broken game report template.

Include:

- a text description of the issue
- the exact game that does not work
- screenshots or videos if they help explain the problem
- logs or stack traces if the issue is a crash

For the bundled local workflow used during development, see `scripts/`.
