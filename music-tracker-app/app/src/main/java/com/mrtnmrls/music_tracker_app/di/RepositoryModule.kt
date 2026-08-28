package com.mrtnmrls.music_tracker_app.di

import com.mrtnmrls.music_tracker_app.data.repository.PlayRepositoryImpl
import com.mrtnmrls.music_tracker_app.domain.repository.PlayRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindPlayRepository(impl: PlayRepositoryImpl): PlayRepository
}