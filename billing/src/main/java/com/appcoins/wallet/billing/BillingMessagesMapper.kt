package com.appcoins.wallet.billing

import android.os.Bundle
import com.appcoins.wallet.billing.exceptions.BillingException
import com.appcoins.wallet.billing.repository.entity.Purchase
import java.io.IOException

class BillingMessagesMapper {

  internal fun mapSupported(supportType: Billing.BillingSupportType): Int =
      when (supportType) {
        Billing.BillingSupportType.SUPPORTED -> AppcoinsBillingBinder.RESULT_OK
        Billing.BillingSupportType.MERCHANT_NOT_FOUND -> AppcoinsBillingBinder.RESULT_BILLING_UNAVAILABLE
        Billing.BillingSupportType.UNKNOWN_ERROR -> AppcoinsBillingBinder.RESULT_BILLING_UNAVAILABLE
        Billing.BillingSupportType.NO_INTERNET_CONNECTION -> AppcoinsBillingBinder.RESULT_SERVICE_UNAVAILABLE
        Billing.BillingSupportType.API_ERROR -> AppcoinsBillingBinder.RESULT_ERROR
      }


  private fun map(throwable: Throwable?): Int {
    return throwable?.let {
      when (it) {
        is BillingException -> it.getErrorCode()
          is IOException -> AppcoinsBillingBinder.RESULT_SERVICE_UNAVAILABLE
          is IllegalArgumentException -> AppcoinsBillingBinder.RESULT_DEVELOPER_ERROR
        else -> AppcoinsBillingBinder.RESULT_ERROR
      }
    } ?: AppcoinsBillingBinder.RESULT_ERROR
  }

  fun mapSkuDetails(serializedProducts: List<String>): Bundle {
    val result = Bundle()
    result.putInt(AppcoinsBillingBinder.RESPONSE_CODE, AppcoinsBillingBinder.RESULT_OK)
    result.putStringArrayList(AppcoinsBillingBinder.DETAILS_LIST, ArrayList(serializedProducts))
    return result
  }

  fun mapSkuDetailsError(exception: Exception): Bundle {
    val result = Bundle()
    result.putInt(AppcoinsBillingBinder.RESPONSE_CODE, map(exception.cause))
    return result
  }

    fun mapPurchasesError(exception: Exception): Bundle {
        val result = Bundle()
        result.putInt(AppcoinsBillingBinder.RESPONSE_CODE, map(exception.cause))
        return result
    }

  fun mapBuyIntentError(exception: Exception): Bundle {
    val result = Bundle()
    result.putInt(AppcoinsBillingBinder.RESPONSE_CODE, map(exception.cause))
    return result
  }

  fun mapConsumePurchasesError(exception: Exception): Int {
    return map(exception.cause)
  }

  fun mapCancellation(): Bundle {
    val bundle = Bundle()
    bundle.putInt(AppcoinsBillingBinder.RESPONSE_CODE, AppcoinsBillingBinder.RESULT_USER_CANCELED)
    return bundle
  }

  fun mapPurchase(purchaseId: String, signature: String, signatureData: String): Bundle {
    val intent = Bundle()
    intent.putString(AppcoinsBillingBinder.INAPP_PURCHASE_ID, purchaseId)
    intent.putString(AppcoinsBillingBinder.INAPP_PURCHASE_DATA, signatureData)
    intent.putString(AppcoinsBillingBinder.INAPP_DATA_SIGNATURE, signature)
    intent.putInt(AppcoinsBillingBinder.RESPONSE_CODE, AppcoinsBillingBinder.RESULT_OK)
    return intent
  }
}