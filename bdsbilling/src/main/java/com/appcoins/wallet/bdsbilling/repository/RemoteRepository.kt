package com.appcoins.wallet.bdsbilling.repository

import com.appcoins.wallet.bdsbilling.repository.entity.*
import com.appcoins.wallet.bdsbilling.repository.entity.authorization.Authorization
import com.appcoins.wallet.billing.repository.entity.Product
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.http.*
import java.math.BigDecimal

class RemoteRepository(private val api: BdsApi, private val responseMapper: BdsApiResponseMapper,
                       private val bdsApiSecondary: BdsApiSecondary) {
  companion object {
    private val ADYEN_GATEWAY = "adyen"
  }

  internal fun isBillingSupported(packageName: String,
                                  type: BillingSupportedType): Single<Boolean> {
    return api.getPackage(packageName, type.name.toLowerCase()).map { responseMapper.map(it) }
  }

  internal fun getSkuDetails(packageName: String, skus: List<String>,
                             type: String): Single<List<Product>> {
    return api.getPackages(packageName, skus.joinToString(separator = ","))
        .map { responseMapper.map(it) }
  }

  internal fun getSkuPurchase(packageName: String,
                              skuId: String,
                              walletAddress: String,
                              walletSignature: String): Single<Purchase> {
    return api.getSkuPurchase(packageName, skuId, walletAddress, walletSignature)
  }

  internal fun getSkuTransaction(packageName: String,
                                 skuId: String,
                                 walletAddress: String,
                                 walletSignature: String): Single<TransactionsResponse> {
    return api.getSkuTransaction(walletAddress, walletSignature, 0, TransactionType.INAPP, 1,
        "latest", false, skuId, packageName)

  }

  internal fun getPurchases(packageName: String,
                            walletAddress: String,
                            walletSignature: String,
                            type: BillingSupportedType): Single<List<Purchase>> {
    return api.getPurchases(packageName, walletAddress, walletSignature,
        type.name.toLowerCase()).map { responseMapper.map(it) }
  }

  internal fun consumePurchase(packageName: String,
                               purchaseToken: String,
                               walletAddress: String,
                               walletSignature: String): Single<Boolean> {
    return api.consumePurchase(packageName, purchaseToken, walletAddress, walletSignature,
        Consumed())
        .map { responseMapper.map(it) }
  }

  fun registerAuthorizationProof(origin: String, type: String, oemWallet: String, id: String,
                                 paymentType: String, walletAddress: String,
                                 walletSignature: String, productName: String, packageName: String,
                                 priceValue: BigDecimal,
                                 developerWallet: String, storeWallet: String,
                                 developerPayload: String?): Single<TransactionStatus> {
      return api.createTransaction(paymentType, origin, packageName, priceValue.toPlainString(), "APPC",
              productName,
              type, developerWallet, storeWallet, oemWallet, id, developerPayload, walletAddress, walletSignature)
  }

  fun registerPaymentProof(paymentId: String, paymentType: String, walletAddress: String,
                           walletSignature: String,
                           paymentProof: String): Completable {
    return api.patchTransaction(paymentType, paymentId, walletAddress, walletSignature,
        paymentProof)
  }

  internal fun getGateways(): Single<List<Gateway>> {
    return api.getGateways().map { responseMapper.map(it) }
  }

  fun patchTransaction(uid: String, walletAddress: String, walletSignature: String,
                       paykey: String): Completable {
    return api.patchTransaction(ADYEN_GATEWAY, uid, walletAddress, walletSignature, paykey)
  }

  fun getSessionKey(uid: String, walletAddress: String,
                    walletSignature: String): Single<Authorization> {
    return api.getSessionKey(uid, walletAddress, walletSignature)
        .singleOrError()
  }

  fun createAdyenTransaction(origin: String?, walletAddress: String,
                             walletSignature: String, token: String,
                             packageName: String, priceValue: BigDecimal, priceCurrency: String,
                             productName: String?, type: String,
                             walletDeveloper: String,
                             walletStore: String, walletOem: String, developerPayload: String?): Single<TransactionStatus> {
      return api.createTransaction(ADYEN_GATEWAY, origin, packageName, priceValue.toPlainString(),
              priceCurrency,
              productName, type, walletDeveloper, walletStore, walletOem, token, developerPayload,
              walletAddress,
              walletSignature)
  }

  fun getAppcoinsTransaction(uid: String, address: String,
                             signedContent: String): Single<Transaction> {
    return api.getAppcoinsTransaction(uid, address, signedContent)
  }

  fun getWallet(packageName: String): Single<GetWalletResponse> {
    return bdsApiSecondary.getWallet(packageName)
  }

  interface BdsApi {

    @GET("inapp/8.20180518/packages/{packageName}")
    fun getPackage(@Path("packageName") packageName: String, @Query("type")
    type: String): Single<GetPackageResponse>

    @GET("inapp/8.20180518/packages/{packageName}/products")
    fun getPackages(@Path("packageName") packageName: String,
                    @Query("names") names: String): Single<DetailsResponseBody>

    @Headers("Content-Type: application/json")
    @GET("inapp/8.20180518/packages/{packageName}/products/{skuId}/purchase")
    fun getSkuPurchase(@Path("packageName") packageName: String,
                       @Path("skuId") skuId: String,
                       @Query("wallet.address") walletAddress: String,
                       @Query("wallet.signature") walletSignature: String): Single<Purchase>

    @GET("broker/8.20180518/transactions")
    fun getSkuTransaction(
        @Query("wallet.address") walletAddress: String,
        @Query("wallet.signature") walletSignature: String,
        @Query("cursor") cursor: Long,
        @Query("type") type: TransactionType,
        @Query("limit") limit: Long,
        @Query("sort.name") sort: String,
        @Query("sort.reverse") isReverse: Boolean,
        @Query("product") skuId: String,
        @Query("domain") packageName: String
    ): Single<TransactionsResponse>

    @GET("broker/8.20180518/transactions/{uId}")
    fun getAppcoinsTransaction(@Path("uId") uId: String,
                               @Query("wallet.address") walletAddress: String,
                               @Query("wallet.signature")
                               walletSignature: String): Single<Transaction>


    @GET("inapp/8.20180518/packages/{packageName}/purchases")
    fun getPurchases(@Path("packageName") packageName: String,
                     @Query("wallet.address") walletAddress: String,
                     @Query("wallet.signature") walletSignature: String,
                     @Query("type") type: String): Single<GetPurchasesResponse>

    @Headers("Content-Type: application/json")
    @PATCH("inapp/8.20180518/packages/{packageName}/purchases/{purchaseId}")
    fun consumePurchase(@Path("packageName") packageName: String,
                        @Path("purchaseId") purchaseToken: String,
                        @Query("wallet.address") walletAddress: String,
                        @Query("wallet.signature") walletSignature: String,
                        @Body data: Consumed): Single<Void>

    @GET("inapp/8.20180518/gateways")
    fun getGateways(): Single<GetGatewaysResponse>

    @FormUrlEncoded
    @PATCH("broker/8.20180518/gateways/{gateway}/transactions/{uid}")
    fun patchTransaction(
        @Path("gateway") gateway: String,
        @Path("uid") uid: String, @Query("wallet.address") walletAddress: String,
        @Query("wallet.signature") walletSignature: String, @Field("pay_key")
        paykey: String): Completable

    @GET("broker/8.20180518/gateways/adyen/transactions/{uid}/authorization")
    fun getSessionKey(
        @Path("uid") uid: String, @Query("wallet.address") walletAddress: String,
        @Query("wallet.signature") walletSignature: String): Observable<Authorization>

    @FormUrlEncoded
    @POST("broker/8.20180518/gateways/{gateway}/transactions")
    fun createTransaction(@Path("gateway") gateway: String,
                          @Field("origin") origin: String?,
                          @Field("domain") domain: String,
                          @Field("price.value") priceValue: String?,
                          @Field("price.currency") priceCurrency: String,
                          @Field("product") product: String?,
                          @Field("type") type: String,
                          @Field("wallets.developer") walletsDeveloper: String,
                          @Field("wallets.store") walletsStore: String,
                          @Field("wallets.oem") walletsOem: String,
                          @Field("token") token: String,
                          @Field("metadata") developerPayload: String?,
                          @Query("wallet.address") walletAddress: String,
                          @Query("wallet.signature")
                          walletSignature: String): Single<TransactionStatus>
  }

  data class Consumed(val status: String = "CONSUMED")
}
