package com.example.data

import kotlinx.coroutines.flow.Flow

class LoaderScriptRepository(private val loaderScriptDao: LoaderScriptDao) {
    val allScripts: Flow<List<LoaderScript>> = loaderScriptDao.getAllScripts()

    suspend fun insert(script: LoaderScript) {
        loaderScriptDao.insertScript(script)
    }

    suspend fun delete(script: LoaderScript) {
        loaderScriptDao.deleteScript(script)
    }

    suspend fun deleteById(id: Int) {
        loaderScriptDao.deleteScriptById(id)
    }
}
