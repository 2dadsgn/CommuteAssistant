package com.commuteassistant.domain.usecase;

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
public final class GetDepartureRecommendationUseCase_Factory implements Factory<GetDepartureRecommendationUseCase> {
  private final Provider<CommuteRepository> repositoryProvider;

  private final Provider<GoogleMapsApiService> apiServiceProvider;

  private final Provider<ApiKeyProvider> apiKeyProvider;

  public GetDepartureRecommendationUseCase_Factory(Provider<CommuteRepository> repositoryProvider,
      Provider<GoogleMapsApiService> apiServiceProvider, Provider<ApiKeyProvider> apiKeyProvider) {
    this.repositoryProvider = repositoryProvider;
    this.apiServiceProvider = apiServiceProvider;
    this.apiKeyProvider = apiKeyProvider;
  }

  @Override
  public GetDepartureRecommendationUseCase get() {
    return newInstance(repositoryProvider.get(), apiServiceProvider.get(), apiKeyProvider.get());
  }

  public static GetDepartureRecommendationUseCase_Factory create(
      Provider<CommuteRepository> repositoryProvider,
      Provider<GoogleMapsApiService> apiServiceProvider, Provider<ApiKeyProvider> apiKeyProvider) {
    return new GetDepartureRecommendationUseCase_Factory(repositoryProvider, apiServiceProvider, apiKeyProvider);
  }

  public static GetDepartureRecommendationUseCase newInstance(CommuteRepository repository,
      GoogleMapsApiService apiService, ApiKeyProvider apiKeyProvider) {
    return new GetDepartureRecommendationUseCase(repository, apiService, apiKeyProvider);
  }
}
