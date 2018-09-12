package com.asfoundation.wallet.repository;

import com.appcoins.wallet.billing.BillingPaymentProofSubmission;
import com.appcoins.wallet.billing.PaymentProof;
import com.asf.wallet.BuildConfig;
import com.asfoundation.wallet.interact.DefaultTokenProvider;
import com.asfoundation.wallet.interact.SendTransactionInteract;
import io.reactivex.Completable;

public class BuyTransactionValidator implements TransactionValidator {
  private final SendTransactionInteract sendTransactionInteract;
  private final BillingPaymentProofSubmission billingPaymentProofSubmission;
  private final DefaultTokenProvider defaultTokenProvider;

  public BuyTransactionValidator(SendTransactionInteract sendTransactionInteract,
      BillingPaymentProofSubmission billingPaymentProofSubmission,
      DefaultTokenProvider defaultTokenProvider) {
    this.sendTransactionInteract = sendTransactionInteract;
    this.billingPaymentProofSubmission = billingPaymentProofSubmission;
    this.defaultTokenProvider = defaultTokenProvider;
  }

  @Override public Completable validate(PaymentTransaction paymentTransaction) {
    String packageName = paymentTransaction.getPackageName();
    String storeAddress = BuildConfig.DEFAULT_STORE_ADDRESS;
    String oemAddress = BuildConfig.DEFAULT_OEM_ADDRESS;
    String productName = paymentTransaction.getTransactionBuilder()
        .getSkuId();
    defaultTokenProvider.getDefaultToken()
        .flatMapCompletable(tokenInfo -> sendTransactionInteract.computeBuyTransactionHash(
            paymentTransaction.getTransactionBuilder())
            .map(hash -> new PaymentProof("appcoins", paymentTransaction.getApproveHash(), hash,
                productName, packageName, storeAddress, oemAddress))
            .flatMapCompletable(billingPaymentProofSubmission::processPurchaseProof));
    return Completable.complete();
  }
}
