package com.commuteassistant.notifications;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.commuteassistant.data.repository.CommuteRepository;
import com.commuteassistant.domain.usecase.GetDepartureRecommendationUseCase;
import dagger.internal.DaggerGenerated;
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
public final class TrafficCheckWorker_Factory {
  private final Provider<CommuteRepository> repositoryProvider;

  private final Provider<GetDepartureRecommendationUseCase> recommendationUseCaseProvider;

  public TrafficCheckWorker_Factory(Provider<CommuteRepository> repositoryProvider,
      Provider<GetDepartureRecommendationUseCase> recommendationUseCaseProvider) {
    this.repositoryProvider = repositoryProvider;
    this.recommendationUseCaseProvider = recommendationUseCaseProvider;
  }

  public TrafficCheckWorker get(Context context, WorkerParameters workerParams) {
    return newInstance(context, workerParams, repositoryProvider.get(), recommendationUseCaseProvider.get());
  }

  public static TrafficCheckWorker_Factory create(Provider<CommuteRepository> repositoryProvider,
      Provider<GetDepartureRecommendationUseCase> recommendationUseCaseProvider) {
    return new TrafficCheckWorker_Factory(repositoryProvider, recommendationUseCaseProvider);
  }

  public static TrafficCheckWorker newInstance(Context context, WorkerParameters workerParams,
      CommuteRepository repository, GetDepartureRecommendationUseCase recommendationUseCase) {
    return new TrafficCheckWorker(context, workerParams, repository, recommendationUseCase);
  }
}
