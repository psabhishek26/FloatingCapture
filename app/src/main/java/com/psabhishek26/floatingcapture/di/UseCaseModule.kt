package com.psabhishek26.floatingcapture.di

import com.psabhishek26.floatingcapture.domain.repository.MediaCaptureRepository
import com.psabhishek26.floatingcapture.domain.usecase.StartRecordingUseCase
import com.psabhishek26.floatingcapture.domain.usecase.StopRecordingUseCase
import com.psabhishek26.floatingcapture.domain.usecase.TakeScreenshotUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideTakeScreenshotUseCase(
        repository: MediaCaptureRepository
    ): TakeScreenshotUseCase = TakeScreenshotUseCase(repository)

    @Provides
    @Singleton
    fun provideStartRecordingUseCase(
        repository: MediaCaptureRepository
    ): StartRecordingUseCase = StartRecordingUseCase(repository)

    @Provides
    @Singleton
    fun provideStopRecordingUseCase(
        repository: MediaCaptureRepository
    ): StopRecordingUseCase = StopRecordingUseCase(repository)
}