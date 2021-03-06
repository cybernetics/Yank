/**
 * Copyright 2015 Knowm Inc. (http://knowm.org) and contributors.
 * Copyright 2011-2015 Xeiam LLC (http://xeiam.com) and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.knowm.yank;

import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.ArrayListHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.knowm.yank.exceptions.SQLStatementNotFoundException;
import org.knowm.yank.handlers.InsertedIDResultSetHandler;
import org.knowm.yank.processors.YankBeanProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariDataSource;

/**
 * A wrapper for DBUtils' QueryRunner's methods: update, query, and batch. Connections are retrieved from the connection pool in DBConnectionManager.
 *
 * @author timmolter
 */
public final class Yank {

  private static final YankPoolManager YANK_POOL_MANAGER = YankPoolManager.INSTANCE;

  /** slf4J logger wrapper */
  private static Logger logger = LoggerFactory.getLogger(Yank.class);

  private static final String BEAN_EXCEPTION_MESSAGE = "Error converting row to bean!! Make sure you have a default no-args constructor!";
  private static final String QUERY_EXCEPTION_MESSAGE = "Error in SQL query!!!";

  /**
   * Prevent class instantiation with private constructor
   */
  private Yank() {

  }

  // ////// INSERT //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Executes a given INSERT SQL prepared statement matching the sqlKey String in a properties file loaded via Yank.addSQLStatements(...). Returns the
   * auto-increment id of the inserted row.
   *
   * @param sqlKey The SQL Key found in a properties file corresponding to the desired SQL statement value
   * @param params The replacement parameters
   * @return the auto-increment id of the inserted row, or null if no id is available
   * @throws SQLStatementNotFoundException if an SQL statement could not be found for the given sqlKey String
   */
  public static Long insertSQLKey(String sqlKey, Object[] params) {

    String sql = YANK_POOL_MANAGER.getMergedSqlProperties().getProperty(sqlKey);
    if (sql == null || sql.equalsIgnoreCase("")) {
      throw new SQLStatementNotFoundException();
    } else {
      return insert(sql, params);
    }
  }

  /**
   * Executes a given INSERT SQL prepared statement. Returns the auto-increment id of the inserted row.
   *
   * @param sql The query to execute
   * @param params The replacement parameters
   * @return the auto-increment id of the inserted row, or null if no id is available
   */
  public static Long insert(String sql, Object[] params) {

    Long returnLong = null;

    try {
      ResultSetHandler<Long> rsh = new InsertedIDResultSetHandler();
      returnLong = new QueryRunner(YANK_POOL_MANAGER.getDataSource()).insert(sql, rsh, params);
    } catch (SQLException e) {
      handleSQLException(e);
    }

    return returnLong == null ? 0 : returnLong;
  }

  // ////// INSERT, UPDATE, DELETE, or UPSERT //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Executes the given INSERT, UPDATE, DELETE, REPLACE or UPSERT SQL statement matching the sqlKey String in a properties file loaded via
   * Yank.addSQLStatements(...). Returns the number of rows affected.
   *
   * @param sqlKey The SQL Key found in a properties file corresponding to the desired SQL statement value
   * @param params The replacement parameters
   * @return The number of rows affected
   * @throws SQLStatementNotFoundException if an SQL statement could not be found for the given sqlKey String
   */
  public static int executeSQLKey(String sqlKey, Object[] params) {

    String sql = YANK_POOL_MANAGER.getMergedSqlProperties().getProperty(sqlKey);
    if (sql == null || sql.equalsIgnoreCase("")) {
      throw new SQLStatementNotFoundException();
    } else {
      return execute(sql, params);
    }
  }

  /**
   * Executes the given INSERT, UPDATE, DELETE, REPLACE or UPSERT SQL prepared statement. Returns the number of rows affected.
   *
   * @param sql The query to execute
   * @param params The replacement parameters
   * @return The number of rows affected
   */
  public static int execute(String sql, Object[] params) {

    int returnInt = 0;

    try {

      returnInt = new QueryRunner(YANK_POOL_MANAGER.getDataSource()).update(sql, params);

    } catch (SQLException e) {
      handleSQLException(e);
    }

    return returnInt;
  }

  // ////// Single Scalar QUERY //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Return just one scalar given a SQL Key using an SQL statement matching the sqlKey String in a properties file loaded via
   * Yank.addSQLStatements(...). If more than one row match the query, only the first row is returned.
   *
   * @param sqlKey The SQL Key found in a properties file corresponding to the desired SQL statement value
   * @param scalarType The Class of the desired return scalar matching the table
   * @param params The replacement parameters
   * @return The Object
   * @throws SQLStatementNotFoundException if an SQL statement could not be found for the given sqlKey String
   */
  public static <T> T queryScalarSQLKey(String sqlKey, Class<T> scalarType, Object[] params) {

    String sql = YANK_POOL_MANAGER.getMergedSqlProperties().getProperty(sqlKey);
    if (sql == null || sql.equalsIgnoreCase("")) {
      throw new SQLStatementNotFoundException();
    } else {
      return queryScalar(sql, scalarType, params);
    }

  }

