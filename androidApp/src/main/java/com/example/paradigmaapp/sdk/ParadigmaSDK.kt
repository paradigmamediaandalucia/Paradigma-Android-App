package com.example.paradigmaapp.sdk

import com.example.paradigmaapp.api.ktorClient
import com.example.paradigmaapp.repository.Repository
import com.example.paradigmaapp.repository.RepositoryImpl
import com.example.paradigmaapp.cache.AppDatabase // Assuming this will be the Room database

class ParadigmaSDK(appDatabase: AppDatabase) {

    val repository: Repository = RepositoryImpl(ktorClient, appDatabase)
}
