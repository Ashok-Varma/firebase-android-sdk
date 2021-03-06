// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.firebase.firestore.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.google.firebase.firestore.model.DatabaseId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** Tests migrations in SQLiteSchema. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class SQLiteSchemaTest {

  private static final String[] NO_ARGS = new String[0];

  private SQLiteDatabase db;
  private SQLiteSchema schema;

  @Before
  public void setUp() {
    SQLiteOpenHelper opener =
        new SQLiteOpenHelper(RuntimeEnvironment.application, "foo", null, 1) {
          @Override
          public void onCreate(SQLiteDatabase db) {}

          @Override
          public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
        };
    db = opener.getWritableDatabase();
    schema = new SQLiteSchema(db);
  }

  @After
  public void tearDown() {
    if (db != null) {
      db.close();
    }
  }

  @Test
  public void createsMutationsTable() {
    schema.runMigrations();

    assertNoResultsForQuery("SELECT uid, batch_id FROM mutations", NO_ARGS);

    db.execSQL("INSERT INTO mutations (uid, batch_id) VALUES ('foo', 1)");

    Cursor cursor = db.rawQuery("SELECT uid, batch_id FROM mutations", NO_ARGS);
    assertTrue(cursor.moveToFirst());
    assertEquals("foo", cursor.getString(cursor.getColumnIndex("uid")));
    assertEquals(1, cursor.getInt(cursor.getColumnIndex("batch_id")));

    assertFalse(cursor.moveToNext());
    cursor.close();
  }

  @Test
  public void deletesAllTargets() {
    schema.runMigrations(0, 2);

    db.execSQL("INSERT INTO targets (canonical_id, target_id) VALUES ('foo1', 1)");
    db.execSQL("INSERT INTO targets (canonical_id, target_id) VALUES ('foo2', 2)");
    db.execSQL("INSERT INTO target_globals (highest_target_id) VALUES (2)");

    db.execSQL("INSERT INTO target_documents (target_id, path) VALUES (1, 'foo/bar')");
    db.execSQL("INSERT INTO target_documents (target_id, path) VALUES (2, 'foo/baz')");

    schema.runMigrations(2, 3);

    assertNoResultsForQuery("SELECT * FROM targets", NO_ARGS);
    assertNoResultsForQuery("SELECT * FROM target_globals", NO_ARGS);
    assertNoResultsForQuery("SELECT * FROM target_documents", NO_ARGS);
  }

  @Test
  public void countsTargets() {
    schema.runMigrations(0, 3);
    long expected = 50;
    for (int i = 0; i < expected; i++) {
      db.execSQL(
          "INSERT INTO targets (canonical_id, target_id) VALUES (?, ?)",
          new String[] {"foo" + i, "" + i});
    }
    schema.runMigrations(3, 5);
    Cursor c = db.rawQuery("SELECT target_count FROM target_globals LIMIT 1", NO_ARGS);
    assertTrue(c.moveToFirst());
    long targetCount = c.getInt(0);
    assertEquals(expected, targetCount);
  }

  @Test
  public void testDatabaseName() {
    assertEquals(
        "firestore.%5BDEFAULT%5D.my-project.%28default%29",
        SQLitePersistence.databaseName("[DEFAULT]", DatabaseId.forProject("my-project")));
    assertEquals(
        "firestore.%5BDEFAULT%5D.my-project.my-database",
        SQLitePersistence.databaseName(
            "[DEFAULT]", DatabaseId.forDatabase("my-project", "my-database")));
  }

  private void assertNoResultsForQuery(String query, String[] args) {
    Cursor cursor = null;
    try {
      cursor = db.rawQuery(query, args);
      assertFalse(cursor.moveToFirst());
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }
}
