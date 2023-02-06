from datetime import datetime, timezone

from search.martylib.core.logging_utils import configure_binlog
from search.martylib.db_utils import session_scope
from search.mon.warden.proto.structures import component_pb2, functionality_pb2
from search.mon.warden.proto.structures.alert import alert_pb2, message_pb2 as alert_message_pb2
from search.mon.warden.src import const

from search.mon.warden.sqla.warden.model import Component, Incident, Alert
from search.mon.warden.src.services.reducers.st_importers.incident_importer import SpiIncidentImporter
from search.mon.warden.tests.utils.base_test_case import BaseTestCase
from search.mon.warden.tests.utils.clients import Clients
from search.mon.warden.tests.utils.clients.startrek import MockTicket, Status, DisplayedValue, Resolution
from search.mon.warden.tests.utils.creators import create_components, create_functionalities, create_alerts
from search.mon.warden.tests.utils.startrek import create_ticket_link

CREATED_AT = datetime(2020, 9, 1, 12, tzinfo=timezone.utc)

configure_binlog(
    'warden',
    loggers=const.LOGGERS,
    stdout=True,
)

IMPORTER = SpiIncidentImporter('test_spi_importer')
IMPORTER.clients = Clients()
SYNC_TIME = datetime(2020, 8, 30).astimezone().strftime('%Y-%m-%d %H:%M:%S')


