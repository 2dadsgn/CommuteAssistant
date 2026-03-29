package com.commuteassistant.di;

import com.commuteassistant.data.db.CommuteDatabase;
import com.commuteassistant.data.db.CommuteRoutineDao;
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
public final class AppModule_ProvideRoutineDaoFactory implements Factory<CommuteRoutineDao> {
  private final Provider<CommuteDatabase> dbProvider;

  public AppModule_ProvideRoutineDaoFactory(Provider<CommuteDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public CommuteRoutineDao get() {
    return provideRoutineDao(dbProvider.get());
  }

  public static AppModule_ProvideRoutineDaoFactory create(Provider<CommuteDatabase> dbProvider) {
    return new AppModule_ProvideRoutineDaoFactory(dbProvider);
  }

  public static CommuteRoutineDao provideRoutineDao(CommuteDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideRoutineDao(db));
  }
}
