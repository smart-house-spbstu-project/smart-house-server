from common.helpers import *
import requests
import time

USER_URL = BASE_URL + "/user"


def create_new_user(username, user_type, password, auth, expected_status_code=201):
    data = {
        "username": username,
        "user_type": user_type,
        "password": password
    }

    response = requests.post(USER_URL, json=data, verify=False, auth=auth)

    assert response.status_code == expected_status_code
    return response


def delete_user(username, auth, expected_status=204):
    response = requests.delete("{}/{}".format(USER_URL, username), verify=False, auth=auth)
    assert response.status_code == expected_status
    return response


def change_password(username, new_password, auth, expected_status_code=200):
    data = {
        "password": new_password
    }
    response = requests.patch("{}/{}".format(USER_URL, username), auth=auth, json=data, verify=False)
    assert response.status_code == expected_status_code
    return response


class TestUser:

    def test_create_new_user(self):
        response = create_new_user("test_user_111", "user", "test", AUTH)
        username = response.json().get("username")
        assert username == "test_user_111"
        delete_user(username, AUTH)

    def test_create_new_user_already_exists(self):
        response = create_new_user("test_user_111", "user", "test", AUTH)
        username = response.json().get("username")
        response = create_new_user("test_user_111", "user", "test", AUTH, expected_status_code=422)
        assert username == "test_user_111"
        delete_user(username, AUTH)

    def test_create_new_admin(self):
        response = create_new_user("test_user_111", "admin", "test", AUTH)
        username = response.json().get("username")
        assert username == "test_user_111"
        delete_user(username, AUTH)

    def test_create_new_user_as_user(self):
        password = "ttt"
        response = create_new_user("test_user_11", "user", password, AUTH)
        username = response.json().get("username")
        assert username == "test_user_11"

        create_new_user("uuuu", "user", "hdhuhu", (username, password), expected_status_code=403)

        delete_user(username, AUTH)

    def test_delete_user(self):
        response = create_new_user("test_user_111", "user", "test", AUTH)
        username = response.json().get("username")
        assert username == "test_user_111"
        delete_user(username, AUTH)

    def test_delete_user_by_user(self):
        username = "user_tttt{}".format(time.time())
        password = "test_pass"

        create_new_user(username, "user", password, AUTH)
        create_new_user("_1".format(username), "user", password, AUTH)

        delete_user("_1".format(username), (username, password), expected_status=403)

        delete_user("_1".format(username), AUTH)
        delete_user(username, AUTH)

    def test_change_password(self):
        username = "user_{}".format(time.time())
        password = "test_password"
        new_password = "pass"

        create_new_user(username, "user", password, AUTH)
        change_password(username, new_password, (username, password))
        change_password(username, new_password, auth=(username, password), expected_status_code=401)
        change_password(username, password, auth=(username, new_password), expected_status_code=200)
        delete_user(username, AUTH)

    def test_change_password_invalid(self):
        username = "user_{}".format(time.time())
        password = "test_password"
        new_password = "pass"

        create_new_user(username, "user", password, AUTH)
        change_password(username, new_password, auth=AUTH, expected_status_code=403)
        delete_user(username, AUTH)
