package com.commuteassistant.viewmodel;

import com.commuteassistant.data.repository.CommuteRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class RoutineFormViewModel_Factory implements Factory<RoutineFormViewModel> {
  private final Provider<CommuteRepository> repositoryProvider;

  public RoutineFormViewModel_Factory(Provider<CommuteRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public RoutineFormViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static RoutineFormViewModel_Factory create(
      Provider<CommuteRepository> repositoryProvider) {
    return new RoutineFormViewModel_Factory(repositoryProvider);
  }

  public static RoutineFormViewModel newInstance(CommuteRepository repository) {
    return new RoutineFormViewModel(repository);
  }
}
