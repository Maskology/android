package id.maskology.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import id.maskology.data.local.database.MaskologyDatabase
import id.maskology.data.local.database.ProductRemoteKeys
import id.maskology.data.model.Product
import id.maskology.data.remote.api.ApiService
import retrofit2.HttpException

@OptIn(ExperimentalPagingApi::class)
class ProductRemoteMediator(
    private val productDatabase: MaskologyDatabase,
    private val apiService: ApiService
): RemoteMediator<Int, Product>()
{
    override suspend fun initialize(): InitializeAction {
        return InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Product>
    ): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> {
                val remoteKeys = getRemoteKeyClosestToCurrentPosition(state)
                remoteKeys?.nextKey?.minus(1) ?: INITIAL_PAGE_INDEX
            }
            LoadType.PREPEND -> {
                val remoteKeys = getRemoteKeyForFirstItem(state)
                val prevKey = remoteKeys?.prevKey ?: return MediatorResult.Success(
                    endOfPaginationReached = remoteKeys != null
                )
                prevKey
            }
            LoadType.APPEND -> {
                val remoteKeys = getRemoteKeyForLastItem(state)
                val nextKey = remoteKeys?.nextKey ?: return MediatorResult.Success(
                    endOfPaginationReached = remoteKeys != null
                )
                nextKey
            }
        }
        return try {
            val productResponseData = apiService.getAllProduct(page, state.config.pageSize)
            val endOfPaginationReached = productResponseData.listProduct.isEmpty()

            productDatabase.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    productDatabase.productRemoteKeysDao().deleteProductRemoteKeys()
                    productDatabase.productDao().deleteAllProduct()
                }
                val prevKey = if (page == INITIAL_PAGE_INDEX) null else page - 1
                val nextKey = if (endOfPaginationReached) null else page + 1
                val keys = productResponseData.listProduct.map {
                    ProductRemoteKeys(id = it.id, prevKey = prevKey, nextKey = nextKey)
                }

                productDatabase.productRemoteKeysDao().insertAllProductRemoteKeys(keys)
                productDatabase.productDao().insertProduct(productResponseData.listProduct)
            }
            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        }
    }

    private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, Product>) : ProductRemoteKeys? {
        return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()?.let { data ->
            productDatabase.productRemoteKeysDao().getProductRemoteKeysId(data.id)
        }
    }

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, Product>) : ProductRemoteKeys? {
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()?.let { data ->
            productDatabase.productRemoteKeysDao().getProductRemoteKeysId(data.id)
        }
    }

    private suspend fun getRemoteKeyClosestToCurrentPosition(state: PagingState<Int, Product>) : ProductRemoteKeys? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.id?.let { id ->
                productDatabase.productRemoteKeysDao().getProductRemoteKeysId(id)
            }
        }
    }

    private companion object {
        const val INITIAL_PAGE_INDEX = 1
    }

}