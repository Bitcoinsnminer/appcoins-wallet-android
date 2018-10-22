package com.asfoundation.wallet.repository;

import com.appcoins.wallet.billing.repository.entity.Transaction;
import io.reactivex.Single;

public interface BdsTransactionProvider {
  Single<Transaction> get(String packageName, String sku);
}
