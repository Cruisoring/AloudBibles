package com.alouder.bibles.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

	
public abstract class TableMapping
{
	private final String TABLENAME;
	
	public TableMapping(String tableName)
	{
		TABLENAME = tableName;
	}
	
	public void Mapping(SQLiteDatabase database)
	{
		String sql = String.format("SELECT * FROM %s WHERE 0;", TABLENAME);
		Cursor c = database.rawQuery(sql, null);
		getTableIndexes(c);
		c.close();
	}
	
	public abstract void getTableIndexes(Cursor cursor);
}
