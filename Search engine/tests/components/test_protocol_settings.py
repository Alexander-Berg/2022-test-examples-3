from search.martylib.db_utils import prepare_db, session_scope, clear_db
from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import component_pb2, duty_pb2, owner_pb2
from search.mon.warden.proto.structures.component import protocol_pb2, common_pb2
from search.mon.warden.sqla.warden.model import Component, ProtocolSettings
from search.mon.warden.tests.utils.creators import create_components
from search.mon.warden.src.services.component import ComponentApiService


COMPONENT_API_CLIENT = ComponentApiService()


@TestCase.mock_auth(login='test-user')
def update_settings(request):
    try:
        ComponentApiService().update_protocol_settings(request=request, context=None)
    except Exception as e:
        raise e


@TestCase.mock_auth(login='test-user')
def reset_settings(request):
    try:
        ComponentApiService().reset_protocol_settings(request=request, context=None)
    except Exception as e:
        raise e


CREATE_SETTINGS_REQUEST = protocol_pb2.UpdateProtocolSettingsRequest(
    component=common_pb2.ComponentFilter(component_name='test-protocol-settings', parent_component_name=''),
    settings=protocol_pb2.ProtocolSettings(
        devops_notify_time=100,
        responsible_notify_time=150,
        responsible_notify_ydt_treshold=11.0,
        extra_responsible=common_pb2.LoginList(logins=['dude_one', 'dude_two']),
        allowed_to_close_spi_logins=common_pb2.LoginList(logins=['dude_three']),
        alert_aggregation_type=protocol_pb2.ProtocolSettings.AlertAggregationType.by_vertical,
        not_change_spi_status_automatically=True,
        default_start_message='start',
        infra_environment_id=1,
        infra_service_id=2,
        ticket_age=200,
        assign_on_duty=True,
        add_owner_to_ticket_followers=True,
        coordinator_duty=duty_pb2.DutyRecord(
            duty_rule=duty_pb2.DutyRule(abc=duty_pb2.AbcDutyRule(abc_service='test_service'))
        ),
        not_create_new_chat=True,
    )
)


