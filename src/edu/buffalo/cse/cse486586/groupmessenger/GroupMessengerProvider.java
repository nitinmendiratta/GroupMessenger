package edu.buffalo.cse.cse486586.groupmessenger;

import java.io.*;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class GroupMessengerProvider extends ContentProvider {

	private GroupMessengerDataBase gmessengerDB;
	private static final String TABLE_NAME = "provider";
	private static final String COLUMN_KEY = "key";

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// You do not need to implement this.
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// You do not need to implement this.
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) 
	{
		SQLiteDatabase sqlDataBase = gmessengerDB.getWritableDatabase();
		sqlDataBase.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
		return uri;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) 
	{
		SQLiteQueryBuilder buildQuery = new SQLiteQueryBuilder();
		buildQuery.setTables(TABLE_NAME);
		SQLiteDatabase dataBase = gmessengerDB.getReadableDatabase();
		Cursor cr = buildQuery.query(dataBase, null, TABLE_NAME+"."+COLUMN_KEY+"='"+selection+"'", null, null, null, null);
		cr.setNotificationUri(getContext().getContentResolver(), uri);

		return cr;
	}


	@Override
	public boolean onCreate() {
		// If you need to perform any one-time initialization task, please do it here.
		gmessengerDB = new GroupMessengerDataBase(getContext());
		return false;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		// You do not need to implement this.
		return 0;
	}
}
