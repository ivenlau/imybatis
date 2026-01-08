# imybatis

<!-- Plugin description -->
IntelliJ Platform plugin for MyBatis/MyBatis Plus development that enhances productivity with intelligent code completion, navigation, validation, and code generation capabilities.

## Key Features:
- Navigate between Mapper interfaces, XML files, and Entity classes
- Intelligent code completion for SQL keywords, table/column names, and parameters
- Semantic validation including ID consistency checks and type matching
- Generate Entity, Mapper, XML, Service, and Controller code from database tables
- MyBatis Plus support with BaseMapper analysis and Wrapper column name completion
- Support for MySQL/MariaDB and PostgreSQL dialects
<!-- Plugin description end -->

## Features

- **Code Navigation**: Navigate between Mapper interfaces, XML files, and Entity classes
- **Intelligent Code Completion**: SQL keywords, table/column names, and parameter completion
- **Semantic Validation**: ID consistency checks, type matching validation
- **Code Generation**: Generate Entity, Mapper, XML, Service, and Controller code from database tables
- **MyBatis Plus Support**: BaseMapper analysis and Wrapper column name completion
- **Database Support**: MySQL/MariaDB and PostgreSQL dialects

## Requirements

- JDK 17+
- Gradle 8.14+
- IntelliJ IDEA 2025.3+ (Ultimate or Community)
- Android Studio 2025.3+

## Building

```bash
./gradlew buildPlugin
```

## Running

```bash
./gradlew runIde
```

## Development

This plugin is built using:
- Kotlin for implementation
- FreeMarker for code generation templates
- IntelliJ Platform SDK for IDE integration

## License

See LICENSE file for details.
