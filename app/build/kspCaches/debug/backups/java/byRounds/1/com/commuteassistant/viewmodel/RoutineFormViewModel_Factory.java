package com.commuteassistant.viewmodel;

import android.content.Context;
import com.commuteassistant.data.ApiKeyProvider;
import com.commuteassistant.data.GoogleMapsApiService;
import com.commuteassistant.data.repository.CommuteRepository;
import com.commuteassistant.notifications.NotificationScheduler;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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

  private final Provider<NotificationScheduler> notificationSchedulerProvider;

  private final Provider<Context> contextProvider;

  public RoutineFormViewModel_Factory(Provider<CommuteRepository> repositoryProvider,
      Provider<GoogleMapsApiService> apiServiceProvider, Provider<ApiKeyProvider> apiKeyProvider,
      Provider<NotificationScheduler> notificationSchedulerProvider,
      Provider<Context> contextProvider) {
    this.repositoryProvider = repositoryProvider;
    this.apiServiceProvider = apiServiceProvider;
    this.apiKeyProvider = apiKeyProvider;
    this.notificationSchedulerProvider = notificationSchedulerProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public RoutineFormViewModel get() {
    return newInstance(repositoryProvider.get(), apiServiceProvider.get(), apiKeyProvider.get(), notificationSchedulerProvider.get(), contextProvider.get());
  }

  public static RoutineFormViewModel_Factory create(Provider<CommuteRepository> repositoryProvider,
      Provider<GoogleMapsApiService> apiServiceProvider, Provider<ApiKeyProvider> apiKeyProvider,
      Provider<NotificationScheduler> notificationSchedulerProvider,
      Provider<Context> contextProvider) {
    return new RoutineFormViewModel_Factory(repositoryProvider, apiServiceProvider, apiKeyProvider, notificationSchedulerProvider, contextProvider);
  }

  public static RoutineFormViewModel newInstance(CommuteRepository repository,
      GoogleMapsApiService apiService, ApiKeyProvider apiKeyProvider,
      NotificationScheduler notificationScheduler, Context context) {
    return new RoutineFormViewModel(repository, apiService, apiKeyProvider, notificationScheduler, context);
  }
}
