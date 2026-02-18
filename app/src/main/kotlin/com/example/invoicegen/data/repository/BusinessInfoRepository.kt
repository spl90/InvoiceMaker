package com.example.invoicegen.data.repository

import com.example.invoicegen.data.db.BusinessInfoDao
import com.example.invoicegen.data.model.BusinessInfo
import com.example.invoicegen.data.model.toDomain
import com.example.invoicegen.data.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BusinessInfoRepository @Inject constructor(
    private val dao: BusinessInfoDao
) {
    fun getBusinessInfo(): Flow<BusinessInfo> =
        dao.getBusinessInfo().map { it?.toDomain() ?: BusinessInfo() }

    suspend fun getBusinessInfoOnce(): BusinessInfo =
        dao.getBusinessInfoOnce()?.toDomain() ?: BusinessInfo()

    suspend fun save(info: BusinessInfo) {
        dao.upsert(info.toEntity())
    }
}
