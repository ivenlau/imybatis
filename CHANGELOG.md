# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- Native shortcut key to navigate between Mapper interfaces, XML files, and Entity classes

## [1.0.0] - 2025-01-12
### Added
- Database tool window integration for code generation
- Code generation wizard with multi-step UI
- Support for MySQL and PostgreSQL databases
- Generate Entity classes with field mapping from database columns
- Generate Mapper interfaces with CRUD method declarations
- Generate MyBatis XML mapper files with SQL statements
- Generate Service interfaces and implementations
- Generate Controller classes with REST endpoints
- MyBatis Plus support with BaseMapper extension
- Lombok support with @Data and @EqualsAndHashCode annotations
- Traditional getter/setter generation when Lombok is disabled
- Batch operations support (batchInsert, batchUpdate, batchDelete, batchDeleteByIds)
- Insert on duplicate update support with ON DUPLICATE KEY UPDATE
- Combined batchInsertOnDuplicate method for batch operations with duplicate handling
- LocalDateTime vs Date type selection for date/time fields
- Java type import optimization (import java.util.List, java.time.*, etc.)
- @Param annotation support for Mapper interface methods
- Intelligent code completion for SQL keywords in mapper XML files
- Code navigation between Mapper interfaces, XML files, and Entity classes
- Custom plugin icon with pencil and "My Batis" text design
- Column name to property name conversion (snake_case to camelCase)
- Primary key detection and @TableId annotation generation
- @TableField annotation for columns with different names from properties

### Changed
- Improved code quality with explicit imports instead of fully qualified names
- Optimized template engine for better performance
- Enhanced field type mapping with proper Java type detection

### Fixed
- Fixed SQL code completion not working in MyBatis mapper XML files
- Fixed Lombok option being ignored in code generation
- Fixed missing getter/setter generation when Lombok is disabled
- Fixed Mapper interface missing method declarations for traditional MyBatis
- Fixed parameter type mapping for selectById and deleteById methods

## [0.1.0] - 2025-01-11

### Added
- Initial release of imybatis plugin
- Basic code generation from database tables
- MyBatis and MyBatis Plus support
- Code navigation between Mapper, XML, and Entity
- Intelligent code completion for SQL in mapper XML files
- Database metadata extraction for MySQL and PostgreSQL
