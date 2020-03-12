from device_helpers import *


class TestRGBLamp:
    standard_data = {
        "device_type": "rgb_lamp",
        "device_properties":
            {
                "host": "localhost",
                "port": 5555
            }
    }

    def test_add(self):
        standard_test_add_device(TestRGBLamp.standard_data)

    def test_add_lamp_negative_update_time(self):
        data = {
            "device_type": "rgb_lamp",
            "device_properties":
                {
                    "host": "localhost",
                    "port": 5555,
                    "update_time": -1
                }
        }
        create_device(data, expected_status_code=400)

    def test_add_lamp_more_than_7_days_update_time(self):
        data = {
            "device_type": "rgb_lamp",
            "device_properties":
                {
                    "host": "localhost",
                    "port": 5555,
                    "update_time": 605000
                }
        }
        create_device(data, expected_status_code=400)

    def test_change_update_time(self):
        def _check(device_id):
            devices = get_devices()
            checked = False
            for device in devices:
                if device.get('id') == device_id:
                    checked = True
                    assert device.get("update_time") == 1
            assert checked

        modify_data = {
            "device_properties":
                {
                    "update_time": 1
                }
        }
        standard_test_modify_device(TestRGBLamp.standard_data, modify_data, _check)

    def test_change_update_time_negative(self):

        modify_data = {
            "device_properties":
                {
                    "update_time": -1
                }
        }
        device_id = create_device(TestRGBLamp.standard_data)

        modify_device(device_id, modify_data, expected_status_code=400)

        delete_device(device_id)

    def test_change_update_time_more_than_7_days(self):

        modify_data = {
            "device_properties":
                {
                    "update_time": 605000
                }
        }
        device_id = create_device(TestRGBLamp.standard_data)

        modify_device(device_id, modify_data, expected_status_code=400)

        delete_device(device_id)

    def test_delete(self):
        device_id = create_device(TestRGBLamp.standard_data)
        delete_device(device_id)

    def test_delete_unexists(self):
        delete_device("88888", expected_status_code=404)