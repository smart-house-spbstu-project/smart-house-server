import requests
from common.helpers import *

DEVICE_URL = BASE_URL + "/device"


def create_device(request_data, expected_status_code=201):
    response = requests.post(DEVICE_URL, json=request_data, verify=False, auth=AUTH)

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

    return delete_response


def modify_device(device_id, modify_data, expected_status_code=200):
    response = requests.patch("{}/{}".format(DEVICE_URL, device_id), auth=AUTH, verify=False, json=modify_data)

    assert response.status_code == expected_status_code
    return response


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
