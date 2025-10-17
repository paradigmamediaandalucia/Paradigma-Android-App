package com.example.paradigmaapp.android.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.ui.EpisodeListItem
import com.example.paradigmaapp.android.ui.ErrorType
import com.example.paradigmaapp.android.ui.ErrorView
import com.example.paradigmaapp.android.ui.LayoutConstants
import com.example.paradigmaapp.android.ui.SearchBar
import com.example.paradigmaapp.android.viewmodel.DownloadedEpisodeViewModel
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.android.viewmodel.NotificationType
import com.example.paradigmaapp.android.viewmodel.QueueViewModel
import com.example.paradigmaapp.android.viewmodel.SearchViewModel
import com.example.paradigmaapp.model.Episode
import com.example.paradigmaapp.model.stableListKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel,
    mainViewModel: MainViewModel,
    queueViewModel: QueueViewModel,
    downloadedViewModel: DownloadedEpisodeViewModel,
    onEpisodeSelected: (Episode) -> Unit,
    onEpisodeLongClicked: (Episode) -> Unit,
    onBackClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    val searchText by searchViewModel.searchText.collectAsState()
    val searchResults by searchViewModel.searchResults.collectAsState()
    val isSearching by searchViewModel.isSearching.collectAsState()
    val searchError by searchViewModel.searchError.collectAsState()
    val downloadedEpisodes by downloadedViewModel.downloadedEpisodes.collectAsState()
    val queueEpisodeIds by queueViewModel.queueEpisodeIds.collectAsState()
    val preparingEpisodeId by mainViewModel.preparingEpisodeId.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            SearchBar(
                searchText = searchText,
                onSearchTextChanged = { query -> searchViewModel.onSearchTextChanged(query) },
                onClearSearch = { searchViewModel.clearSearch() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        top = 0.dp,
                        end = 70.dp
                    ),
                label = "Buscar episodios..."
            )
            Spacer(modifier = Modifier.height(16.dp))

            when {
                isSearching -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                searchError != null && searchResults.isEmpty() -> {
                    val errorType = determineErrorType(searchError)
                    ErrorView(
                        message = searchError!!,
                        errorType = errorType,
                        onRetry = if (errorType != ErrorType.NO_RESULTS) {
                            { searchViewModel.retrySearch() }
                        } else {
                            null
                        }
                    )
                }

                searchText.length >= SearchViewModel.MIN_QUERY_LENGTH &&
                    searchResults.isEmpty() &&
                    !isSearching &&
                    searchError == null -> {
                    ErrorView(
                        message = "No se encontraron episodios para \"$searchText\".",
                        errorType = ErrorType.NO_RESULTS
                    )
                }

                searchResults.isNotEmpty() -> {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = LayoutConstants.bottomContentPadding)
                    ) {
                        items(searchResults, key = { it.stableListKey() }) { episode ->
                            val isLoading = episode.id == preparingEpisodeId
                            EpisodeListItem(
                                episode = episode,
                                isLoading = isLoading,
                                onPlayEpisode = { onEpisodeSelected(it) },
                                onEpisodeLongClick = { onEpisodeLongClicked(it) },
                                onAddToQueue = { queueViewModel.addEpisodeToQueue(it) },
                                onRemoveFromQueue = { queueViewModel.removeEpisodeFromQueue(it) },
                                onDownloadEpisode = { targetEpisode ->
                                    downloadedViewModel.downloadEpisode(targetEpisode) { result ->
                                        result.onSuccess {
                                            mainViewModel.showTopNotification("Descarga completada", NotificationType.SUCCESS)
                                        }.onFailure {
                                            mainViewModel.showTopNotification("Descarga fallida", NotificationType.FAILURE)
                                        }
                                    }
                                },
                                onDeleteDownload = { downloadedViewModel.deleteDownloadedEpisode(it) },
                                isDownloaded = downloadedEpisodes.any { it.id == episode.id },
                                isInQueue = queueEpisodeIds.contains(episode.id),
                                isParentScrolling = listState.isScrollInProgress,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                searchText.length < SearchViewModel.MIN_QUERY_LENGTH && searchText.isNotEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Buscar",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(60.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Escribe al menos ${SearchViewModel.MIN_QUERY_LENGTH} caracteres para buscar.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Pantalla de búsqueda",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(120.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Busca tus episodios favoritos escribiendo en el campo superior.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun determineErrorType(errorMessage: String?): ErrorType {
    return when {
        errorMessage == null -> ErrorType.GENERAL_SERVER_ERROR
        errorMessage.contains("internet", ignoreCase = true) || errorMessage.contains("conectar", ignoreCase = true) -> ErrorType.NO_INTERNET
        errorMessage.startsWith("No se encontraron", ignoreCase = true) -> ErrorType.NO_RESULTS
        else -> ErrorType.GENERAL_SERVER_ERROR
    }
}
