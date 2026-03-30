package com.commuteassistant.viewmodel;

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

  public HomeViewModel_Factory(Provider<CommuteRepository> repositoryProvider,
      Provider<GetDepartureRecommendationUseCase> recommendationUseCaseProvider) {
    this.repositoryProvider = repositoryProvider;
    this.recommendationUseCaseProvider = recommendationUseCaseProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(repositoryProvider.get(), recommendationUseCaseProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<CommuteRepository> repositoryProvider,
      Provider<GetDepartureRecommendationUseCase> recommendationUseCaseProvider) {
    return new HomeViewModel_Factory(repositoryProvider, recommendationUseCaseProvider);
  }

  public static HomeViewModel newInstance(CommuteRepository repository,
      GetDepartureRecommendationUseCase recommendationUseCase) {
    return new HomeViewModel(repository, recommendationUseCase);
  }
}
