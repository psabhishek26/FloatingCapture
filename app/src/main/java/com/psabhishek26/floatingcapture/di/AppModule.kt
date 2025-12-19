package com.psabhishek26.floatingcapture.di

import android.content.Context
import com.psabhishek26.floatingcapture.data.repository.MediaCaptureRepositoryImpl
import com.psabhishek26.floatingcapture.data.repository.SettingsRepositoryImpl
import com.psabhishek26.floatingcapture.data.repository.StorageRepositoryImpl
import com.psabhishek26.floatingcapture.domain.repository.MediaCaptureRepository
import com.psabhishek26.floatingcapture.domain.repository.SettingsRepository
import com.psabhishek26.floatingcapture.domain.repository.StorageRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideStorageRepository(
        @ApplicationContext context: Context
    ): StorageRepository = StorageRepositoryImpl(context)

    @Provides
    @Singleton
    fun provideMediaCaptureRepository(
        @ApplicationContext context: Context,
        storage: StorageRepository
    ): MediaCaptureRepository = MediaCaptureRepositoryImpl(context, storage)

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository = SettingsRepositoryImpl(context)
}