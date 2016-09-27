# Single Global Temporary Table Bulk Id Strategy for Hibernate
Hibernate's MultiTableBulkIdStrategy for Hibernate using using just a single pre-created global temporary table instead of a table per entity.

Can be useful in environments where DDL statements cannot be executed from application and managing a large number of ID tables is not practical.

## How to use
1. Build this project and add it as a dependency.
1. Create a shared global temporary table, e.g.

  ```
  create global temporary table HT_TEMP_IDS (ID CHAR(36), ENTITY_NAME VARCHAR(100)); 
  ``` 
1. Set the following Hibernate properties:

  ```
  configuration.setProperty(AvailableSettings.HQL_BULK_ID_STRATEGY, SingleGlobalTemporaryTableBulkIdStrategy.class.getName());
  configuration.setProperty(SingleGlobalTemporaryTableBulkIdStrategy.TABLE, "HT_TEMP_IDS");
  configuration.setProperty(SingleGlobalTemporaryTableBulkIdStrategy.DISCRIMINATOR_COLUMN, "ENTITY_NAME");
  ```

## Versions
* Hibernate 5.1.0
* JDK 1.8
