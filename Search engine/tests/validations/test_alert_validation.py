import uuid
from datetime import timedelta

from search.martylib.core.date_utils import now
from search.martylib.db_utils import session_scope, clear_db, prepare_db
from search.martylib.http.exceptions import BadRequest
from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import component_pb2, functionality_pb2, component_check_pb2
from search.mon.warden.proto.structures.alert import alert_pb2, message_pb2 as alert_message_pb2
from search.mon.warden.src import const, utils
from search.mon.warden.src.services.reducers.validators import alert as v_alert
from search.mon.warden.src.workers.checks import ChecksWorker
from search.mon.warden.sqla.warden.model import Component, Alert
from search.mon.warden.tests.utils.creators import create_components, create_functionalities, create_alerts

YASM_URL_BASE = 'https://yasm.yandex-team.ru/alert/'
YASM_TEMPLATE_NAME = 'warden_alerts_for_testing.'
SOLOMON_URL_BASE = 'https://solomon.yandex-team.ru/'
JUGGLER_URL_BASE = 'https://juggler.yandex-team.ru/check_details/?'
SOLOMON_URL = SOLOMON_URL_BASE + '?project=trust_cashregisters&cluster=greed_prod'


def juggler_url(host, service):
    return JUGGLER_URL_BASE + f'host={host}&service={service}'


def yasm_url(name):
    return YASM_URL_BASE + YASM_TEMPLATE_NAME + name


