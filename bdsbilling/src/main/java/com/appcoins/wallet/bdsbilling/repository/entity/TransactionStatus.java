package com.appcoins.wallet.bdsbilling.repository.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionStatus {
  public TransactionStatus(String uid, String status) {
    this.uid = uid;
    this.status = status;
  }

  @JsonProperty("uid") private String uid;

  @JsonProperty("status") private String status;

  @JsonProperty("uid") public String getUid() {
    return uid;
  }

  @JsonProperty("uid") public void setUid(String uid) {
    this.uid = uid;
  }

  @JsonProperty("status") public String getStatus() {
    return status;
  }

  @JsonProperty("status") public void setStatus(String status) {
    this.status = status;
  }
}