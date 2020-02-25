package com.gopea.smart_house_server.routers.users;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

public class UserTypeTest {
    @Test
    public void testGetInvalidUserTypeNull() {
        UserType userType = UserType.getEnum(null);
        assertNull(userType);
    }

    @Test
    public void testGetInvalidUserType() {
        UserType userType = UserType.getEnum("test");
        assertNull(userType);
    }

    @Test
    public void testGetUserType() {
        UserType userType = UserType.getEnum("admin");
        assertEquals(UserType.ADMIN, userType);
    }
}
