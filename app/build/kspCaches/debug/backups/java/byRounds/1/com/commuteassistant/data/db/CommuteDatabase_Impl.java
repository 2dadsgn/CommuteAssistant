package com.commuteassistant.data.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class CommuteDatabase_Impl extends CommuteDatabase {
  private volatile CommuteRoutineDao _commuteRoutineDao;

  private volatile TrafficSnapshotDao _trafficSnapshotDao;

  private volatile SavedPlaceDao _savedPlaceDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(3) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `commute_routines` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `dayOfWeek` INTEGER NOT NULL, `usualDepartureHour` INTEGER NOT NULL, `usualDepartureMinute` INTEGER NOT NULL, `originLat` REAL NOT NULL, `originLng` REAL NOT NULL, `originName` TEXT NOT NULL, `destinationLat` REAL NOT NULL, `destinationLng` REAL NOT NULL, `destinationName` TEXT NOT NULL, `isActive` INTEGER NOT NULL, `isNotificationEnabled` INTEGER NOT NULL, `isPriorityAlert` INTEGER NOT NULL, `notificationOffsetMins` INTEGER NOT NULL, `notificationCount` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `traffic_snapshots` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `routineId` INTEGER NOT NULL, `capturedAt` INTEGER NOT NULL, `durationMinutes` INTEGER NOT NULL, `normalDurationMinutes` INTEGER NOT NULL, `congestionLevel` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `saved_places` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `address` TEXT NOT NULL, `lat` REAL NOT NULL, `lng` REAL NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '627f06f2a339be445d6775fc3807ee59')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `commute_routines`");
        db.execSQL("DROP TABLE IF EXISTS `traffic_snapshots`");
        db.execSQL("DROP TABLE IF EXISTS `saved_places`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsCommuteRoutines = new HashMap<String, TableInfo.Column>(15);
        _columnsCommuteRoutines.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommuteRoutines.put("dayOfWeek", new TableInfo.Column("dayOfWeek", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommuteRoutines.put("usualDepartureHour", new TableInfo.Column("usualDepartureHour", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommuteRoutines.put("usualDepartureMinute", new TableInfo.Column("usualDepartureMinute", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommuteRoutines.put("originLat", new TableInfo.Column("originLat", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommuteRoutines.put("originLng", new TableInfo.Column("originLng", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommuteRoutines.put("originName", new TableInfo.Column("originName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommuteRoutines.put("destinationLat", new TableInfo.Column("destinationLat", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommuteRoutines.put("destinationLng", new TableInfo.Column("destinationLng", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommuteRoutines.put("destinationName", new TableInfo.Column("destinationName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommuteRoutines.put("isActive", new TableInfo.Column("isActive", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommuteRoutines.put("isNotificationEnabled", new TableInfo.Column("isNotificationEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommuteRoutines.put("isPriorityAlert", new TableInfo.Column("isPriorityAlert", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommuteRoutines.put("notificationOffsetMins", new TableInfo.Column("notificationOffsetMins", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommuteRoutines.put("notificationCount", new TableInfo.Column("notificationCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCommuteRoutines = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCommuteRoutines = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCommuteRoutines = new TableInfo("commute_routines", _columnsCommuteRoutines, _foreignKeysCommuteRoutines, _indicesCommuteRoutines);
        final TableInfo _existingCommuteRoutines = TableInfo.read(db, "commute_routines");
        if (!_infoCommuteRoutines.equals(_existingCommuteRoutines)) {
          return new RoomOpenHelper.ValidationResult(false, "commute_routines(com.commuteassistant.data.db.CommuteRoutineEntity).\n"
                  + " Expected:\n" + _infoCommuteRoutines + "\n"
                  + " Found:\n" + _existingCommuteRoutines);
        }
        final HashMap<String, TableInfo.Column> _columnsTrafficSnapshots = new HashMap<String, TableInfo.Column>(6);
        _columnsTrafficSnapshots.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrafficSnapshots.put("routineId", new TableInfo.Column("routineId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrafficSnapshots.put("capturedAt", new TableInfo.Column("capturedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrafficSnapshots.put("durationMinutes", new TableInfo.Column("durationMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrafficSnapshots.put("normalDurationMinutes", new TableInfo.Column("normalDurationMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTrafficSnapshots.put("congestionLevel", new TableInfo.Column("congestionLevel", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTrafficSnapshots = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTrafficSnapshots = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTrafficSnapshots = new TableInfo("traffic_snapshots", _columnsTrafficSnapshots, _foreignKeysTrafficSnapshots, _indicesTrafficSnapshots);
        final TableInfo _existingTrafficSnapshots = TableInfo.read(db, "traffic_snapshots");
        if (!_infoTrafficSnapshots.equals(_existingTrafficSnapshots)) {
          return new RoomOpenHelper.ValidationResult(false, "traffic_snapshots(com.commuteassistant.data.db.TrafficSnapshotEntity).\n"
                  + " Expected:\n" + _infoTrafficSnapshots + "\n"
                  + " Found:\n" + _existingTrafficSnapshots);
        }
        final HashMap<String, TableInfo.Column> _columnsSavedPlaces = new HashMap<String, TableInfo.Column>(5);
        _columnsSavedPlaces.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavedPlaces.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavedPlaces.put("address", new TableInfo.Column("address", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavedPlaces.put("lat", new TableInfo.Column("lat", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavedPlaces.put("lng", new TableInfo.Column("lng", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSavedPlaces = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSavedPlaces = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSavedPlaces = new TableInfo("saved_places", _columnsSavedPlaces, _foreignKeysSavedPlaces, _indicesSavedPlaces);
        final TableInfo _existingSavedPlaces = TableInfo.read(db, "saved_places");
        if (!_infoSavedPlaces.equals(_existingSavedPlaces)) {
          return new RoomOpenHelper.ValidationResult(false, "saved_places(com.commuteassistant.data.db.SavedPlaceEntity).\n"
                  + " Expected:\n" + _infoSavedPlaces + "\n"
                  + " Found:\n" + _existingSavedPlaces);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "627f06f2a339be445d6775fc3807ee59", "c0f5ec0275e212eeb9afa37f330cfba8");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "commute_routines","traffic_snapshots","saved_places");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `commute_routines`");
      _db.execSQL("DELETE FROM `traffic_snapshots`");
      _db.execSQL("DELETE FROM `saved_places`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(CommuteRoutineDao.class, CommuteRoutineDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(TrafficSnapshotDao.class, TrafficSnapshotDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SavedPlaceDao.class, SavedPlaceDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public CommuteRoutineDao routineDao() {
    if (_commuteRoutineDao != null) {
      return _commuteRoutineDao;
    } else {
      synchronized(this) {
        if(_commuteRoutineDao == null) {
          _commuteRoutineDao = new CommuteRoutineDao_Impl(this);
        }
        return _commuteRoutineDao;
      }
    }
  }

  @Override
  public TrafficSnapshotDao snapshotDao() {
    if (_trafficSnapshotDao != null) {
      return _trafficSnapshotDao;
    } else {
      synchronized(this) {
        if(_trafficSnapshotDao == null) {
          _trafficSnapshotDao = new TrafficSnapshotDao_Impl(this);
        }
        return _trafficSnapshotDao;
      }
    }
  }

  @Override
  public SavedPlaceDao savedPlaceDao() {
    if (_savedPlaceDao != null) {
      return _savedPlaceDao;
    } else {
      synchronized(this) {
        if(_savedPlaceDao == null) {
          _savedPlaceDao = new SavedPlaceDao_Impl(this);
        }
        return _savedPlaceDao;
      }
    }
  }
}
