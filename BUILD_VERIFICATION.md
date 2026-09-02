# Build verification

The project is configured for Java 21 and Paper API 1.21.8 (`1.21.8-R0.1-SNAPSHOT`).

The environment used to assemble this ZIP has Java 21, but outbound network/DNS access is unavailable, so the Gradle distribution and Paper API could not be downloaded here. Consequently, a local `./gradlew build` verification could not be completed in this environment, and no compiled JAR is falsely included.

On a network-enabled machine, run:

```bash
./gradlew build
```

or on Windows:

```bat
gradlew.bat build
```

The Gradle wrapper properties point to Gradle 8.14.3.
