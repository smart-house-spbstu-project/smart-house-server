from common.helpers import *
from device_helpers import *
import requests
import time

DEVICE_POOL_URL = BASE_URL + "/device_pool"


class TestDevicePool:
    create_data = {
        "device_type": "window"
    }

    def test_create_device_pool(self):
        device_pool_id = create_device(TestDevicePool.create_data, device_url=DEVICE_POOL_URL)
        delete_device(device_pool_id)

    def test_add_and_remove_device(self):
        device_data = {
            "device_type": "window",
            "device_properties": {
                "host": "1.1.1.1",
                "port": 5465
            }
        }

        device_id = create_device(device_data)
        device_pool_id = create_device(TestDevicePool.create_data, DEVICE_POOL_URL)

        add_data = {
            "device_properties": {
                "add": [
                    device_id
                ]
            }
        }

        remove_data = {
            "device_properties": {
                "remove": [
                    device_id
                ]
            }
        }

        modify_device(device_pool_id, add_data)

        devices = get_devices()

        is_ok = True
        for device in devices:
            if device.get('id') == device_pool_id:
                assert device.get('devices')[0] == device_id
        assert is_ok

        modify_device(device_pool_id, remove_data)

        devices = get_devices()

        is_ok = False
        for device in devices:
            if device.get('id') == device_pool_id:
                is_ok = True
                assert len(device.get('devices')) == 0
        assert is_ok

        delete_device(device_id)
        delete_device(device_pool_id)

    def test_fail_to_add_device_with_different_type(self):
        device_data = {
            "device_type": "door",
            "device_properties": {
                "host": "1.1.1.1",
                "port": 5465
            }
        }

        device_id = create_device(device_data)
        device_pool_id = create_device(TestDevicePool.create_data, DEVICE_POOL_URL)

        add_data = {
            "device_properties": {
                "add": [
                    device_id
                ]
            }
        }

        modify_device(device_pool_id, add_data, expected_status_code=422)

        devices = get_devices()

        is_ok = False
        for device in devices:
            if device.get('id') == device_pool_id:
                is_ok = True
                assert len(device.get('devices')) == 0
        assert is_ok

        delete_device(device_id)
        delete_device(device_pool_id)

    def test_add_disconnected_device(self):
        device_data = {
            "device_type": "window",
            "device_properties": {
                "host": "1.1.1.1",
                "port": 5465
            }
        }

        device_id = create_device(device_data)
        disconnect_device(device_id)
        device_pool_id = create_device(TestDevicePool.create_data, DEVICE_POOL_URL)

        add_data = {
            "device_properties": {
                "add": [
                    device_id
                ]
            }
        }

        remove_data = {
            "device_properties": {
                "remove": [
                    device_id
                ]
            }
        }

        modify_device(device_pool_id, add_data)

        devices = get_devices()

        is_ok = True
        for device in devices:
            if device.get('id') == device_pool_id:
                assert device.get('devices')[0] == device_id
            if device.get('id') == device_id:
                assert device.get('status') == "connected"
        assert is_ok

        modify_device(device_pool_id, remove_data)

        devices = get_devices()

        is_ok = False
        for device in devices:
            if device.get('id') == device_pool_id:
                is_ok = True
                assert len(device.get('devices')) == 0
        assert is_ok

        delete_device(device_id)
        delete_device(device_pool_id)

    def test_execute_command(self):
        device_data = {
            "device_type": "window",
            "device_properties": {
                "host": "1.1.1.1",
                "port": 5465
            }
        }

        device_id_1 = create_device(device_data)
        device_id_2 = create_device(device_data)
        device_pool_id = create_device(TestDevicePool.create_data, DEVICE_POOL_URL)

        command_1 = {
            "state" : "off"
        }

        execute_action_on_device(device_id_1, command_1)
        execute_action_on_device(device_id_2, command_1)

        add_data = {
            "device_properties": {
                "add": [
                    device_id_1,
                    device_id_2
                ]
            }
        }

        remove_data = {
            "device_properties": {
                "remove": [
                    device_id_1,
                    device_id_2
                ]
            }
        }

        modify_device(device_pool_id, add_data)

        execute_action_on_device(device_pool_id, {"state": "on"})

        assert get_device(device_id_1).get('data').get('state') == "on"
        assert get_device(device_id_2).get('data').get('state') == "on"

        modify_device(device_pool_id, remove_data)

        delete_device(device_id_1)
        delete_device(device_id_2)
        delete_device(device_pool_id)

    def test_same_functionality(self):
        device_data = {
            "device_type": "rgb_lamp",
            "device_properties": {
                "host": "1.1.1.1",
                "port": 5465
            }
        }

        device_id_1 = create_device(device_data)
        device_id_2 = create_device(device_data)
        device_pool_id = create_device({"device_type": "rgb_lamp"}, DEVICE_POOL_URL)

        add_data = {
            "device_properties": {
                "add": [
                    device_id_1,
                    device_id_2
                ]
            }
        }

        remove_data = {
            "device_properties": {
                "remove": [
                    device_id_1,
                    device_id_2
                ]
            }
        }

        modify_device(device_pool_id, add_data)

        execute_action_on_device(device_pool_id, {"color": "red"})

        assert get_device(device_id_1).get('data').get('color') == "red"
        assert get_device(device_id_2).get('data').get('color') == "red"

        modify_device(device_pool_id, remove_data)

        delete_device(device_id_1)
        delete_device(device_id_2)
        delete_device(device_pool_id)


if __name__ == '__main__':
    TestDevicePool().test_add_and_remove_device()
