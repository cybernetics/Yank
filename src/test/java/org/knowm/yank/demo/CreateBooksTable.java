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
package org.knowm.yank.demo;

import java.util.Properties;

import org.knowm.yank.PropertiesUtils;
import org.knowm.yank.Yank;

/**
 * Create a table called BOOKS. <br>
 * Demonstrates hardcoding the connection pool properties rather then getting them from a Properties file. <br>
 * Note: myconnectionpoolname can be anything but it needs to match the first String argument in DBProxy.* method calls. See BooksDAO.java. <br>
 *
 * @author timmolter
 */
public class CreateBooksTable {

  public static void main(String[] args) {

    // DB Properties
    Properties dbProps = new Properties();
    dbProps.setProperty("jdbcUrl", "jdbc:mysql://localhost:3306/Yank");
    dbProps.setProperty("username", "root");
    dbProps.setProperty("password", "");
    dbProps.setProperty("maximumPoolSize", "5");

    // SQL Statements in Properties file
    Properties sqlProps = PropertiesUtils.getPropertiesFromClasspath("MYSQL_SQL.properties");

    Yank.setupDataSource(dbProps);
    Yank.addSQLStatements(sqlProps);

    // create table
    BooksDAO.createBooksTable();

    Yank.releaseDataSource();
  }
}
