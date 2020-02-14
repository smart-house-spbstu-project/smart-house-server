package com.gopea.smart_house_server.configs;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StatusCodeTest {

  @Test
  public void testGetStatusCode() {
    assertEquals(500, StatusCode.ERROR.getStatusCode());
  }
  @Test
  public void testToStrimng() {
    assertEquals(String.valueOf(500), StatusCode.ERROR.toString());
  }
}
