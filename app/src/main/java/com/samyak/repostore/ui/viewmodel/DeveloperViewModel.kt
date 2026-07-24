package com.samyak.repostore.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.samyak.repostore.data.model.AppItem
import com.samyak.repostore.data.repository.GitHubRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DeveloperViewModel(
    private val repository: GitHubRepository,
    private val developerName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<DeveloperUiState>(DeveloperUiState.Loading)
    val uiState: StateFlow<DeveloperUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private val loadedApps = mutableListOf<AppItem>()
    private var loadJob: Job? = null
    private var isLoadingMore = false

    init {
        loadDeveloperApps()
    }

    private fun loadDeveloperApps(refresh: Boolean = false) {
        if (refresh) {
            currentPage = 1
            loadedApps.clear()
            isLoadingMore = false
        }

        loadJob?.cancel()

        loadJob = viewModelScope.launch {
            _uiState.value = if (loadedApps.isEmpty()) {
                DeveloperUiState.Loading
            } else {
                DeveloperUiState.LoadingMore(loadedApps.toList())
            }

            val result = repository.getDeveloperRepos(developerName, currentPage)

            result.fold(
                onSuccess = { apps ->
                    if (refresh || currentPage == 1) {
                        loadedApps.clear()
                    }
                    loadedApps.addAll(apps)

                    _uiState.value = if (loadedApps.isEmpty()) {
                        DeveloperUiState.Empty
                    } else {
                        DeveloperUiState.Success(loadedApps.toList())
                    }
                    isLoadingMore = false
                },
                onFailure = { error ->
                    _uiState.value = if (loadedApps.isEmpty()) {
                        DeveloperUiState.Error(error.message ?: "Failed to load repositories")
                    } else {
                        DeveloperUiState.Success(loadedApps.toList())
                    }
                    isLoadingMore = false
                }
            )
        }
    }

    fun loadMore() {
        if (isLoadingMore) return
        if (_uiState.value is DeveloperUiState.Loading) return

        isLoadingMore = true
        currentPage++

        viewModelScope.launch {
            _uiState.value = DeveloperUiState.LoadingMore(loadedApps.toList())

            val result = repository.getDeveloperRepos(developerName, currentPage)

            result.fold(
                onSuccess = { apps ->
                    // De-duplicate against already-loaded repos so overlapping pages
                    // don't create duplicate list entries (which break DiffUtil).
                    val existingIds = loadedApps.map { it.repo.id }.toHashSet()
                    val newApps = apps.filter { existingIds.add(it.repo.id) }
                    loadedApps.addAll(newApps)
                    _uiState.value = DeveloperUiState.Success(loadedApps.toList())
                },
                onFailure = {
                    // Revert the page increment so a failed page can be retried instead
                    // of being permanently skipped on the next loadMore.
                    currentPage--
                    _uiState.value = DeveloperUiState.Success(loadedApps.toList())
                }
            )
            isLoadingMore = false
        }
    }

    fun refresh() {
        loadDeveloperApps(refresh = true)
    }

    fun retry() {
        loadDeveloperApps(refresh = true)
    }
}

sealed class DeveloperUiState {
    data object Loading : DeveloperUiState()
    data object Empty : DeveloperUiState()
    data class LoadingMore(val currentApps: List<AppItem>) : DeveloperUiState()
    data class Success(val apps: List<AppItem>) : DeveloperUiState()
    data class Error(val message: String) : DeveloperUiState()
}

class DeveloperViewModelFactory(
    private val repository: GitHubRepository,
    private val developerName: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeveloperViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DeveloperViewModel(repository, developerName) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
