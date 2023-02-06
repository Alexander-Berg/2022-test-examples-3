import pytest

from yandextank.plugins.Solomon.solomonsensor import SolomonSensor
from yandextank.plugins.Solomon.solomonreceiver import monitoring_data


sensor_1 = SolomonSensor("", "", "", "", "", [])
sensor_2 = SolomonSensor("", "", "", "", "", ['host'])


class TestSolomon(object):

    @pytest.mark.parametrize(
        'sensor, labels, formated', [
            (
                sensor_1,
                {
                    "cluster": "test",
                    "project": "test",
                    "cluster": "test",
                    "project": "test",
                    "service": "test",
                    "servant": "test",
                    "os": "test",
                    "name": "solomon",
                    "host": "Vla",
                    "disk": "sda1"
                },
                "solomon_Vla_sda1"
            ),
            (
                sensor_1,
                {
                    "cluster": "test",
                    "project": "test",
                    "cluster": "test",
                    "project": "test",
                    "service": "test",
                    "servant": "test",
                    "os": "test",
                    "sensor": "/IO/DiskRead*",
                    "host": "/Vla/",
                    "disk": ".sda/*",
                    "options": "-S.M.A.R.T"
                },
                "-IO-DiskRead*_Vla_sda_S-M-A-R-T"
            ),
            (
                sensor_2,
                {
                    "cluster": "test",
                    "project": "test",
                    "cluster": "test",
                    "project": "test",
                    "service": "test",
                    "servant": "test",
                    "os": "test",
                    "name": "solomon",
                    "host": "Vla",
                    "disk": "sda1"
                },
                "solomon-Vla_sda1"
            ),
            (
                sensor_2,
                {
                    "cluster": "test",
                    "project": "test",
                    "cluster": "test",
                    "project": "test",
                    "service": "test",
                    "servant": "test",
                    "os": "test",
                    "sensor": "/IO/DiskRead*",
                    "host": "/Vla/",
                    "disk": ".sda/*",
                    "options": "-S.M.A.R.T"
                },
                "-IO-DiskRead*-Vla_sda_S-M-A-R-T"
            )
        ]
    )
    def test_format_sensors(self, sensor, labels, formated):
        assert(sensor.format_sensor(labels) == formated)

    @pytest.mark.parametrize(
        'dto, body', [
            (
                {'cluster': "cloud", 'service': "bind_monitor", 'path': "/DiggerCounters/*", 'host': "Sas"},
                '{cluster="cloud", service="bind_monitor", path="/DiggerCounters/*", host="Sas"}'
            ),
            (
                '{cluster="cloud", service="bind_monitor", path="/DiggerCounters/*", host="Sas"}',
                '{cluster="cloud", service="bind_monitor", path="/DiggerCounters/*", host="Sas"}'
            )
        ]
    )
    def test_get_body(self, dto, body):
        assert(sensor_1.get_body(dto) == body)
        assert(sensor_1.get_body(dto) == sensor_2.get_body(dto))

    @pytest.mark.parametrize(
        'metrics, result', [
            (
                {1: {'sens1': 1, 'sens2': 2}},
                {'timestamp': 1, 'data': {'test': {'comment': "", 'metrics': {'custom:sens1': 1, 'custom:sens2': 2}}}}
            )
        ]
    )
    def test_monitoring_data(self, metrics, result):
        assert(monitoring_data("test", metrics, "") == result)
