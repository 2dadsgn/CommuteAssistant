package com.commuteassistant.data.repository;

import com.commuteassistant.data.db.CommuteRoutineDao;
import com.commuteassistant.data.db.SavedPlaceDao;
import com.commuteassistant.data.db.TrafficSnapshotDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class CommuteRepository_Factory implements Factory<CommuteRepository> {
  private final Provider<CommuteRoutineDao> routineDaoProvider;

  private final Provider<TrafficSnapshotDao> snapshotDaoProvider;

  private final Provider<SavedPlaceDao> savedPlaceDaoProvider;

  public CommuteRepository_Factory(Provider<CommuteRoutineDao> routineDaoProvider,
      Provider<TrafficSnapshotDao> snapshotDaoProvider,
      Provider<SavedPlaceDao> savedPlaceDaoProvider) {
    this.routineDaoProvider = routineDaoProvider;
    this.snapshotDaoProvider = snapshotDaoProvider;
    this.savedPlaceDaoProvider = savedPlaceDaoProvider;
  }

  @Override
  public CommuteRepository get() {
    return newInstance(routineDaoProvider.get(), snapshotDaoProvider.get(), savedPlaceDaoProvider.get());
  }

  public static CommuteRepository_Factory create(Provider<CommuteRoutineDao> routineDaoProvider,
      Provider<TrafficSnapshotDao> snapshotDaoProvider,
      Provider<SavedPlaceDao> savedPlaceDaoProvider) {
    return new CommuteRepository_Factory(routineDaoProvider, snapshotDaoProvider, savedPlaceDaoProvider);
  }

  public static CommuteRepository newInstance(CommuteRoutineDao routineDao,
      TrafficSnapshotDao snapshotDao, SavedPlaceDao savedPlaceDao) {
    return new CommuteRepository(routineDao, snapshotDao, savedPlaceDao);
  }
}
