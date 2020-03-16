from time import sleep

import requests
from common.helpers import *
import json

DEVICE_URL = BASE_URL + "/device"


def create_device(request_data, device_url=DEVICE_URL, expected_status_code=201):
    response = requests.post(device_url, json=request_data, verify=False, auth=AUTH)

    assert response.status_code == expected_status_code, response.json()

    response_data = response.json()
    return response_data.get("id")


def get_devices():
    response = requests.get(DEVICE_URL, auth=AUTH, verify=False)
    assert response.status_code == 200

    return response.json()


def delete_device(device_id, expected_status_code=204):
    delete_response = requests.delete("{}/{}".format(DEVICE_URL, device_id), auth=AUTH, verify=False)
    assert delete_response.status_code == expected_status_code
    if not delete_response.text:
        return {}
    return delete_response.json()


def modify_device(device_id, modify_data, expected_status_code=200):
    response = requests.patch("{}/{}".format(DEVICE_URL, device_id), auth=AUTH, verify=False, json=modify_data)

    assert response.status_code == expected_status_code
    return response.json()


def standard_test_add_device(request_data):
    device_id = create_device(request_data)

    print(device_id, "was created")

    response_array = get_devices()

    assert len(response_array) > 0

    is_ok = False
    for device in response_array:
        if device.get("id") == device_id:
            is_ok = True
            break

    assert is_ok

    delete_device(device_id)


def standard_test_modify_device(request_data, modify_data, check_function):
    device_id = create_device(request_data)

    modify_device(device_id, modify_data)

    check_function(device_id)
    delete_device(device_id)


def connect_device(device_id, expected_status_code=200):
    response = requests.post("{}/{}/connect".format(DEVICE_URL, device_id), auth=AUTH, verify=False)
    assert response.status_code == expected_status_code
    return response.json()


def disconnect_device(device_id, expected_status_code=200):
    response = requests.post("{}/{}/disconnect".format(DEVICE_URL, device_id), auth=AUTH, verify=False)
    assert response.status_code == expected_status_code
    return response.json()


def get_device(device_id, expected_status_code=200):
    response = requests.get("{}/{}".format(DEVICE_URL, device_id), auth=AUTH, verify=False)
    assert response.status_code == expected_status_code
    return response.json()


def reboot_device(device_id, expected_status_code=200):
    r1 = requests.post("{}/{}/reboot".format(DEVICE_URL, device_id), auth=AUTH, verify=False)
    assert r1.status_code == expected_status_code
    return r1.json()


def power_off_device(device_id, expected_status_code=200):
    r1 = requests.post("{}/{}/power_off".format(DEVICE_URL, device_id), auth=AUTH, verify=False)
    assert r1.status_code == expected_status_code
    return r1.json()


def execute_action_on_device(device_id, execute_data, expected_status_code=200):
    r1 = requests.post("{}/{}/execute".format(DEVICE_URL, device_id), auth=AUTH, verify=False, json=execute_data)
    assert r1.status_code == expected_status_code
    return r1.json()


def standard_test_connect_device(create_device_data):
    device_id = create_device(create_device_data)

    disconnect_device(device_id)
    r1 = connect_device(device_id)
    assert r1.get("command_action") == "connected"

    response = get_device(device_id)

    assert response.get("status").get("status") == "Connected"

    delete_device(device_id)
    delete_device(device_id, expected_status_code=404)


def standard_test_disconnect_device(create_device_data):
    device_id = create_device(create_device_data)

    r1 = disconnect_device(device_id)
    assert r1.get("command_action") == "disconnected"

    response = get_devices()
    is_ok = False
    for device in response:
        if device.get('id') == device_id:
            assert device.get("status") in ["disconnected", "error"]
            is_ok = True

    assert is_ok

    delete_device(device_id)
    delete_device(device_id, expected_status_code=404)


def standard_test_reboot_device(create_device_data):
    device_id = create_device(create_device_data)

    r1 = reboot_device(device_id)
    assert r1.get("command_action") == "connected"

    response = get_device(device_id)

    assert response.get("status").get("status") == "Connected"

    delete_device(device_id)
    delete_device(device_id, expected_status_code=404)


def standard_test_power_off_device(create_device_data):
    device_id = create_device(create_device_data)

    power_off_device(device_id)

    response = get_devices()
    is_ok = False
    for device in response:
        if device.get('id') == device_id:
            assert device.get("status") not in ["Connected", "connected"], device
            is_ok = True

    assert is_ok

    delete_device(device_id)
    delete_device(device_id, expected_status_code=404)


def check_execute(literal):
    def ret(device_id, response):
        assert response.get("state") == literal
        assert get_device(device_id).get("data").get("state") == literal

    return ret


def standard_execute_action_device(create_device_data, execute_data, check_funct):
    device_id = create_device(create_device_data)
    response = execute_action_on_device(device_id, execute_data)
    check_funct(device_id, response)
    delete_device(device_id)
    delete_device(device_id, expected_status_code=404)


def standard_metrics_test(create_device_data):
    modify_data = {
        "device_properties":
            {
                "update_time": 1
            }
    }

    device_id = create_device(create_device_data)
    modify_device(device_id, modify_data)

    sleep(120)

    response = requests.get("{}/{}/metrics".format(DEVICE_URL, device_id), auth=AUTH, verify=False)
    assert response.status_code == 200
    array = response.json()

    assert len(array) == 100

    delete_device(device_id)