class TestWardenValidateAlert(TestCase):
    maxDiff = None

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        prepare_db()
        utils.create_locks()
        utils.setup_metrics()

    @classmethod
    def tearDownClass(cls):
        clear_db()

    def test_validate_yasm_alert(self):
        test_cases = [
            {
                'alert': alert_pb2.Alert(
                    name='alert 1',
                    url=yasm_url('warden_testing_startflow'),
                    id=uuid.uuid4().__str__(),
                    validated=True,
                    beholder_settings=alert_pb2.BeholderAlertSettings(
                        create_spi=False,
                        start_flow=True,
                    )),
                'excepted_errors': {const.TO_START_FLOW_NEED_CREATE_SPI, },
                'message': 'Impossible to start flow without creating SPI'
            },
            {
                'alert': alert_pb2.Alert(
                    name='alert 2',
                    url=yasm_url('warden_testing_without_juggler'),
                    id=uuid.uuid4().__str__(),
                    validated=True,
                    beholder_settings=alert_pb2.BeholderAlertSettings(
                        create_spi=True,
                        start_flow=False
                    )),
                'excepted_errors': {const.YASM_NO_LINKED_JUGGLER_CHECK, },
                'message': 'Need linked juggler check'
            },
            {
                'alert': alert_pb2.Alert(
                    name='alert 3',
                    url=yasm_url('warden_testing_createspi_startflow_without_tags'),
                    id=uuid.uuid4().__str__(),
                    validated=True,
                    beholder_settings=alert_pb2.BeholderAlertSettings(
                        create_spi=True,
                        start_flow=True
                    )
                ),
                'excepted_errors': {const.CHECKS_MUST_HAVE_TAGS_CREATESPI_AND_START_FLOW, },
                'message': 'Need tags warden_alert_create_spi and warden_alert_start_flow'
            },
            {
                'alert': alert_pb2.Alert(
                    name='alert 4',
                    id=uuid.uuid4().__str__(),
                    validated=True,
                    url=yasm_url('warden_testing_createspi_startflow'),
                    beholder_settings=alert_pb2.BeholderAlertSettings(
                        create_spi=True,
                        start_flow=True,
                    )
                ),
                'excepted_errors': set(),
                'message': 'All ok 1'
            },
            {
                'alert': alert_pb2.Alert(
                    name='alert 5',
                    id=uuid.uuid4().__str__(),
                    validated=True,
                    url=yasm_url('warden_testing_createspi'),
                    beholder_settings=alert_pb2.BeholderAlertSettings(
                        create_spi=True,
                    )
                ),
                'excepted_errors': set(),
                'message': 'All ok 2'
            },
            {
                'alert': alert_pb2.Alert(
                    name='alert 6',
                    url=yasm_url('warden_testing_donothing'),
                    id=uuid.uuid4().__str__(),
                    validated=True,
                    beholder_settings=alert_pb2.BeholderAlertSettings(
                        create_spi=False,
                        start_flow=False
                    )
                ),
                'excepted_errors': set(),
                'message': 'All ok 3'
            },
            {
                'alert': alert_pb2.Alert(
                    name='alert 7',
                    url=yasm_url('warden_testing_deleted_juggler'),
                    id=uuid.uuid4().__str__(),
                    validated=True,
                    beholder_settings=alert_pb2.BeholderAlertSettings(
                        create_spi=True,
                    )
                ),
                'excepted_errors': {const.YASM_NO_LINKED_JUGGLER_CHECK, },
                'message': 'Juggler check was deleted'
            }
        ]

        validator = v_alert.AlertValidator()
        for test_case in test_cases:
            res = set(validator._validate_yasm_alert(test_case['alert']))
            if res != test_case['excepted_errors']:
                raise BadRequest(f"{test_case['message']} failed. Excepted {test_case['excepted_errors']}, got {res}")

    def test_validate_juggler_alert(self):
        test_cases = [
            {
                'alert': alert_pb2.Alert(
                    name='check 1',
                    url=juggler_url('warden_test_juggler_alert',
                                    'warden_testing_createspi_startflow_only_juggler_failed'),
                    id=uuid.uuid4().__str__(),
                    validated=True,
                    beholder_settings=alert_pb2.BeholderAlertSettings(
                        create_spi=True,
                        start_flow=True
                    ),
                ),
                'excepted_errors': {const.CHECKS_MUST_HAVE_TAGS_CREATESPI_AND_START_FLOW, },
                'message': 'Need tags warden_alert_create_spi and warden_alert_start_flow'
            },
            {
                'alert': alert_pb2.Alert(
                    name='check 2',
                    url=juggler_url('warden_test_juggler_alert',
                                    'warden_testing_calculate_metric_juggler_failed'),
                    id=uuid.uuid4().__str__(),
                    validated=True,
                    beholder_settings=alert_pb2.BeholderAlertSettings(
                        calculate_metric=True,
                        calculate_background_metric=True
                    ),
                ),
                'excepted_errors': {const.JUGGLER_CHECKS_HAVE_YASM_ALERT, },
                'message': 'Need linked yasm to calculate metric'
            },
            {
                'alert': alert_pb2.Alert(
                    name='check 3',
                    url=juggler_url('warden_test_alert',
                                    'warden_testing_calculate_metric_juggler'),
                    id=uuid.uuid4().__str__(),
                    validated=True,
                    beholder_settings=alert_pb2.BeholderAlertSettings(
                        calculate_metric=True,
                        calculate_background_metric=True
                    )),
                'excepted_errors': set(),
                'message': 'All ok 4'
            },
            {
                'alert': alert_pb2.Alert(
                    name='check 4',
                    url=juggler_url('warden_test_juggler_alert',
                                    'warden_testing_createspi_startflow_only_juggler'),
                    id=uuid.uuid4().__str__(),
                    validated=True,
                    beholder_settings=alert_pb2.BeholderAlertSettings(
                        create_spi=True,
                        start_flow=True
                    ),
                ),
                'excepted_errors': set(),
                'message': 'All ok 5'
            },
            {
                'alert': alert_pb2.Alert(
                    name='check 5',
                    url=juggler_url('warden_test_juggler_alert',
                                    'warden_testing_createspi_with_push_to_beholder'),
                    id=uuid.uuid4().__str__(),
                    validated=True,
                    beholder_settings=alert_pb2.BeholderAlertSettings(
                        create_spi=True,
                        start_flow=True
                    ),
                ),
                'excepted_errors': set(),
                'message': 'All ok 6'
            },
        ]

        validator = v_alert.AlertValidator()
        for test_case in test_cases:
            res = set(validator._validate_juggler_alert(test_case['alert']))
            if res != test_case['excepted_errors']:
                raise BadRequest(f"{test_case['message']} failed. Excepted {test_case['excepted_errors']}, got {res}")

    def test_validate_solomon_alert(self):
        test_cases = [
            {
                'alert': alert_pb2.Alert(
                    name='Solomon 1',
                    url=SOLOMON_URL,
                    id=uuid.uuid4().__str__(),
                    validated=True,
                    beholder_settings=alert_pb2.BeholderAlertSettings(
                        create_spi=True,
                        start_flow=True
                    )
                ),
                'excepted_errors': {const.SOLOMON_ALERT_CANNOT_CREATESPI_AND_START_FLOW, },
                'message': 'Impossible to create SPI with Solomon'
            },
            {
                'alert': alert_pb2.Alert(
                    name='Solomon 2',
                    url=SOLOMON_URL,
                    id=uuid.uuid4().__str__(),
                    validated=True,
                    beholder_settings=alert_pb2.BeholderAlertSettings(
                        calculate_metric=True,
                        calculate_background_metric=True
                    )
                ),
                'excepted_errors': set(),
                'message': 'All ok 7'
            }
        ]
        validator = v_alert.AlertValidator()
        for test_case in test_cases:
            res = set(validator._validate_solomon_alert(test_case['alert']))
            if res != set(test_case['excepted_errors']):
                raise BadRequest(f"{test_case['message']} failed. Excepted {test_case['excepted_errors']}, got {res}")

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_alert_and_component_validation(self):
        create_components(
            component_pb2.CreateComponentRequest(
                component=component_pb2.Component(
                    slug='test_component_af_2',
                    abc_service_slug='test_component_af_2',
                )
            )
        )
        created_functionality_list = create_functionalities(
            functionality_pb2.AddFunctionalityRequest(
                component_name='test_component_af_2',
                functionality=functionality_pb2.Functionality(
                    name='test_functionality',
                    slug='test_component_af_2_test_functionality'
                )
            )
        )
        created_alerts = create_alerts(
            alert_message_pb2.AddAlertRequest(
                functionality_id=created_functionality_list[0],
                alert=alert_pb2.Alert(
                    name='alert 4',
                    id=uuid.uuid4().__str__(),
                    validated=True,
                    url=juggler_url('warden_test_juggler_alert', 'warden_testing_createspi_startflow_only_juggler'),
                    beholder_settings=alert_pb2.BeholderAlertSettings(
                        create_spi=True,
                        start_flow=True,
                    )
                ),
            )
        )
        checks_worker = ChecksWorker()
        checks_worker.run_once()
        with session_scope() as session:
            component = session.query(Component).filter(Component.name == 'test_component_af_2').one()
            component_proto = component.to_protobuf()
            for check in component_proto.component_checks:
                if check.check_type == component_check_pb2.ComponentCheck.CheckType.ALERT:
                    self.assertEqual(check.status, component_check_pb2.ComponentCheck.Status.OK)
            self.assertEqual(component_proto.state, component_pb2.Component.State.INVALID)

            alert = session.query(Alert).filter(Alert.id == created_alerts[0]).one()
            alert.url = JUGGLER_URL_BASE
            component.validation_time = int(now().timestamp() - timedelta(days=1).total_seconds())

        checks_worker.run_once()
        with session_scope() as session:
            component = session.query(Component).filter(Component.name == 'test_component_af_2').one()
            component_proto = component.to_protobuf()
            for check in component_proto.component_checks:
                if check.check_type == component_check_pb2.ComponentCheck.CheckType.ALERT:
                    if check.name == const.INCORRECT_ALERT_URL:
                        self.assertEqual(check.status, component_check_pb2.ComponentCheck.Status.ERROR)
                    else:
                        self.assertEqual(check.status, component_check_pb2.ComponentCheck.Status.OK)

    def test_get_host_and_service_of_juggler_alerts(self):
        test_cases = [
            {
                'alert': alert_pb2.Alert(
                    name='check 1',
                    type=alert_pb2.Alert.Type.JUGGLER,
                    url=juggler_url('host',
                                    'service'),
                    id=uuid.uuid4().__str__(),
                ),
                'expected_host': 'host',
                'expected_service': 'service'
            },
            {
                'alert': alert_pb2.Alert(
                    name='check 2',
                    type=alert_pb2.Alert.Type.JUGGLER,
                    url=JUGGLER_URL_BASE + 'host=test2',
                    id=uuid.uuid4().__str__(),
                ),
                'expected_host': None,
                'expected_service': None
            },
            {
                'alert': alert_pb2.Alert(
                    name='check 3',
                    type=alert_pb2.Alert.Type.JUGGLER,
                    url=JUGGLER_URL_BASE + 'service=test3',
                    id=uuid.uuid4().__str__(),
                ),
                'expected_host': None,
                'expected_service': None
            },
            {
                'alert': alert_pb2.Alert(
                    name='check 4',
                    type=alert_pb2.Alert.Type.JUGGLER,
                    url=JUGGLER_URL_BASE + 'host=test4a&service=test4b&param=empty',
                    id=uuid.uuid4().__str__(),
                ),
                'expected_host': 'test4a',
                'expected_service': 'test4b'
            },
            {
                'alert': alert_pb2.Alert(
                    name='check 5',
                    type=alert_pb2.Alert.Type.JUGGLER,
                    url='invalid_url/?host=&service=',
                    id=uuid.uuid4().__str__(),
                ),
                'expected_host': None,
                'expected_service': None
            }
        ]

        alert_validator = v_alert.AlertValidator()
        for test in test_cases:
            host, service = alert_validator.get_host_and_service_of_alert(test['alert'])
            self.assertEqual(host, test['expected_host'])
            self.assertEqual(service, test['expected_service'])

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_duplicated_juggler_alerts_with_flags(self):
        create_components(
            component_pb2.CreateComponentRequest(
                component=component_pb2.Component(
                    slug='test_duplication1',
                    abc_service_slug='test_duplication1',
                )
            )
        )
        func1 = create_functionalities(
            functionality_pb2.AddFunctionalityRequest(
                component_name='test_duplication1',
                functionality=functionality_pb2.Functionality(
                    name='test_duplication1_f1',
                    slug='test_duplication1_test_duplication1_f1',
                )
            )
        )

        func2 = create_functionalities(
            functionality_pb2.AddFunctionalityRequest(
                component_name='test_duplication1',
                functionality=functionality_pb2.Functionality(
                    name='test_duplication1_f2',
                    slug='test_duplication1_test_duplication1_f2',
                )
            )
        )

        func3 = create_functionalities(
            functionality_pb2.AddFunctionalityRequest(
                component_name='test_duplication1',
                functionality=functionality_pb2.Functionality(
                    name='test_duplication1_f3',
                    slug='test_duplication1_test_duplication1_f3',
                )
            )
        )

        alert1 = create_alerts(
            alert_message_pb2.AddAlertRequest(
                functionality_id=func1[0],
                alert=alert_pb2.Alert(
                    name='alert_1',
                    id=uuid.uuid4().__str__(),
                    type=alert_pb2.Alert.Type.JUGGLER,
                    validated=False,
                    url=juggler_url('warden_test_juggler_alert', 'warden_testing_createspi_startflow_only_juggler'),
                    beholder_settings=alert_pb2.BeholderAlertSettings(
                        create_spi=True,
                        start_flow=True,
                    )
                ),
            ))[0]

        _ = create_alerts(
            alert_message_pb2.AddAlertRequest(
                functionality_id=func2[0],
                alert=alert_pb2.Alert(
                    name='alert_2',
                    id=uuid.uuid4().__str__(),
                    validated=False,
                    type=alert_pb2.Alert.Type.JUGGLER,
                    url=juggler_url('warden_test_juggler_alert', 'warden_testing_createspi_startflow_only_juggler'),
                    beholder_settings=alert_pb2.BeholderAlertSettings(
                        create_spi=False,
                        start_flow=False,
                    )
                )
            )
        )[0]

        alert3 = create_alerts(
            alert_message_pb2.AddAlertRequest(
                functionality_id=func3[0],
                alert=alert_pb2.Alert(
                    name='alert_3',
                    id=uuid.uuid4().__str__(),
                    validated=False,
                    type=alert_pb2.Alert.Type.JUGGLER,
                    url=juggler_url('warden_test_juggler_alert', 'warden_testing_createspi_startflow_only_juggler'),
                    beholder_settings=alert_pb2.BeholderAlertSettings(
                        create_spi=True,
                        start_flow=False,
                    )
                )
            )
        )[0]

        checks_worker = ChecksWorker()
        checks_worker.run_once()

        with session_scope() as session:
            component = session.query(Component).filter(Component.name == 'test_duplication1').one()
            self.assertIsNotNone(component)
            component_proto = component.to_protobuf()
            checks = component_proto.component_checks
            duplication_checks = [check for check in checks if check.check_type == component_check_pb2.ComponentCheck.CheckType.ALERT and check.name == const.DUPLICATED_ALERTS]
            self.assertEqual(len(duplication_checks), 1)
            duplication_check = duplication_checks[0]
            self.assertEqual(set(duplication_check.causes.alert_ids), {alert1, alert3})
