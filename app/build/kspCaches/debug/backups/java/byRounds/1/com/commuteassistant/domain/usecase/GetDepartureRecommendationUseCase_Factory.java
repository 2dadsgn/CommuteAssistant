package com.commuteassistant.domain.usecase;

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

  public GetDepartureRecommendationUseCase_Factory(Provider<CommuteRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public GetDepartureRecommendationUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static GetDepartureRecommendationUseCase_Factory create(
      Provider<CommuteRepository> repositoryProvider) {
    return new GetDepartureRecommendationUseCase_Factory(repositoryProvider);
  }

  public static GetDepartureRecommendationUseCase newInstance(CommuteRepository repository) {
    return new GetDepartureRecommendationUseCase(repository);
  }
}
