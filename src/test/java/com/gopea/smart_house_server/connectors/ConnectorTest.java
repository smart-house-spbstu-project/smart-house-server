package com.gopea.smart_house_server.connectors;

import com.gopea.smart_house_server.examples.StandardDeviceExample;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mock;

public class ConnectorTest {

  private StandardDeviceExample deviceExample;
  private BaseTestDeviceConnector deviceConnector;

  @Before
  public void before() {
    deviceExample = mock(StandardDeviceExample.class);
    deviceConnector = new BaseTestDeviceConnector("host", 8080, deviceExample);
  }

  @Test
  public void testGetHost() {
    assertEquals("host", deviceConnector.getHost());
  }

  @Test
  public void testGetPort() {
    assertEquals(8080, deviceConnector.getPort());
  }

}