class TestProtocolSettings(TestCase):

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        prepare_db()

        create_components(
            component_pb2.CreateComponentRequest(component=component_pb2.Component(
                slug='test-protocol-settings',
                owner_list=[owner_pb2.Owner(login='test-user')],
                abc_service_slug='test-protocol-settings-abc'
            )),
        )

    @classmethod
    def tearDownClass(cls):
        clear_db()

    def tearDown(self) -> None:
        with session_scope() as session:
            settings = session.query(ProtocolSettings).all()
            for s in settings:
                component = s.component
                component.protocol_settings = None
                session.delete(s)

    def test_default_settings(self):
        request = protocol_pb2.GetProtocolSettingsRequest(
            component=common_pb2.ComponentFilter(component_name='test-protocol-settings', parent_component_name='')
        )

        response = COMPONENT_API_CLIENT.get_protocol_settings(request, None)

        self.assertEqual(response.component.component_name, 'test-protocol-settings')
        self.assertEqual(response.component.parent_component_name, '')
        self.assertEqual(response.settings, protocol_pb2.ProtocolSettings())

    @TestCase.mock_auth(login='test-user')
    def test_create_settings(self):
        update_settings(CREATE_SETTINGS_REQUEST)

        with session_scope() as session:
            component = session.query(Component).filter(Component.name == 'test-protocol-settings', Component.parent_component_name == '').one_or_none()

            settings: protocol_pb2.ProtocolSettings = component.protocol_settings.to_protobuf()
            self.assertEqual(settings.devops_notify_time, 100)
            self.assertEqual(settings.responsible_notify_time, 150)
            self.assertEqual(settings.responsible_notify_ydt_treshold, 11.0)
            self.assertEqual(settings.extra_responsible, common_pb2.LoginList(logins=['dude_one', 'dude_two']))
            self.assertEqual(settings.allowed_to_close_spi_logins, common_pb2.LoginList(logins=['dude_three']))
            self.assertEqual(settings.alert_aggregation_type, protocol_pb2.ProtocolSettings.AlertAggregationType.by_vertical)
            self.assertTrue(settings.not_change_spi_status_automatically)
            self.assertEqual(settings.default_start_message, 'start')
            self.assertEqual(settings.infra_environment_id, 1)
            self.assertEqual(settings.infra_service_id, 2)
            self.assertEqual(settings.ticket_age, 200)
            self.assertTrue(settings.assign_on_duty)
            self.assertTrue(settings.add_owner_to_ticket_followers)
            self.assertEqual(settings.coordinator_duty.duty_rule.abc.abc_service, 'test_service')
            self.assertTrue(settings.not_create_new_chat)

    @TestCase.mock_auth(login='test-user')
    def test_reset_settings(self):
        request = protocol_pb2.UpdateProtocolSettingsRequest(
            component=common_pb2.ComponentFilter(component_name='test-protocol-settings', parent_component_name=''),
            settings=protocol_pb2.ProtocolSettings(devops_notify_time=100)
        )

        update_settings(request)

        reset_settings(protocol_pb2.GetProtocolSettingsRequest(
            component=common_pb2.ComponentFilter(
                component_name='test-protocol-settings',
                parent_component_name=''
            )
        ))

        with session_scope() as session:
            component = session.query(Component).filter(Component.name == 'test-protocol-settings', Component.parent_component_name == '').one_or_none()
            self.assertIsNone(component.protocol_settings)

    @TestCase.mock_auth(login='test-user')
    def test_update_all_settings(self):
        update_settings(CREATE_SETTINGS_REQUEST)

        request = protocol_pb2.UpdateProtocolSettingsRequest(
            component=common_pb2.ComponentFilter(component_name='test-protocol-settings', parent_component_name=''),
            settings=protocol_pb2.ProtocolSettings(
                devops_notify_time=101,
                responsible_notify_time=151,
                responsible_notify_ydt_treshold=11.1,
                extra_responsible=common_pb2.LoginList(logins=['dude_one', 'dude_two_1']),
                allowed_to_close_spi_logins=common_pb2.LoginList(logins=['dude_three', 'dude_four']),
                alert_aggregation_type=protocol_pb2.ProtocolSettings.AlertAggregationType.by_check,
                not_change_spi_status_automatically=False,
                default_start_message='start_start',
                infra_environment_id=2,
                infra_service_id=3,
                ticket_age=201,
                assign_on_duty=False,
                add_owner_to_ticket_followers=False,
                coordinator_duty=duty_pb2.DutyRecord(
                    duty_rule=duty_pb2.DutyRule(abc=duty_pb2.AbcDutyRule(abc_service='another_test_service'))
                ),
                not_create_new_chat=True,
            )
        )

        update_settings(request)

        with session_scope() as session:
            component = session.query(Component).filter(Component.name == 'test-protocol-settings', Component.parent_component_name == '').one_or_none()

            settings: protocol_pb2.ProtocolSettings = component.protocol_settings.to_protobuf()
            self.assertEqual(settings.devops_notify_time, 101)
            self.assertEqual(settings.responsible_notify_time, 151)
            self.assertEqual(settings.responsible_notify_ydt_treshold, 11.1)
            self.assertEqual(settings.extra_responsible, common_pb2.LoginList(logins=['dude_one', 'dude_two_1']))
            self.assertEqual(settings.allowed_to_close_spi_logins, common_pb2.LoginList(logins=['dude_three', 'dude_four']))
            self.assertEqual(settings.alert_aggregation_type, protocol_pb2.ProtocolSettings.AlertAggregationType.by_check)
            self.assertFalse(settings.not_change_spi_status_automatically)
            self.assertEqual(settings.default_start_message, 'start_start')
            self.assertEqual(settings.infra_environment_id, 2)
            self.assertEqual(settings.infra_service_id, 3)
            self.assertEqual(settings.ticket_age, 201)
            self.assertFalse(settings.assign_on_duty)
            self.assertFalse(settings.add_owner_to_ticket_followers)
            self.assertEqual(settings.coordinator_duty.duty_rule.abc.abc_service, 'another_test_service')
            self.assertTrue(settings.not_create_new_chat)

    @TestCase.mock_auth(login='test-user')
    def test_get_protocol_settings_list(self):
        update_settings(CREATE_SETTINGS_REQUEST)
        response = COMPONENT_API_CLIENT.get_protocol_settings_list(None, None)

        self.assertIsNotNone(response)
        self.assertEqual(len(response.objects), 1)

    @TestCase.mock_auth(login='test-user')
    def test_add_coord_duty(self):
        request = protocol_pb2.UpdateProtocolSettingsRequest(
            component=common_pb2.ComponentFilter(component_name='test-protocol-settings', parent_component_name=''),
            settings=protocol_pb2.ProtocolSettings()
        )
        update_settings(request)

        request = protocol_pb2.UpdateProtocolSettingsRequest(
            component=common_pb2.ComponentFilter(component_name='test-protocol-settings', parent_component_name=''),
            settings=protocol_pb2.ProtocolSettings(
                coordinator_duty=duty_pb2.DutyRecord(
                    duty_rule=duty_pb2.DutyRule(abc=duty_pb2.AbcDutyRule(abc_service='test_service'))
                )
            )
        )
        update_settings(request)

        with session_scope() as session:
            component = session.query(Component).filter(Component.name == 'test-protocol-settings',
                                                        Component.parent_component_name == '').one_or_none()

            settings: protocol_pb2.ProtocolSettings = component.protocol_settings.to_protobuf()
            self.assertEqual(settings.coordinator_duty.duty_rule.abc.abc_service, 'test_service')
