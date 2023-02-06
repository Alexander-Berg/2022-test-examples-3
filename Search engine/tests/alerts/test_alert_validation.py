from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures.alert import alert_pb2
from search.mon.warden.src.services.reducers import AlertReducer


class TestAlertValidation(TestCase):

    def test_alert_validation(self):
        test_cases = [
            {
                'alert': alert_pb2.Alert(
                    name='test_solomon_1',
                    url='https://solomon.yandex-team.ru/test',
                    beholder_settings=alert_pb2.BeholderAlertSettings(start_flow=True),
                ),
                'result': 'Create SPI, Start Flow and Create Infra Events are not available for Solomon charts',
            },
            {
                'alert': alert_pb2.Alert(
                    name='test_solomon_2',
                    url='https://solomon.yandex-team.ru/test',
                    beholder_settings=alert_pb2.BeholderAlertSettings(create_spi=True),
                ),
                'result': 'Create SPI, Start Flow and Create Infra Events are not available for Solomon charts',
            },
            {
                'alert': alert_pb2.Alert(
                    name='test_solomon_3',
                    url='https://solomon.yandex-team.ru/test',
                    beholder_settings=alert_pb2.BeholderAlertSettings(create_infra_event=True),
                ),
                'result': 'Create SPI, Start Flow and Create Infra Events are not available for Solomon charts',
            },
            {
                'alert': alert_pb2.Alert(
                    name='test_solomon_4',
                    url='https://solomon.yandex-team.ru/test',
                    beholder_settings=alert_pb2.BeholderAlertSettings(calculate_metric=True),
                ),
                'result': 'Only saved solomon charts are allowed to compute metrics',
            },
            {
                'alert': alert_pb2.Alert(
                    name='test_solomon_5',
                    url='https://solomon.yandex-team.ru/test',
                    beholder_settings=alert_pb2.BeholderAlertSettings(calculate_background_metric=True),
                ),
                'result': 'Only saved solomon charts are allowed to compute metrics',
            },
            {
                'alert': alert_pb2.Alert(
                    name='test_solomon_5',
                    url='https://solomon.yandex-team.ru/admin/projects/noc/test',
                    beholder_settings=alert_pb2.BeholderAlertSettings(calculate_metric=True),
                ),
                'result': '',
            },
            {
                'alert': alert_pb2.Alert(
                    name='test_solomon_5',
                    url='https://solomon.yandex-team.ru/admin/projects/noc/test',
                    beholder_settings=alert_pb2.BeholderAlertSettings(calculate_metric=True),
                ),
                'result': '',
            },
            {
                'alert': alert_pb2.Alert(
                    name='test_solomon_6',
                    url='https://solomon.yandex-team.ru/admin/projects/noc/test',
                    beholder_settings=alert_pb2.BeholderAlertSettings(calculate_background_metric=True, use_weight_function=True),
                ),
                'result': 'Crit threshold for solomon alerts with weight function usage could not be zero',
            },
            {
                'alert': alert_pb2.Alert(
                    name='test_error_abs_1',
                    url='https://solomon.yandex-team.ru/admin/projects/noc/test',
                    beholder_settings=alert_pb2.BeholderAlertSettings(calculate_metric=True),
                    category=alert_pb2.Alert.Category.ERROR_ABS,
                ),
                'result': 'For ERROR_ABS alerts absolute url should be passed via url_abs',
            },
            {
                'alert': alert_pb2.Alert(
                    name='test_error_abs_1',
                    url='https://yasm.yandex-team.ru/alert/test',
                    beholder_settings=alert_pb2.BeholderAlertSettings(calculate_background_metric=True),
                    category=alert_pb2.Alert.Category.ERROR_ABS,
                ),
                'result': 'For ERROR_ABS alerts absolute url should be passed via url_abs',
            },
            {
                'alert': alert_pb2.Alert(
                    name='test_boolean_1',
                    url='https://juggler.yandex-team.ru/alert/test',
                    beholder_settings=alert_pb2.BeholderAlertSettings(calculate_background_metric=True),
                    category=alert_pb2.Alert.Category.BOOLEAN,
                ),
                'result': 'BOOLEAN alerts can not calculate background metric',
            },
        ]

        alert_reducer = AlertReducer()
        for test_case in test_cases:
            validation_result = alert_reducer.validate_alert(test_case['alert'])
            self.assertEqual(validation_result, test_case['result'])
