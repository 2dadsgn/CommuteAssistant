package com.commuteassistant.viewmodel;

import com.commuteassistant.data.ApiKeyProvider;
import com.commuteassistant.data.GoogleMapsApiService;
import com.commuteassistant.data.repository.CommuteRepository;
import com.commuteassistant.domain.usecase.GetDepartureRecommendationUseCase;
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<CommuteRepository> repositoryProvider;

  private final Provider<GetDepartureRecommendationUseCase> recommendationUseCaseProvider;

  private final Provider<GoogleMapsApiService> apiServiceProvider;

  private final Provider<ApiKeyProvider> apiKeyProvider;

  public HomeViewModel_Factory(Provider<CommuteRepository> repositoryProvider,
      Provider<GetDepartureRecommendationUseCase> recommendationUseCaseProvider,
      Provider<GoogleMapsApiService> apiServiceProvider, Provider<ApiKeyProvider> apiKeyProvider) {
    this.repositoryProvider = repositoryProvider;
    this.recommendationUseCaseProvider = recommendationUseCaseProvider;
    this.apiServiceProvider = apiServiceProvider;
    this.apiKeyProvider = apiKeyProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(repositoryProvider.get(), recommendationUseCaseProvider.get(), apiServiceProvider.get(), apiKeyProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<CommuteRepository> repositoryProvider,
      Provider<GetDepartureRecommendationUseCase> recommendationUseCaseProvider,
      Provider<GoogleMapsApiService> apiServiceProvider, Provider<ApiKeyProvider> apiKeyProvider) {
    return new HomeViewModel_Factory(repositoryProvider, recommendationUseCaseProvider, apiServiceProvider, apiKeyProvider);
  }

  public static HomeViewModel newInstance(CommuteRepository repository,
      GetDepartureRecommendationUseCase recommendationUseCase, GoogleMapsApiService apiService,
      ApiKeyProvider apiKeyProvider) {
    return new HomeViewModel(repository, recommendationUseCase, apiService, apiKeyProvider);
  }
}