class TestSPIImporter(BaseTestCase):

    @staticmethod
    def load_to_db():
        create_components(
            component_pb2.CreateComponentRequest(component=component_pb2.Component(slug='test-spi-importer', abc_service_slug='test-spi-importer-abc')),
            component_pb2.CreateComponentRequest(component=component_pb2.Component(slug='test-spi-importer-2', abc_service_slug='test-spi-importer-abc-2')),
            component_pb2.CreateComponentRequest(component=component_pb2.Component(slug='test-spi-importer-3', abc_service_slug='test-spi-importer-abc-3')),
            component_pb2.CreateComponentRequest(component=component_pb2.Component(slug='test-spi-importer-4', abc_service_slug='test-spi-importer-abc-4')),
            component_pb2.CreateComponentRequest(component=component_pb2.Component(slug='zen', abc_service_slug='test-zen')),
        )
        create_components(
            component_pb2.CreateComponentRequest(component=component_pb2.Component(slug='test-spi-importer__test-spi-importer-service'))
        )

    def tearDown(self) -> None:
        with session_scope() as session:
            incidents = session.query(Incident).all()
            for incident in incidents:
                session.delete(incident)

    def test_spi_sync(self):
        # Set up test data
        component_ticket = MockTicket('SPI', CREATED_AT, Status.closed, components=[DisplayedValue('test-spi-importer')])
        non_component_ticket = MockTicket('SPI', CREATED_AT, Status.other)
        IMPORTER.clients.startrek.set_data((component_ticket, non_component_ticket))

        with session_scope() as session:
            IMPORTER.sync_issues(session, datetime(2020, 8, 30).astimezone().strftime('%Y-%m-%d %H:%M:%S'))

            component = session.query(Component).filter(Component.name == 'test-spi-importer').one_or_none()
            self.assertIsNotNone(component)
            self.assertEqual(len(component.incidents), 1)

            incidents = session.query(Incident).all()
            self.assertEqual(len(incidents), 2)
            for incident in incidents:
                if incident.key == component_ticket.key:
                    self.assertEqual(incident.status, Status.closed.value)
                else:
                    self.assertEqual(incident.key, non_component_ticket.key)
                    self.assertEqual(incident.status, Status.other.value)

    def test_victim_ydt_calculation(self):
        # Set up test data
        umbrella_ticket = MockTicket('SPI', CREATED_AT, Status.closed, components=[DisplayedValue('test-spi-importer')], tags=['spi:umbrella'])
        spi_victim_1 = MockTicket('SPI', CREATED_AT, Status.closed, components=[DisplayedValue('test-spi-importer-2')], tags=['spi:victim'], ydt=10)
        spi_victim_2 = MockTicket('SPI', CREATED_AT, Status.closed, components=[DisplayedValue('test-spi-importer-3')], tags=['spi:victim'], ydt=15)
        create_ticket_link(umbrella_ticket, spi_victim_1)
        create_ticket_link(umbrella_ticket, spi_victim_2)

        IMPORTER.clients.startrek.set_data((umbrella_ticket, spi_victim_1, spi_victim_2))

        with session_scope() as session:
            IMPORTER.sync_issues(session, datetime(2020, 8, 30).astimezone().strftime('%Y-%m-%d %H:%M:%S'))
            component = session.query(Component).filter(Component.name == 'test-spi-importer').one_or_none()
            self.assertEqual(len(component.incidents), 1)
            for incident in component.incidents:
                if incident.key == umbrella_ticket.key:
                    self.assertEqual(incident.yandex_downtime, 25)

    def _validate_component_incident_exist(self, component: Component, incident: MockTicket):
        self.assertEqual(len(component.incidents), 1)
        for incident_model in component.incidents:
            self.assertEqual(incident_model.key, incident.key)

    def test_component_and_service_links(self):
        # Set up test data
        service_incident = MockTicket(
            'SPI',
            CREATED_AT,
            Status.closed,
            components=[DisplayedValue('test-spi-importer')],
            tags=['service:test-spi-importer-service'],
        )
        IMPORTER.clients.startrek.set_data((service_incident,))
        with session_scope() as session:
            IMPORTER.sync_issues(session, SYNC_TIME)
            component = session.query(Component).filter(Component.name == 'test-spi-importer').one_or_none()
            service = (
                session.query(Component)
                .filter(Component.name == 'test-spi-importer-service', Component.parent_component_name == 'test-spi-importer')
                .one_or_none()
            )
            self._validate_component_incident_exist(component, service_incident)
            self._validate_component_incident_exist(service, service_incident)

    def test_incident_solved(self):
        # Solved by action items and status
        solved_incident = MockTicket('SPI', CREATED_AT, Status.in_work, components=[DisplayedValue('test-spi-importer')])
        action_item = MockTicket('ACTIONITEM', CREATED_AT, Status.other, tags=['spi:actionitem'])
        create_ticket_link(solved_incident, action_item)

        # Solved by resolution
        solved_incident_2 = MockTicket('SPI', CREATED_AT, Status.closed, resolution=Resolution.not_appear, components=[DisplayedValue('test-spi-importer')])

        # Solved by resolution and links
        solved_incident_3 = MockTicket('SPI', CREATED_AT, Status.closed, resolution=Resolution.duplicated, components=[DisplayedValue('test-spi-importer')])
        duplicate_incident = MockTicket('SPI', CREATED_AT, Status.closed)
        create_ticket_link(solved_incident_3, duplicate_incident)

        solved_incident_3 = MockTicket('SPI', CREATED_AT, Status.closed, resolution=Resolution.relapse, components=[DisplayedValue('test-spi-importer')])
        create_ticket_link(solved_incident_3, duplicate_incident)

        # Solved by resolution and ai or spproblem
        solved_incident_4 = MockTicket('SPI', CREATED_AT, Status.closed, resolution=Resolution.solved, components=[DisplayedValue('test-spi-importer')])
        create_ticket_link(solved_incident_4, action_item)

        solved_incident_5 = MockTicket('SPI', CREATED_AT, Status.closed, resolution=Resolution.solved, components=[DisplayedValue('test-spi-importer')])
        spproblem_ticket = MockTicket('SPPROBLEM', CREATED_AT, Status.other)
        create_ticket_link(solved_incident_5, spproblem_ticket)

        IMPORTER.clients.startrek.set_data(
            (solved_incident, solved_incident_2, solved_incident_3, solved_incident_4, solved_incident_5, action_item, duplicate_incident, spproblem_ticket),
        )
        with session_scope() as session:
            IMPORTER.sync_issues(session, SYNC_TIME)
            component = session.query(Component).filter(Component.name == 'test-spi-importer').one_or_none()
            for incident in component.incidents:
                self.assertTrue(incident.solved)

    def test_not_solved_incident(self):
        not_solved_incident = MockTicket('SPI', CREATED_AT, Status.in_work, components=[DisplayedValue('test-spi-importer')])
        not_solved_incident_2 = MockTicket('SPI', CREATED_AT, Status.other, components=[DisplayedValue('test-spi-importer')])
        not_solved_incident_3 = MockTicket('SPI', CREATED_AT, Status.closed, resolution=Resolution.duplicated, components=[DisplayedValue('test-spi-importer')])
        not_solved_incident_4 = MockTicket('SPI', CREATED_AT, Status.closed, resolution=Resolution.relapse, components=[DisplayedValue('test-spi-importer')])
        not_solved_incident_5 = MockTicket('SPI', CREATED_AT, Status.closed, resolution=Resolution.solved, components=[DisplayedValue('test-spi-importer')])

        IMPORTER.clients.startrek.set_data((not_solved_incident, not_solved_incident_2, not_solved_incident_3, not_solved_incident_4, not_solved_incident_5))
        with session_scope() as session:
            IMPORTER.sync_issues(session, SYNC_TIME)
            component = session.query(Component).filter(Component.name == 'test-spi-importer').one_or_none()
            for incident in component.incidents:
                self.assertFalse(incident.solved)

    def test_spi_is_critical(self):
        minilsr = MockTicket('MINILSR', CREATED_AT, Status.in_work, components=[DisplayedValue('test-spi-importer')])
        incident = MockTicket('SPI', CREATED_AT, Status.in_work)
        create_ticket_link(minilsr, incident)
        IMPORTER.clients.startrek.set_data((minilsr, incident))
        self.assertTrue(IMPORTER.is_lsr_candidate(incident))

    def test_import_alerts_relations(self):
        functionality_ids = create_functionalities(
            functionality_pb2.AddFunctionalityRequest(
                component_name='test-spi-importer-4', functionality=functionality_pb2.Functionality(name='test-importer', weight=1)
            ),
        )
        alert_ids = create_alerts(alert_message_pb2.AddAlertRequest(functionality_id=functionality_ids[0], alert=alert_pb2.Alert(name='test-alert')))
        issue = MockTicket('SPI', CREATED_AT, Status.in_work, components=[DisplayedValue('test-spi-importer')], wardenAlerts=alert_ids)

        IMPORTER.clients.startrek.set_data((issue, ))
        with session_scope() as session:
            IMPORTER.sync_issues(session, SYNC_TIME)
            alert = session.query(Alert).filter(Alert.id == alert_ids[0]).one()
            self.assertEqual(alert.name, 'test-alert')
            component = session.query(Component).filter(Component.name == 'test-spi-importer').one_or_none()
            for incident in component.incidents:
                self.assertEqual(set(str(i.id) for i in incident.alerts), set(alert_ids))
