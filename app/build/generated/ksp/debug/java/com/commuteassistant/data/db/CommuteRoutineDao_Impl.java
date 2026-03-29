package com.commuteassistant.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class CommuteRoutineDao_Impl implements CommuteRoutineDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CommuteRoutineEntity> __insertionAdapterOfCommuteRoutineEntity;

  private final EntityDeletionOrUpdateAdapter<CommuteRoutineEntity> __deletionAdapterOfCommuteRoutineEntity;

  private final SharedSQLiteStatement __preparedStmtOfSetActive;

  public CommuteRoutineDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCommuteRoutineEntity = new EntityInsertionAdapter<CommuteRoutineEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `commute_routines` (`id`,`dayOfWeek`,`usualDepartureHour`,`usualDepartureMinute`,`originLat`,`originLng`,`originName`,`destinationLat`,`destinationLng`,`destinationName`,`isActive`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CommuteRoutineEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getDayOfWeek());
        statement.bindLong(3, entity.getUsualDepartureHour());
        statement.bindLong(4, entity.getUsualDepartureMinute());
        statement.bindDouble(5, entity.getOriginLat());
        statement.bindDouble(6, entity.getOriginLng());
        statement.bindString(7, entity.getOriginName());
        statement.bindDouble(8, entity.getDestinationLat());
        statement.bindDouble(9, entity.getDestinationLng());
        statement.bindString(10, entity.getDestinationName());
        final int _tmp = entity.isActive() ? 1 : 0;
        statement.bindLong(11, _tmp);
      }
    };
    this.__deletionAdapterOfCommuteRoutineEntity = new EntityDeletionOrUpdateAdapter<CommuteRoutineEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `commute_routines` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CommuteRoutineEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__preparedStmtOfSetActive = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE commute_routines SET isActive = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object upsert(final CommuteRoutineEntity entity,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfCommuteRoutineEntity.insertAndReturnId(entity);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final CommuteRoutineEntity entity,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfCommuteRoutineEntity.handle(entity);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object setActive(final long id, final boolean active,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetActive.acquire();
        int _argIndex = 1;
        final int _tmp = active ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfSetActive.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CommuteRoutineEntity>> observeActiveRoutines() {
    final String _sql = "SELECT * FROM commute_routines WHERE isActive = 1 ORDER BY dayOfWeek, usualDepartureHour";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"commute_routines"}, new Callable<List<CommuteRoutineEntity>>() {
      @Override
      @NonNull
      public List<CommuteRoutineEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDayOfWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "dayOfWeek");
          final int _cursorIndexOfUsualDepartureHour = CursorUtil.getColumnIndexOrThrow(_cursor, "usualDepartureHour");
          final int _cursorIndexOfUsualDepartureMinute = CursorUtil.getColumnIndexOrThrow(_cursor, "usualDepartureMinute");
          final int _cursorIndexOfOriginLat = CursorUtil.getColumnIndexOrThrow(_cursor, "originLat");
          final int _cursorIndexOfOriginLng = CursorUtil.getColumnIndexOrThrow(_cursor, "originLng");
          final int _cursorIndexOfOriginName = CursorUtil.getColumnIndexOrThrow(_cursor, "originName");
          final int _cursorIndexOfDestinationLat = CursorUtil.getColumnIndexOrThrow(_cursor, "destinationLat");
          final int _cursorIndexOfDestinationLng = CursorUtil.getColumnIndexOrThrow(_cursor, "destinationLng");
          final int _cursorIndexOfDestinationName = CursorUtil.getColumnIndexOrThrow(_cursor, "destinationName");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final List<CommuteRoutineEntity> _result = new ArrayList<CommuteRoutineEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CommuteRoutineEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final int _tmpDayOfWeek;
            _tmpDayOfWeek = _cursor.getInt(_cursorIndexOfDayOfWeek);
            final int _tmpUsualDepartureHour;
            _tmpUsualDepartureHour = _cursor.getInt(_cursorIndexOfUsualDepartureHour);
            final int _tmpUsualDepartureMinute;
            _tmpUsualDepartureMinute = _cursor.getInt(_cursorIndexOfUsualDepartureMinute);
            final double _tmpOriginLat;
            _tmpOriginLat = _cursor.getDouble(_cursorIndexOfOriginLat);
            final double _tmpOriginLng;
            _tmpOriginLng = _cursor.getDouble(_cursorIndexOfOriginLng);
            final String _tmpOriginName;
            _tmpOriginName = _cursor.getString(_cursorIndexOfOriginName);
            final double _tmpDestinationLat;
            _tmpDestinationLat = _cursor.getDouble(_cursorIndexOfDestinationLat);
            final double _tmpDestinationLng;
            _tmpDestinationLng = _cursor.getDouble(_cursorIndexOfDestinationLng);
            final String _tmpDestinationName;
            _tmpDestinationName = _cursor.getString(_cursorIndexOfDestinationName);
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            _item = new CommuteRoutineEntity(_tmpId,_tmpDayOfWeek,_tmpUsualDepartureHour,_tmpUsualDepartureMinute,_tmpOriginLat,_tmpOriginLng,_tmpOriginName,_tmpDestinationLat,_tmpDestinationLng,_tmpDestinationName,_tmpIsActive);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getById(final long id,
      final Continuation<? super CommuteRoutineEntity> $completion) {
    final String _sql = "SELECT * FROM commute_routines WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CommuteRoutineEntity>() {
      @Override
      @Nullable
      public CommuteRoutineEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDayOfWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "dayOfWeek");
          final int _cursorIndexOfUsualDepartureHour = CursorUtil.getColumnIndexOrThrow(_cursor, "usualDepartureHour");
          final int _cursorIndexOfUsualDepartureMinute = CursorUtil.getColumnIndexOrThrow(_cursor, "usualDepartureMinute");
          final int _cursorIndexOfOriginLat = CursorUtil.getColumnIndexOrThrow(_cursor, "originLat");
          final int _cursorIndexOfOriginLng = CursorUtil.getColumnIndexOrThrow(_cursor, "originLng");
          final int _cursorIndexOfOriginName = CursorUtil.getColumnIndexOrThrow(_cursor, "originName");
          final int _cursorIndexOfDestinationLat = CursorUtil.getColumnIndexOrThrow(_cursor, "destinationLat");
          final int _cursorIndexOfDestinationLng = CursorUtil.getColumnIndexOrThrow(_cursor, "destinationLng");
          final int _cursorIndexOfDestinationName = CursorUtil.getColumnIndexOrThrow(_cursor, "destinationName");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final CommuteRoutineEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final int _tmpDayOfWeek;
            _tmpDayOfWeek = _cursor.getInt(_cursorIndexOfDayOfWeek);
            final int _tmpUsualDepartureHour;
            _tmpUsualDepartureHour = _cursor.getInt(_cursorIndexOfUsualDepartureHour);
            final int _tmpUsualDepartureMinute;
            _tmpUsualDepartureMinute = _cursor.getInt(_cursorIndexOfUsualDepartureMinute);
            final double _tmpOriginLat;
            _tmpOriginLat = _cursor.getDouble(_cursorIndexOfOriginLat);
            final double _tmpOriginLng;
            _tmpOriginLng = _cursor.getDouble(_cursorIndexOfOriginLng);
            final String _tmpOriginName;
            _tmpOriginName = _cursor.getString(_cursorIndexOfOriginName);
            final double _tmpDestinationLat;
            _tmpDestinationLat = _cursor.getDouble(_cursorIndexOfDestinationLat);
            final double _tmpDestinationLng;
            _tmpDestinationLng = _cursor.getDouble(_cursorIndexOfDestinationLng);
            final String _tmpDestinationName;
            _tmpDestinationName = _cursor.getString(_cursorIndexOfDestinationName);
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            _result = new CommuteRoutineEntity(_tmpId,_tmpDayOfWeek,_tmpUsualDepartureHour,_tmpUsualDepartureMinute,_tmpOriginLat,_tmpOriginLng,_tmpOriginName,_tmpDestinationLat,_tmpDestinationLng,_tmpDestinationName,_tmpIsActive);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getByDay(final int dayValue,
      final Continuation<? super List<CommuteRoutineEntity>> $completion) {
    final String _sql = "SELECT * FROM commute_routines WHERE dayOfWeek = ? AND isActive = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, dayValue);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CommuteRoutineEntity>>() {
      @Override
      @NonNull
      public List<CommuteRoutineEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDayOfWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "dayOfWeek");
          final int _cursorIndexOfUsualDepartureHour = CursorUtil.getColumnIndexOrThrow(_cursor, "usualDepartureHour");
          final int _cursorIndexOfUsualDepartureMinute = CursorUtil.getColumnIndexOrThrow(_cursor, "usualDepartureMinute");
          final int _cursorIndexOfOriginLat = CursorUtil.getColumnIndexOrThrow(_cursor, "originLat");
          final int _cursorIndexOfOriginLng = CursorUtil.getColumnIndexOrThrow(_cursor, "originLng");
          final int _cursorIndexOfOriginName = CursorUtil.getColumnIndexOrThrow(_cursor, "originName");
          final int _cursorIndexOfDestinationLat = CursorUtil.getColumnIndexOrThrow(_cursor, "destinationLat");
          final int _cursorIndexOfDestinationLng = CursorUtil.getColumnIndexOrThrow(_cursor, "destinationLng");
          final int _cursorIndexOfDestinationName = CursorUtil.getColumnIndexOrThrow(_cursor, "destinationName");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final List<CommuteRoutineEntity> _result = new ArrayList<CommuteRoutineEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CommuteRoutineEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final int _tmpDayOfWeek;
            _tmpDayOfWeek = _cursor.getInt(_cursorIndexOfDayOfWeek);
            final int _tmpUsualDepartureHour;
            _tmpUsualDepartureHour = _cursor.getInt(_cursorIndexOfUsualDepartureHour);
            final int _tmpUsualDepartureMinute;
            _tmpUsualDepartureMinute = _cursor.getInt(_cursorIndexOfUsualDepartureMinute);
            final double _tmpOriginLat;
            _tmpOriginLat = _cursor.getDouble(_cursorIndexOfOriginLat);
            final double _tmpOriginLng;
            _tmpOriginLng = _cursor.getDouble(_cursorIndexOfOriginLng);
            final String _tmpOriginName;
            _tmpOriginName = _cursor.getString(_cursorIndexOfOriginName);
            final double _tmpDestinationLat;
            _tmpDestinationLat = _cursor.getDouble(_cursorIndexOfDestinationLat);
            final double _tmpDestinationLng;
            _tmpDestinationLng = _cursor.getDouble(_cursorIndexOfDestinationLng);
            final String _tmpDestinationName;
            _tmpDestinationName = _cursor.getString(_cursorIndexOfDestinationName);
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            _item = new CommuteRoutineEntity(_tmpId,_tmpDayOfWeek,_tmpUsualDepartureHour,_tmpUsualDepartureMinute,_tmpOriginLat,_tmpOriginLng,_tmpOriginName,_tmpDestinationLat,_tmpDestinationLng,_tmpDestinationName,_tmpIsActive);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
