package com.asfoundation.wallet.ui.iab;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.appcoins.wallet.billing.BdsBilling;
import com.appcoins.wallet.billing.BillingThrowableCodeMapper;
import com.appcoins.wallet.billing.WalletService;
import com.appcoins.wallet.billing.repository.BdsApiResponseMapper;
import com.appcoins.wallet.billing.repository.BdsRepository;
import com.appcoins.wallet.billing.repository.RemoteRepository;
import com.appcoins.wallet.billing.repository.entity.DeveloperPurchase;
import com.appcoins.wallet.billing.repository.entity.Purchase;
import com.asf.wallet.R;
import com.asfoundation.wallet.repository.BdsPendingTransactionService;
import com.facebook.appevents.AppEventsLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxrelay2.PublishRelay;
import dagger.android.support.DaggerFragment;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Currency;
import java.util.Formatter;
import java.util.Locale;
import javax.inject.Inject;

import static com.asfoundation.wallet.ui.iab.IabActivity.PRODUCT_NAME;
import static com.asfoundation.wallet.ui.iab.IabActivity.TRANSACTION_AMOUNT;
import static com.asfoundation.wallet.ui.iab.IabActivity.TRANSACTION_CURRENCY;
import static com.asfoundation.wallet.ui.iab.IabActivity.TRANSACTION_DATA;

/**
 * Created by franciscocalado on 20/07/2018.
 */

public class ExpressCheckoutBuyFragment extends DaggerFragment implements ExpressCheckoutBuyView {
  public static final String APP_PACKAGE = "app_package";
  public static final String SKU_ID = "sku_id";

  private static final String INAPP_PURCHASE_DATA = "INAPP_PURCHASE_DATA";
  private static final String INAPP_DATA_SIGNATURE = "INAPP_DATA_SIGNATURE";
  private static final String INAPP_PURCHASE_ID = "INAPP_PURCHASE_ID";
  private final CompositeDisposable compositeDisposable = new CompositeDisposable();
  @Inject InAppPurchaseInteractor inAppPurchaseInteractor;
  @Inject RemoteRepository.BdsApi bdsApi;
  @Inject WalletService walletService;
  @Inject BdsPendingTransactionService bdsPendingTransactionService;
  @Inject BdsRepository bdsRepository;
  private Bundle extras;
  private PublishRelay<Snackbar> buyButtonClick;
  private IabView iabView;
  private ExpressCheckoutBuyPresenter presenter;
  private ProgressBar loadingView;
  private View dialog;
  private TextView appName;
  private TextView itemHeaderDescription;
  private TextView itemListDescription;
  private TextView itemPrice;
  private TextView itemFinalPrice;
  private ImageView appIcon;
  private Button buyButton;
  private Button cancelButton;
  private FiatValue fiatValue;
  private View errorView;
  private TextView errorMessage;
  private Button errorDismissButton;
  private BdsBilling bdsBilling;
  private PublishSubject<Boolean> setupSubject;
  private View processingDialog;
  private TextView walletAddressView;

  public static ExpressCheckoutBuyFragment newInstance(Bundle extras) {
    ExpressCheckoutBuyFragment fragment = new ExpressCheckoutBuyFragment();
    Bundle bundle = new Bundle();
    bundle.putBundle("extras", extras);
    fragment.setArguments(bundle);
    return fragment;
  }

  public static String serializeJson(Purchase purchase) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    DeveloperPurchase developerPurchase = objectMapper.readValue(new Gson().toJson(
        purchase.getSignature()
            .getMessage()), DeveloperPurchase.class);
    return objectMapper.writeValueAsString(developerPurchase);
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setupSubject = PublishSubject.create();

    extras = getArguments().getBundle("extras");

    bdsBilling = new BdsBilling(getAppPackage(),
        new BdsRepository(new RemoteRepository(bdsApi, new BdsApiResponseMapper()),
            new BillingThrowableCodeMapper()), walletService, new BillingThrowableCodeMapper());

