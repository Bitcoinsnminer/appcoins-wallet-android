package com.asfoundation.wallet.ui.iab;

import com.appcoins.wallet.billing.BillingMessagesMapper;
import com.appcoins.wallet.billing.BillingPaymentProofSubmission;
import com.appcoins.wallet.billing.mappers.ExternalBillingSerializer;
import com.appcoins.wallet.billing.repository.entity.Purchase;
import com.asfoundation.wallet.entity.TransactionBuilder;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.math.BigDecimal;
import java.util.List;

public class BdsInAppPurchaseInteractor {
  private final AsfInAppPurchaseInteractor inAppPurchaseInteractor;
  private final BillingPaymentProofSubmission billingPaymentProofSubmission;
  private final ApproveKeyProvider approveKeyProvider;

  public BdsInAppPurchaseInteractor(AsfInAppPurchaseInteractor inAppPurchaseInteractor,
      BillingPaymentProofSubmission billingPaymentProofSubmission,
      ApproveKeyProvider approveKeyProvider) {
    this.inAppPurchaseInteractor = inAppPurchaseInteractor;
    this.billingPaymentProofSubmission = billingPaymentProofSubmission;
    this.approveKeyProvider = approveKeyProvider;
  }

  public Single<TransactionBuilder> parseTransaction(String uri) {
    return inAppPurchaseInteractor.parseTransaction(uri);
  }

  public Completable send(String uri, AsfInAppPurchaseInteractor.TransactionType transactionType,
      String packageName, String productName, BigDecimal channelBudget, String developerPayload) {
    return inAppPurchaseInteractor.send(uri, transactionType, packageName, productName,
        channelBudget, developerPayload);
  }

  public Completable resume(String uri, AsfInAppPurchaseInteractor.TransactionType transactionType,
      String packageName, String productName, String developerPayload) {
    return approveKeyProvider.getKey(packageName, productName)
        .doOnSuccess(billingPaymentProofSubmission::saveTransactionId)
        .flatMapCompletable(
            approveKey -> inAppPurchaseInteractor.resume(uri, transactionType, packageName,
                productName, approveKey, developerPayload));
  }

  public Observable<Payment> getTransactionState(String uri) {
    return inAppPurchaseInteractor.getTransactionState(uri);
  }

  public Completable remove(String uri) {
    return inAppPurchaseInteractor.remove(uri);
  }

  public void start() {
    inAppPurchaseInteractor.start();
  }

  public Observable<List<Payment>> getAll() {
    return inAppPurchaseInteractor.getAll();
  }

  public List<BigDecimal> getTopUpChannelSuggestionValues(BigDecimal price) {
    return inAppPurchaseInteractor.getTopUpChannelSuggestionValues(price);
  }

  public boolean shouldShowDialog() {
    return inAppPurchaseInteractor.shouldShowDialog();
  }

  public void dontShowAgain() {
    inAppPurchaseInteractor.dontShowAgain();
  }

  public Single<Boolean> hasChannel() {
    return inAppPurchaseInteractor.hasChannel();
  }

  public Single<String> getWalletAddress() {
    return inAppPurchaseInteractor.getWalletAddress();
  }

  public BillingMessagesMapper getBillingMessagesMapper() {
    return inAppPurchaseInteractor.getBillingMessagesMapper();
  }

  public ExternalBillingSerializer getBillingSerializer() {
    return inAppPurchaseInteractor.getBillingSerializer();
  }

  public Single<AsfInAppPurchaseInteractor.CurrentPaymentStep> getCurrentPaymentStep(
      String packageName, TransactionBuilder transactionBuilder) {
    return inAppPurchaseInteractor.getCurrentPaymentStep(packageName, transactionBuilder);
  }

  public Single<Purchase> getCompletedPurchase(String packageName, String productName) {
    return inAppPurchaseInteractor.getCompletedPurchase(packageName, productName);
  }
}
