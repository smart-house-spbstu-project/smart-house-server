package com.gopea.smart_house_server.common;

public enum InternalStatus {
  OK(true, "OK"),
  FAILED(false, "FAIL");
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