    presenter = new ExpressCheckoutBuyPresenter(this, inAppPurchaseInteractor,
        AndroidSchedulers.mainThread(), new CompositeDisposable(),
        inAppPurchaseInteractor.getBillingMessagesMapper(), bdsPendingTransactionService,
        bdsBilling);
  }

  @Override public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    return inflater.inflate(R.layout.fragment_express_checkout_buy, container, false);
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    loadingView = view.findViewById(R.id.loading_view);
    dialog = view.findViewById(R.id.info_dialog);
    appName = view.findViewById(R.id.app_name);
    itemHeaderDescription = view.findViewById(R.id.app_sku_description);
    itemListDescription = view.findViewById(R.id.sku_description);
    itemPrice = view.findViewById(R.id.sku_price);
    itemFinalPrice = view.findViewById(R.id.total_price);
    appIcon = view.findViewById(R.id.app_icon);
    buyButton = view.findViewById(R.id.buy_button);
    cancelButton = view.findViewById(R.id.cancel_button);
    errorView = view.findViewById(R.id.error_message);
    errorMessage = view.findViewById(R.id.activity_iab_error_message);
    errorDismissButton = view.findViewById(R.id.activity_iab_error_ok_button);
    processingDialog = view.findViewById(R.id.processing_loading);
    ((TextView) processingDialog.findViewById(R.id.loading_message)).setText(
        R.string.activity_aib_buying_message);
    walletAddressView = view.findViewById(R.id.wallet_address_footer);

    Single.defer(() -> Single.just(getAppPackage()))
        .observeOn(Schedulers.io())
        .map(packageName -> new Pair<>(getApplicationName(packageName),
            getContext().getPackageManager()
                .getApplicationIcon(packageName)))
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(pair -> {
          appName.setText(pair.first);
          appIcon.setImageDrawable(pair.second);
        }, throwable -> {
          throwable.printStackTrace();
          showError();
        });
    buyButton.setOnClickListener(v -> iabView.navigateToCreditCardAuthorization());
    presenter.present(((BigDecimal) extras.getSerializable(TRANSACTION_AMOUNT)).doubleValue(),
        extras.getString(TRANSACTION_CURRENCY), getSkuId());
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    compositeDisposable.dispose();

    presenter.stop();
    loadingView = null;
    dialog = null;
    appName = null;
    itemHeaderDescription = null;
    itemListDescription = null;
    itemPrice = null;
    itemFinalPrice = null;
    appIcon = null;
    buyButton = null;
    cancelButton = null;
  }

  @Override public void onDetach() {
    super.onDetach();
    iabView = null;
  }

  private String getSkuId() {
    return inAppPurchaseInteractor.parseTransaction(extras.getString(TRANSACTION_DATA), true)
        .blockingGet()
        .getSkuId();
  }

  @Override public void onAttach(Context context) {
    super.onAttach(context);
    if (!(context instanceof IabView)) {
      throw new IllegalStateException(
          "Express checkout buy fragment must be attached to IAB activity");
    }
    iabView = ((IabView) context);
  }

  @Override public void setup(FiatValue response) {
    Formatter formatter = new Formatter();
    StringBuilder builder = new StringBuilder();
    String valueText = formatter.format(Locale.getDefault(), "%(,.2f",
        (BigDecimal) extras.getSerializable(TRANSACTION_AMOUNT))
        .toString() + " APPC";
    String valueTextCompose = valueText + " = ";
    DecimalFormat decimalFormat = new DecimalFormat("0.00");
    String currency = mapCurrencyCodeToSymbol(response.getCurrency());
    String priceText = currency + decimalFormat.format(response.getAmount());
    String finalString = valueTextCompose + priceText;
    Spannable spannable = new SpannableString(finalString);
    spannable.setSpan(new AbsoluteSizeSpan(12, true), finalString.indexOf(valueTextCompose),
        finalString.indexOf(valueTextCompose) + valueTextCompose.length(),
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    spannable.setSpan(
        new ForegroundColorSpan(getResources().getColor(R.color.dialog_buy_total_value)),
        finalString.indexOf(priceText), finalString.indexOf(priceText) + priceText.length(),
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    itemPrice.setText(valueText);
    itemFinalPrice.setText(spannable, TextView.BufferType.SPANNABLE);
    fiatValue = response;
    if (extras.containsKey(PRODUCT_NAME)) {
      itemHeaderDescription.setText(extras.getString(PRODUCT_NAME));
      itemListDescription.setText(extras.getString(PRODUCT_NAME));
    }
    AppEventsLogger.newLogger(getContext())
        .logEvent("in_app_purchase_dialog_credit_card_open");

    compositeDisposable.add(walletService.getWalletAddress()
        .doOnSuccess(address -> walletAddressView.setText(address))
        .subscribe(__ -> {
        }, Throwable::printStackTrace));
    setupSubject.onNext(true);
  }

  @Override public void showError() {
    loadingView.setVisibility(View.GONE);
    dialog.setVisibility(View.GONE);
    errorView.setVisibility(View.VISIBLE);
    errorMessage.setText(R.string.activity_iab_error_message);
  }

  @Override public Observable<Object> getCancelClick() {
    return RxView.clicks(cancelButton);
  }

  @Override public void close(Bundle data) {
    iabView.finish(data);
  }

  @Override public Observable<Object> errorDismisses() {
    return RxView.clicks(errorDismissButton);
  }

  @Override public void hideLoading() {
    loadingView.setVisibility(View.GONE);
    if (processingDialog.getVisibility() != View.VISIBLE) {
      dialog.setVisibility(View.VISIBLE);
    }
  }

  @Override public void showLoading() {
    loadingView.setVisibility(View.VISIBLE);
    dialog.setVisibility(View.INVISIBLE);
  }

  @Override public Observable<Boolean> setupUiCompleted() {
    return setupSubject;
  }

  @Override public void showProcessingLoadingDialog() {
    dialog.setVisibility(View.GONE);
    loadingView.setVisibility(View.GONE);
    processingDialog.setVisibility(View.VISIBLE);
  }

  @Override public void finish(Purchase purchase) throws IOException {
    Bundle bundle = new Bundle();
    bundle.putString(INAPP_PURCHASE_DATA, serializeJson(purchase));
    bundle.putString(INAPP_DATA_SIGNATURE, purchase.getSignature()
        .getValue());
    bundle.putString(INAPP_PURCHASE_ID, purchase.getUid());
    close(bundle);
  }

  private CharSequence getApplicationName(String appPackage)
      throws PackageManager.NameNotFoundException {
    PackageManager packageManager = getContext().getPackageManager();
    ApplicationInfo packageInfo = packageManager.getApplicationInfo(appPackage, 0);
    return packageManager.getApplicationLabel(packageInfo);
  }

  public String getAppPackage() {
    if (extras.containsKey(APP_PACKAGE)) {
      return extras.getString(APP_PACKAGE);
    }
    throw new IllegalArgumentException("previous app package name not found");
  }

  public String getProductName() {
    if (extras.containsKey(PRODUCT_NAME)) {
      return extras.getString(PRODUCT_NAME);
    }
    throw new IllegalArgumentException("product name not found");
  }

  public String mapCurrencyCodeToSymbol(String currencyCode) {
    return Currency.getInstance(currencyCode)
        .getSymbol();
  }
}
