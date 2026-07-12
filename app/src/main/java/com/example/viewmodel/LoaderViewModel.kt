package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.LoaderScript
import com.example.data.LoaderScriptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LoaderViewModel(private val repository: LoaderScriptRepository) : ViewModel() {

    // Inputs
    private val _scriptInput = MutableStateFlow("")
    val scriptInput: StateFlow<String> = _scriptInput.asStateFlow()

    private val _customLabel = MutableStateFlow("")
    val customLabel: StateFlow<String> = _customLabel.asStateFlow()

    // Loaded State
    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    private val _generatedScript = MutableStateFlow<String?>(null)
    val generatedScript: StateFlow<String?> = _generatedScript.asStateFlow()

    // Status Message / Notification
    private val _statusMessage = MutableStateFlow("Siap memuat script...")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    // Saved script database flows
    val savedScripts: StateFlow<List<LoaderScript>> = repository.allScripts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Pre-populate with some mock/example scripts if empty
        viewModelScope.launch {
            repository.allScripts.collect { list ->
                if (list.isEmpty()) {
                    repository.insert(LoaderScript(name = "Meknoyu Hub V1", scriptIdOrUrl = "hub1"))
                    repository.insert(LoaderScript(name = "Auto Farm Blox Fruits", scriptIdOrUrl = "fruits"))
                    repository.insert(LoaderScript(name = "Universal Fly Hack", scriptIdOrUrl = "fly"))
                }
            }
        }
    }

    fun onInputChange(text: String) {
        _scriptInput.value = text
    }

    fun onLabelChange(text: String) {
        _customLabel.value = text
    }

    fun loadScript() {
        val input = _scriptInput.value.trim()
        if (input.isEmpty()) {
            _statusMessage.value = "Peringatan: Silakan masukkan nomer atau script terlebih dahulu!"
            _isLoaded.value = false
            _generatedScript.value = null
            return
        }

        // Generate the standard Meknoyu loadstring format
        val scriptCode = "loadstring(game:HttpGet(\"https://meknoyu.com/Loader/$input\"))()"
        _generatedScript.value = scriptCode
        _isLoaded.value = true
        _statusMessage.value = "Script berhasil dimuat! Siap dieksekusi di Roblox."
    }

    fun unloadScript() {
        _isLoaded.value = false
        _generatedScript.value = null
        _statusMessage.value = "Script berhasil di-unload (dinonaktifkan)."
    }

    fun unloadAll() {
        unloadScript()
        _scriptInput.value = ""
        _customLabel.value = ""
        _statusMessage.value = "Semua state telah dibersihkan."
    }

    fun saveCurrentScriptToFavorites() {
        val input = _scriptInput.value.trim()
        var label = _customLabel.value.trim()

        if (input.isEmpty()) {
            _statusMessage.value = "Peringatan: Tidak ada script untuk disimpan!"
            return
        }

        if (label.isEmpty()) {
            label = "Script $input"
        }

        viewModelScope.launch {
            repository.insert(
                LoaderScript(
                    name = label,
                    scriptIdOrUrl = input
                )
            )
            _customLabel.value = ""
            _statusMessage.value = "Script '$label' disimpan ke Favorit!"
        }
    }

    fun loadFromFavorite(script: LoaderScript) {
        _scriptInput.value = script.scriptIdOrUrl
        _statusMessage.value = "Memilih script: ${script.name}"
        loadScript()
    }

    fun deleteFavorite(script: LoaderScript) {
        viewModelScope.launch {
            repository.delete(script)
            _statusMessage.value = "Berhasil menghapus '${script.name}'"
        }
    }
}

class LoaderViewModelFactory(private val repository: LoaderScriptRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoaderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoaderViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
