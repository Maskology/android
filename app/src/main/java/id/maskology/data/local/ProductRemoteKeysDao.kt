package id.maskology.data.local

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

interface ProductRemoteKeysDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllProductRemoteKeys(remoteKeys: List<ProductRemoteKeys>)

    @Query("SELECT * FROM productRemoteKeys WHERE id = :id")
    suspend fun getProductRemoteKeysId(id: String): ProductRemoteKeys?

    @Query("DELETE FROM productRemoteKeys")
    suspend fun deleteProductRemoteKeys()
}