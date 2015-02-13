/**
 * Copyright 2011 - 2015 Xeiam LLC.
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
package com.xeiam.yank.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.xeiam.yank.YankPoolManager;
import com.xeiam.yank.PropertiesUtils;

/**
 * Inserts a Batch of Book Objects into the BOOKS table.
 * 
 * @author timmolter
 */
public class InsertBatch {

  public static void main(String[] args) {

    // DB Properties
    Properties props = PropertiesUtils.getPropertiesFromClasspath("MYSQL_DB.properties");

    // init DB Connection Manager
    YankPoolManager.INSTANCE.init(props);

    // query
    List<Book> books = new ArrayList<Book>();

    Book book = new Book();
    book.setTitle("Cryptonomicon");
    book.setAuthor("Neal Stephenson");
    book.setPrice(23.99);
    books.add(book);

    book = new Book();
    book.setTitle("Harry Potter");
    book.setAuthor("Joanne K. Rowling");
    book.setPrice(11.99);
    books.add(book);

    book = new Book();
    book.setTitle("Don Quijote");
    book.setAuthor("Cervantes");
    book.setPrice(21.99);
    books.add(book);

    BooksDAO.insertBatch(books);

    // shutodwn DB Connection Manager
    YankPoolManager.INSTANCE.release();

  }
}