  /**
   * Return just one scalar given a an SQL statement
   *
   * @param scalarType The Class of the desired return scalar matching the table
   * @param params The replacement parameters
   * @return The scalar Object
   * @throws SQLStatementNotFoundException if an SQL statement could not be found for the given sqlKey String
   */
  public static <T> T queryScalar(String sql, Class<T> scalarType, Object[] params) {

    T returnObject = null;

    try {

      ScalarHandler<T> resultSetHandler = new ScalarHandler<T>();

      returnObject = new QueryRunner(YANK_POOL_MANAGER.getDataSource()).query(sql, resultSetHandler, params);

    } catch (SQLException e) {
      handleSQLException(e);
    }

    return returnObject;
  }

  // ////// Single Object QUERY //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Return just one Bean given a SQL Key using an SQL statement matching the sqlKey String in a properties file loaded via
   * Yank.addSQLStatements(...). If more than one row match the query, only the first row is returned.
   *
   * @param sqlKey The SQL Key found in a properties file corresponding to the desired SQL statement value
   * @param params The replacement parameters
   * @param beanType The Class of the desired return Object matching the table
   * @return The Object
   * @throws SQLStatementNotFoundException if an SQL statement could not be found for the given sqlKey String
   */
  public static <T> T queryBeanSQLKey(String sqlKey, Class<T> beanType, Object[] params) {

    String sql = YANK_POOL_MANAGER.getMergedSqlProperties().getProperty(sqlKey);
    if (sql == null || sql.equalsIgnoreCase("")) {
      throw new SQLStatementNotFoundException();
    } else {
      return queryBean(sql, beanType, params);
    }

  }

  /**
   * Return just one Bean given an SQL statement. If more than one row match the query, only the first row is returned.
   *
   * @param sql The SQL statement
   * @param params The replacement parameters
   * @param beanType The Class of the desired return Object matching the table
   * @return The Object
   */
  public static <T> T queryBean(String sql, Class<T> beanType, Object[] params) {

    T returnObject = null;

    try {

      BeanHandler<T> resultSetHandler = new BeanHandler<T>(beanType, new BasicRowProcessor(new YankBeanProcessor<T>(beanType)));

      returnObject = new QueryRunner(YANK_POOL_MANAGER.getDataSource()).query(sql, resultSetHandler, params);

    } catch (SQLException e) {
      handleSQLException(e);
    }

    return returnObject;
  }

  // ////// Object List QUERY //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Return a List of Beans given a SQL Key using an SQL statement matching the sqlKey String in a properties file loaded via
   * Yank.addSQLStatements(...).
   *
   * @param sqlKey The SQL Key found in a properties file corresponding to the desired SQL statement value
   * @param beanType The Class of the desired return Objects matching the table
   * @param params The replacement parameters
   * @return The List of Objects
   * @throws SQLStatementNotFoundException if an SQL statement could not be found for the given sqlKey String
   */
  public static <T> List<T> queryBeanListSQLKey(String sqlKey, Class<T> beanType, Object[] params) {

    String sql = YANK_POOL_MANAGER.getMergedSqlProperties().getProperty(sqlKey);
    if (sql == null || sql.equalsIgnoreCase("")) {
      throw new SQLStatementNotFoundException();
    } else {
      return queryBeanList(sql, beanType, params);
    }
  }

  /**
   * Return a List of Beans given an SQL statement
   *
   * @param sql The SQL statement
   * @param beanType The Class of the desired return Objects matching the table
   * @param params The replacement parameters
   * @return The List of Objects
   */
  public static <T> List<T> queryBeanList(String sql, Class<T> beanType, Object[] params) {

    List<T> returnList = null;

    try {

      BeanListHandler<T> resultSetHandler = new BeanListHandler<T>(beanType, new BasicRowProcessor(new YankBeanProcessor<T>(beanType)));

      returnList = new QueryRunner(YANK_POOL_MANAGER.getDataSource()).query(sql, resultSetHandler, params);

    } catch (SQLException e) {
      handleSQLException(e);
    }
    return returnList;
  }

  // ////// Column List QUERY //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Return a List of Objects from a single table column given a SQL Key using an SQL statement matching the sqlKey String in a properties file loaded
   * via Yank.addSQLStatements(...).
   *
   * @param sqlKey The SQL Key found in a properties file corresponding to the desired SQL statement value
   * @param params The replacement parameters
   * @param columnType The Class of the desired return Objects matching the table
   * @return The Column as a List
   * @throws SQLStatementNotFoundException if an SQL statement could not be found for the given sqlKey String
   */
  public static <T> List<T> queryColumnSQLKey(String sqlKey, String columnName, Class<T> columnType, Object[] params) {

    String sql = YANK_POOL_MANAGER.getMergedSqlProperties().getProperty(sqlKey);
    if (sql == null || sql.equalsIgnoreCase("")) {
      throw new SQLStatementNotFoundException();
    } else {
      return queryColumn(sql, columnName, columnType, params);
    }
  }

