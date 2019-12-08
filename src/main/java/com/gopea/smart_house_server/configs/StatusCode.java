package com.gopea.smart_house_server.configs;

import java.util.Objects;

public enum StatusCode {
  SUCCESS(200),
  CREATED(201),
  NO_CONTENT(204),
  BAD_REQUEST(400),
  NOT_FOUND(404),
  FORBIDDEN(403),
  ERROR(500),
  UNAUTHORISED(401),
  UNPROCESSABLE_ENTITY(422),
  UNAVAILABLE(503);

  private final int statusCode;

  StatusCode(int statusCode) {
    this.statusCode = statusCode;
  }

  /**
   * Getter for the statusCode.
   *
   * @return The statusCode.
   */
  public int getStatusCode() {
    return statusCode;
  }

  @Override
  public String toString() {
    return "" + statusCode;
  }
}
