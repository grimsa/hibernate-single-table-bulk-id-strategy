# Single Global Temporary Table Bulk Id Strategy for Hibernate

**Note: [Hibernate 5.2.8 added new Bulk Id strategies](http://in.relation.to/2017/02/01/non-temporary-table-bulk-id-strategies/), which make this project obsolete for most cases.**

Hibernate's MultiTableBulkIdStrategy for Hibernate using using just a single pre-created global temporary table instead of a table per entity.

Can be useful in environments where DDL statements cannot be executed from application and managing a large number of ID tables is not practical.

## How to use
1. Add a dependency to your project. For Maven, use the following:

  ```xml
  <dependency>
    <groupId>com.github.grimsa.hibernate</groupId>
    <artifactId>single-table-bulk-id-strategy</artifactId>
    <version>1.3</version>
  </dependency>
  ```
  For Gradle:

  ```
  dependencies {
     compile 'com.github.grimsa.hibernate:single-table-bulk-id-strategy:1.3'
  }
  ```
  
2. Create a shared global temporary table, e.g.

  ```
  create global temporary table HT_TEMP_IDS (ID CHAR(36), ENTITY_NAME VARCHAR(100));
  ```
3. Set the following Hibernate properties:

  ```
  config.setProperty(AvailableSettings.HQL_BULK_ID_STRATEGY, SingleGlobalTemporaryTableBulkIdStrategy.class.getName());
  config.setProperty(SingleGlobalTemporaryTableBulkIdStrategy.TABLE, "HT_TEMP_IDS");
  config.setProperty(SingleGlobalTemporaryTableBulkIdStrategy.ID_COLUMN, "ID");                      // "ID" is default
  config.setProperty(SingleGlobalTemporaryTableBulkIdStrategy.DISCRIMINATOR_COLUMN, "ENTITY_NAME");  // "ENTITY_NAME" is default
  config.setProperty(SingleGlobalTemporaryTableBulkIdStrategy.CLEAN_ROWS, "true");
  ```

## Release history
* 1.3 released 2020-04-08
    * Added support for overridden table ID columns when deleting from many-to-many join tables([#3](https://github.com/grimsa/hibernate-single-table-bulk-id-strategy/issues/3))
    * Upgraded Hibernate to 5.4.12
* 1.2 released 2017-07-24
    * Support different ID column names in entities ([#1](https://github.com/grimsa/hibernate-single-table-bulk-id-strategy/issues/1))
* 1.1 released 2016-09-29
    * Update to support Hibernate 5.2
* 1.0 released 2016-09-29
    * Initial version built for Hibernate 5.1 and JDK 1.8