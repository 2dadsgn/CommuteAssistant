package com.commuteassistant.viewmodel;

import com.commuteassistant.data.ApiKeyProvider;
import com.commuteassistant.data.GoogleMapsApiService;
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

  private final Provider<GoogleMapsApiService> apiServiceProvider;

  private final Provider<ApiKeyProvider> apiKeyProvider;

  public RoutineFormViewModel_Factory(Provider<CommuteRepository> repositoryProvider,
      Provider<GoogleMapsApiService> apiServiceProvider, Provider<ApiKeyProvider> apiKeyProvider) {
    this.repositoryProvider = repositoryProvider;
    this.apiServiceProvider = apiServiceProvider;
    this.apiKeyProvider = apiKeyProvider;
  }

  @Override
  public RoutineFormViewModel get() {
    return newInstance(repositoryProvider.get(), apiServiceProvider.get(), apiKeyProvider.get());
  }

  public static RoutineFormViewModel_Factory create(Provider<CommuteRepository> repositoryProvider,
      Provider<GoogleMapsApiService> apiServiceProvider, Provider<ApiKeyProvider> apiKeyProvider) {
    return new RoutineFormViewModel_Factory(repositoryProvider, apiServiceProvider, apiKeyProvider);
  }

  public static RoutineFormViewModel newInstance(CommuteRepository repository,
      GoogleMapsApiService apiService, ApiKeyProvider apiKeyProvider) {
    return new RoutineFormViewModel(repository, apiService, apiKeyProvider);
  }
}
