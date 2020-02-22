package com.gopea.smart_house_server.devices;


public enum DeviceType {
  //FIRE_DETECTOR(RGBLamp.class),
  //FIREFIGHT_SYSTEM(RGBLamp.class),
  LAMP(Lamp.class),
  RGB_LAMP(RGBLamp.class),
  WINDOW(Window.class),
  DOOR(Door.class);

  private Class<? extends BaseDevice> clazz;

  DeviceType(Class<? extends BaseDevice> clazz) {
    this.clazz = clazz;
  }

  public static DeviceType getEnum(Class<? extends BaseDevice> clazz) {
    for (DeviceType type : DeviceType.values()) {
      if (type.getClazz().equals(clazz)) {
        return type;
      }
    }
    return null;
  }

  public static DeviceType getEnum(String name) {
    if (name == null) {
      return null;
    }
    name = name.toUpperCase();
    DeviceType deviceType = null;
    try {
      deviceType = DeviceType.valueOf(name);
    } catch (IllegalArgumentException e) {
    }
    return deviceType;
  }

  /**
   * Getter for the clazz.
   *
   * @return The clazz.
   */
  public Class<? extends BaseDevice> getClazz() {
    return clazz;
  }
}
