# Single Global Temporary Table Bulk Id Strategy for Hibernate
Hibernate's MultiTableBulkIdStrategy for Hibernate using using just a single pre-created global temporary table instead of a table per entity.

Can be useful in environments where DDL statements cannot be executed from application and managing a large number of ID tables is not practical.

## How to use
1. Add a dependency to your project. For Maven, use the following:

  ```xml
  <dependency>
    <groupId>com.github.grimsa.hibernate</groupId>
    <artifactId>single-table-bulk-id-strategy</artifactId>
    <version>1.0</version>
  </dependency>
  ```
  For Gradle:

  ```
  dependencies {
     compile 'com.github.grimsa.hibernate:single-table-bulk-id-strategy:1.0'
  }
  ```
  
1. Create a shared global temporary table, e.g.

  ```
  create global temporary table HT_TEMP_IDS (ID CHAR(36), ENTITY_NAME VARCHAR(100)); 
  ``` 
1. Set the following Hibernate properties:

  ```
  configuration.setProperty(AvailableSettings.HQL_BULK_ID_STRATEGY, SingleGlobalTemporaryTableBulkIdStrategy.class.getName());
  configuration.setProperty(SingleGlobalTemporaryTableBulkIdStrategy.TABLE, "HT_TEMP_IDS");
  configuration.setProperty(SingleGlobalTemporaryTableBulkIdStrategy.DISCRIMINATOR_COLUMN, "ENTITY_NAME");
  configuration.setProperty(SingleGlobalTemporaryTableBulkIdStrategy.CLEAN_ROWS, "true");
  ```

## Release history

* 1.0 released 2016-09-29. Requires Hibernate 5.1 and JDK 1.8