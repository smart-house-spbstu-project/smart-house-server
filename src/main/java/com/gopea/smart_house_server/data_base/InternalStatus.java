package com.gopea.smart_house_server.data_base;

public enum InternalStatus {
  OK(true, "OK"),
  FAILED(false, "FALSE");
  public final boolean isOk;
  public final String type;

  InternalStatus(boolean isOk, String type) {
    this.isOk = isOk;
    this.type = type;
  }

  @Override
  public String toString() {
    return type;
  }
}
