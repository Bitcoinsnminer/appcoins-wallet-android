package com.appcoins.wallet.bdsbilling.repository.entity


data class Transaction(val uid: String, val status: Status, val gateway: Gateway?, var txId: String?) {
  companion object {
    fun notFound(): Transaction {
      return Transaction("", Status.INVALID_TRANSACTION, Gateway.unknown(), null)
    }

  }

  enum class Status {
    PENDING, PENDING_SERVICE_AUTHORIZATION, PROCESSING, COMPLETED, INVALID_TRANSACTION, FAILED,
    CANCELED
  }

}