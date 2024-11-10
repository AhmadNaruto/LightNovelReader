package indi.dmzz_yyhyy.lightnovelreader.ui.home.exploration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import indi.dmzz_yyhyy.lightnovelreader.data.web.WebBookDataSource
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel
class ExplorationViewModel @Inject constructor(
    private val webBookDataSource: WebBookDataSource
) : ViewModel() {
    private var _uiState = MutableExplorationUiState()
    val uiState: ExplorationUiState = _uiState

    init {
        viewModelScope.launch(Dispatchers.IO) {
            webBookDataSource.isOffLineFlow.collect {
                _uiState.isOffLine = it
            }
        }
    }

    fun refresh() {
        _uiState.isRefreshing = true
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.isOffLine = webBookDataSource.isOffLine()
            _uiState.isRefreshing = false
        }
    }
}