from device_helpers import *


class TestLamp:
    standard_data = {
        "device_type": "lamp",
        "device_properties":
            {
                "host": "localhost",
                "port": 5555
            }
    }

    def test_add_lamp(self):
        standard_test_add_device(TestLamp.standard_data)

    def test_add_lamp_negative_update_time(self):
        data = {
            "device_type": "lamp",
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
            "device_type": "lamp",
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
        standard_test_modify_device(TestLamp.standard_data, modify_data, _check)

    def test_change_update_time_negative(self):

        modify_data = {
            "device_properties":
                {
                    "update_time": -1
                }
        }
        device_id = create_device(TestLamp.standard_data)

        modify_device(device_id, modify_data, expected_status_code=400)

        delete_device(device_id)

    def test_change_update_time_more_than_7_days(self):

        modify_data = {
            "device_properties":
                {
                    "update_time": 605000
                }
        }
        device_id = create_device(TestLamp.standard_data)

        modify_device(device_id, modify_data, expected_status_code=400)

        delete_device(device_id)

    def test_delete(self):
        device_id = create_device(TestLamp.standard_data)
        delete_device(device_id)

    def test_delete_unexists(self):
        delete_device("88888", expected_status_code=404)

    def test_connect(self):
        standard_test_connect_device(TestLamp.standard_data)

    def test_disconnect(self):
        standard_test_disconnect_device(TestLamp.standard_data)

    def test_reboot(self):
        standard_test_reboot_device(TestLamp.standard_data)

    def test_reboot_after_disconnect(self):
        device_id = create_device(TestLamp.standard_data)
        disconnect_device(device_id)
        reboot_device(device_id, expected_status_code=503)

    def test_power_off(self):
        standard_test_power_off_device(TestLamp.standard_data)

    def test_power_off_after_disconnect(self):
        device_id = create_device(TestLamp.standard_data)
        disconnect_device(device_id)
        power_off_device(device_id, expected_status_code=503)

    def test_execute_on(self):
        command = {
            "state": "on"
        }
        standard_execute_action_device(TestLamp.standard_data, command, check_execute("on"))

    def test_execute_off(self):
        command = {
            "state": "off"
        }
        standard_execute_action_device(TestLamp.standard_data, command, check_execute("off"))
    def test_metrics(self):
        standard_metrics_test(TestLamp.standard_data)