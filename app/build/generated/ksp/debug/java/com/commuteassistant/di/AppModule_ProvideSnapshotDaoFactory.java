package com.commuteassistant.di;

import com.commuteassistant.data.db.CommuteDatabase;
import com.commuteassistant.data.db.TrafficSnapshotDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class AppModule_ProvideSnapshotDaoFactory implements Factory<TrafficSnapshotDao> {
  private final Provider<CommuteDatabase> dbProvider;

  public AppModule_ProvideSnapshotDaoFactory(Provider<CommuteDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public TrafficSnapshotDao get() {
    return provideSnapshotDao(dbProvider.get());
  }

  public static AppModule_ProvideSnapshotDaoFactory create(Provider<CommuteDatabase> dbProvider) {
    return new AppModule_ProvideSnapshotDaoFactory(dbProvider);
  }

  public static TrafficSnapshotDao provideSnapshotDao(CommuteDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideSnapshotDao(db));
  }
}
