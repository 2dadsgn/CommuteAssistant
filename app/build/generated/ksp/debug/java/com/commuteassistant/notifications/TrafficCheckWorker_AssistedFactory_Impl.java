package com.commuteassistant.notifications;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.InstanceFactory;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class TrafficCheckWorker_AssistedFactory_Impl implements TrafficCheckWorker_AssistedFactory {
  private final TrafficCheckWorker_Factory delegateFactory;

  TrafficCheckWorker_AssistedFactory_Impl(TrafficCheckWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public TrafficCheckWorker create(Context p0, WorkerParameters p1) {
    return delegateFactory.get(p0, p1);
  }

  public static Provider<TrafficCheckWorker_AssistedFactory> create(
      TrafficCheckWorker_Factory delegateFactory) {
    return InstanceFactory.create(new TrafficCheckWorker_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<TrafficCheckWorker_AssistedFactory> createFactoryProvider(
      TrafficCheckWorker_Factory delegateFactory) {
    return InstanceFactory.create(new TrafficCheckWorker_AssistedFactory_Impl(delegateFactory));
  }
}