  /**
   * Return a List of Objects from a single table column given an SQL statement
   *
   * @param <T>
   * @param sql The SQL statement
   * @param params The replacement parameters
   * @param columnType The Class of the desired return Objects matching the table
   * @return The Column as a List
   */
  public static <T> List<T> queryColumn(String sql, String columnName, Class<T> columnType, Object[] params) {

    List<T> returnList = null;

    try {

      ColumnListHandler<T> resultSetHandler = new ColumnListHandler<T>(columnName);

      returnList = new QueryRunner(YANK_POOL_MANAGER.getDataSource()).query(sql, resultSetHandler, params);

    } catch (SQLException e) {
      handleSQLException(e);
    }

    return returnList;
  }

  // ////// OBJECT[] LIST QUERY //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Return a List of generic Object[]s given a SQL Key using an SQL statement matching the sqlKey String in a properties file loaded via
   * Yank.addSQLStatements(...).
   *
   * @param sqlKey The SQL Key found in a properties file corresponding to the desired SQL statement value
   * @param params The replacement parameters
   * @return The List of generic Object[]s
   * @throws SQLStatementNotFoundException if an SQL statement could not be found for the given sqlKey String
   */
  public static List<Object[]> queryObjectArraysSQLKey(String sqlKey, Object[] params) {

    String sql = YANK_POOL_MANAGER.getMergedSqlProperties().getProperty(sqlKey);
    if (sql == null || sql.equalsIgnoreCase("")) {
      throw new SQLStatementNotFoundException();
    } else {
      return queryObjectArrays(sql, params);
    }
  }

  /**
   * Return a List of generic Object[]s given an SQL statement
   *
   * @param sql The SQL statement
   * @param params The replacement parameters
   * @return The List of generic Object[]s
   */
  public static List<Object[]> queryObjectArrays(String sql, Object[] params) {

    List<Object[]> returnList = null;

    try {

      ArrayListHandler resultSetHandler = new ArrayListHandler();
      returnList = new QueryRunner(YANK_POOL_MANAGER.getDataSource()).query(sql, resultSetHandler, params);

    } catch (SQLException e) {
      handleSQLException(e);
    }

    return returnList;
  }

  // ////// BATCH //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Batch executes the given INSERT, UPDATE, DELETE, REPLACE or UPSERT SQL statement matching the sqlKey String in a properties file loaded via
   * Yank.addSQLStatements(...).
   *
   * @param sqlKey The SQL Key found in a properties file corresponding to the desired SQL statement value
   * @param params An array of query replacement parameters. Each row in this array is one set of batch replacement values
   * @return The number of rows affected or each individual execution
   * @throws SQLStatementNotFoundException if an SQL statement could not be found for the given sqlKey String
   */
  public static int[] executeBatchSQLKey(String sqlKey, Object[][] params) {

    String sql = YANK_POOL_MANAGER.getMergedSqlProperties().getProperty(sqlKey);
    if (sql == null || sql.equalsIgnoreCase("")) {
      throw new SQLStatementNotFoundException();
    } else {
      return executeBatch(sql, params);
    }
  }

  /**
   * Batch executes the given INSERT, UPDATE, DELETE, REPLACE or UPSERT SQL statement
   *
   * @param sql The SQL statement
   * @param params An array of query replacement parameters. Each row in this array is one set of batch replacement values
   * @return The number of rows affected or each individual execution
   */
  public static int[] executeBatch(String sql, Object[][] params) {

    int[] returnIntArray = null;

    try {

      returnIntArray = new QueryRunner(YANK_POOL_MANAGER.getDataSource()).batch(sql, params);

    } catch (SQLException e) {
      handleSQLException(e);
    }

    return returnIntArray;
  }

  /**
   * Handles exceptions and logs them
   *
   * @param e the SQLException
   */
  private static void handleSQLException(SQLException e) {

    if (e.getMessage().startsWith("Cannot create")) {
      logger.error(BEAN_EXCEPTION_MESSAGE, e);
    } else {
      logger.error(QUERY_EXCEPTION_MESSAGE, e);
    }
  }

  /**
   * Add properties for a DataSource (connection pool). Yank uses a Hikari DataSource (connection pool) under the hood, so you have to provide the
   * minimal essential properties and the optional properties as defined here: https://github.com/brettwooldridge/HikariCP
   *
   * @param dataSourceProperties
   */
  public static void setupDataSource(Properties dataSourceProperties) {

    YANK_POOL_MANAGER.setupDataSource(dataSourceProperties);
  }

  /**
   * Add SQL statements in a properties file. Adding more will merge Properties.
   *
   * @param sqlProperties
   */
  public static void addSQLStatements(Properties sqlProperties) {

    YANK_POOL_MANAGER.addSQLStatements(sqlProperties);
  }

  /**
   * Closes all open connection pools
   */
  public static synchronized void releaseDataSource() {

    YANK_POOL_MANAGER.releaseDataSource();
  }

  /**
   * Exposes access to the configured DataSource
   *
   * @return a configured (pooled) HikariDataSource.
   */
  public static HikariDataSource getDataSource() {

    return YANK_POOL_MANAGER.getDataSource();
  }
}
