package com.gopea.smart_house_server.routers.users;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertArrayEquals;

public class UserTest {


    @Test
    public void testGetUserType() {
        User target = new User("test_user", UserType.USER, "pass");
        assertEquals(UserType.USER, target.getUserType());
    }

    @Test
    public void testGetUserName() {
        User target = new User("test_user", UserType.USER, "pass");
        assertEquals("test_user", target.getUsername());
    }

    @Test
    public void testGetUserPassword() {
        byte[] password = new byte[]{0, 1, 2};

        User target = new User("test_user", UserType.USER, password);

        assertArrayEquals(password, target.getPassword());
    }

    @Test
    public void testSetPassword() {
        byte[] password = new byte[]{0, 1, 2};

        User target = new User("test_user", UserType.USER, "hh");
        target.setPassword(password);

        assertArrayEquals(password, target.getPassword());
    }

    @Test
    public void testCheckPassword() {
        String password = "password";

        User target = new User("test_user", UserType.USER, password);

        assertTrue(target.checkPassword(password));
    }
}
