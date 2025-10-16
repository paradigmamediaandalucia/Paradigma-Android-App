package com.example.paradigmaapp.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.paradigmaapp.android.api.AndainaStream
import com.example.paradigmaapp.android.data.AppPreferences
import com.example.paradigmaapp.sdk.ParadigmaSDK
import com.example.paradigmaapp.repository.Repository

/**
 * Factoría personalizada para la creación de ViewModels.
 * Inyecta todas las dependencias necesarias a cada ViewModel.
 *
 * @param appPreferences Instancia para la gestión de preferencias.
 * @param paradigmaSDK Instancia del SDK principal que contiene el repositorio.
 * @param applicationContext El Context de la aplicación.
 * @param remoteConfigService El servicio para obtener URLs de forma remota.
 * @param andainaStream El servicio para gestionar la radio en directo.
 */
class ViewModelFactory(
    private val appPreferences: AppPreferences,
    private val paradigmaSDK: ParadigmaSDK,
    private val applicationContext: Context,
    private val andainaStream: AndainaStream,
    private val volumeControlViewModel: VolumeControlViewModel
) : ViewModelProvider.Factory {

    private val repository: Repository = paradigmaSDK.repository

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val savedStateHandle = extras.createSavedStateHandle()

        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                // Creamos los ViewModels de los que depende MainViewModel.
                val queueVM = QueueViewModel(appPreferences, repository)
                val onGoingVM = OnGoingEpisodeViewModel(appPreferences, repository)
                val downloadedVM = DownloadedEpisodeViewModel(appPreferences, repository, applicationContext)

                MainViewModel(
                    repository = repository,
                    appPreferences = appPreferences,
                    context = applicationContext,
                    queueViewModel = queueVM,
                    onGoingViewModel = onGoingVM,
                    downloadedViewModel = downloadedVM,
                    andainaStreamPlayer = andainaStream,
                    volumeControlViewModel = volumeControlViewModel
                ) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(appPreferences) as T
            }
            // El resto de ViewModels se mantienen igual.
            modelClass.isAssignableFrom(ProgramaViewModel::class.java) -> {
                ProgramaViewModel(repository, savedStateHandle) as T
            }
            modelClass.isAssignableFrom(EpisodeDetailViewModel::class.java) -> {
                val episodeId = savedStateHandle.get<String>("episodeId")
                    ?: throw IllegalArgumentException("Missing episodeId for EpisodeDetailViewModel")
                EpisodeDetailViewModel(episodeId, repository) as T
            }
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> {
                SearchViewModel(repository) as T
            }
            modelClass.isAssignableFrom(QueueViewModel::class.java) -> {
                QueueViewModel(appPreferences, repository) as T
            }
            modelClass.isAssignableFrom(DownloadedEpisodeViewModel::class.java) -> {
                DownloadedEpisodeViewModel(appPreferences, repository, applicationContext) as T
            }
            modelClass.isAssignableFrom(OnGoingEpisodeViewModel::class.java) -> {
                OnGoingEpisodeViewModel(appPreferences, repository) as T
            }
            else -> throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}")
        }
    }
}
