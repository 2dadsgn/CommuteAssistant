package com.commuteassistant.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Double;
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

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class TrafficSnapshotDao_Impl implements TrafficSnapshotDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TrafficSnapshotEntity> __insertionAdapterOfTrafficSnapshotEntity;

  private final SharedSQLiteStatement __preparedStmtOfPruneOlderThan;

  public TrafficSnapshotDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTrafficSnapshotEntity = new EntityInsertionAdapter<TrafficSnapshotEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `traffic_snapshots` (`id`,`routineId`,`capturedAt`,`durationMinutes`,`normalDurationMinutes`,`congestionLevel`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TrafficSnapshotEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getRoutineId());
        statement.bindLong(3, entity.getCapturedAt());
        statement.bindLong(4, entity.getDurationMinutes());
        statement.bindLong(5, entity.getNormalDurationMinutes());
        statement.bindString(6, entity.getCongestionLevel());
      }
    };
    this.__preparedStmtOfPruneOlderThan = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM traffic_snapshots WHERE capturedAt < ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final TrafficSnapshotEntity entity,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfTrafficSnapshotEntity.insertAndReturnId(entity);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object pruneOlderThan(final long before, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfPruneOlderThan.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, before);
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
          __preparedStmtOfPruneOlderThan.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getSnapshots(final long routineId, final long since,
      final Continuation<? super List<TrafficSnapshotEntity>> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM traffic_snapshots\n"
            + "        WHERE routineId = ? AND capturedAt > ?\n"
            + "        ORDER BY capturedAt DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, routineId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, since);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TrafficSnapshotEntity>>() {
      @Override
      @NonNull
      public List<TrafficSnapshotEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfRoutineId = CursorUtil.getColumnIndexOrThrow(_cursor, "routineId");
          final int _cursorIndexOfCapturedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "capturedAt");
          final int _cursorIndexOfDurationMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMinutes");
          final int _cursorIndexOfNormalDurationMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "normalDurationMinutes");
          final int _cursorIndexOfCongestionLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "congestionLevel");
          final List<TrafficSnapshotEntity> _result = new ArrayList<TrafficSnapshotEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TrafficSnapshotEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpRoutineId;
            _tmpRoutineId = _cursor.getLong(_cursorIndexOfRoutineId);
            final long _tmpCapturedAt;
            _tmpCapturedAt = _cursor.getLong(_cursorIndexOfCapturedAt);
            final int _tmpDurationMinutes;
            _tmpDurationMinutes = _cursor.getInt(_cursorIndexOfDurationMinutes);
            final int _tmpNormalDurationMinutes;
            _tmpNormalDurationMinutes = _cursor.getInt(_cursorIndexOfNormalDurationMinutes);
            final String _tmpCongestionLevel;
            _tmpCongestionLevel = _cursor.getString(_cursorIndexOfCongestionLevel);
            _item = new TrafficSnapshotEntity(_tmpId,_tmpRoutineId,_tmpCapturedAt,_tmpDurationMinutes,_tmpNormalDurationMinutes,_tmpCongestionLevel);
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

  @Override
  public Object averageDuration(final long routineId,
      final Continuation<? super Double> $completion) {
    final String _sql = "\n"
            + "        SELECT AVG(durationMinutes) FROM traffic_snapshots\n"
            + "        WHERE routineId = ?\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, routineId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Double>() {
      @Override
      @Nullable
      public Double call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Double _result;
          if (_cursor.moveToFirst()) {
            final Double _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getDouble(0);
            }
            _result = _tmp;
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
