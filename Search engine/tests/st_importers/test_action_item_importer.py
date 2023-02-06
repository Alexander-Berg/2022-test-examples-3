from datetime import datetime, timezone

from search.martylib.db_utils import prepare_db, session_scope, clear_db
from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import component_pb2
from search.mon.warden.sqla.warden.model import Component, ActionItem, LSR, Incident
from search.mon.warden.src.services.reducers.st_importers.action_item_importer import ActionItemImporter
from search.mon.warden.src.services.reducers.st_importers.incident_importer import SpiIncidentImporter
from search.mon.warden.src.services.reducers.st_importers.live_site_review_importer import LSRImporter
from search.mon.warden.tests.utils.clients import Clients
from search.mon.warden.tests.utils.clients.startrek import MockTicket, Status, DisplayedValue
from search.mon.warden.tests.utils.creators import create_components
from search.mon.warden.tests.utils.startrek import create_ticket_link, delete_ticket_link

CREATED_AT = datetime(2020, 9, 1, 12, tzinfo=timezone.utc)

AI_IMPORTER = ActionItemImporter('test_ai_importer')
SPI_IMPORTER = SpiIncidentImporter('test_ai_importer')
LSR_IMPORTER = LSRImporter('test_ai_importer')

clients = Clients()

AI_IMPORTER.clients = clients
AI_IMPORTER.lsr_importer.clients = clients
AI_IMPORTER.spi_importer.clients = clients
SPI_IMPORTER.clients = clients
LSR_IMPORTER.clients = clients

SYNC_TIME = datetime(2020, 8, 30).astimezone().strftime('%Y-%m-%d %H:%M:%S')


class TestActionItemImporter(TestCase):

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        prepare_db()
        create_components(
            component_pb2.CreateComponentRequest(component=component_pb2.Component(slug='test-ai-importer', abc_service_slug='test-ai-importer-abc')),
            component_pb2.CreateComponentRequest(component=component_pb2.Component(slug='zen', abc_service_slug='test-zen')),
        )
        create_components(
            component_pb2.CreateComponentRequest(component=component_pb2.Component(slug='test-ai-importer__test-ai-importer-service'))
        )

    @classmethod
    def tearDownClass(cls):
        clear_db()

    def tearDown(self) -> None:
        with session_scope() as session:
            incidents = session.query(Incident).all()
            for incident in incidents:
                session.delete(incident)

            action_items = session.query(ActionItem).all()
            for action_item in action_items:
                session.delete(action_item)

            lsrs = session.query(LSR).all()
            for lsr in lsrs:
                session.delete(lsr)

    def test_ai_sync(self):
        # Set up test data
        incident_ticket = MockTicket('SPI', CREATED_AT, Status.in_work, components=[DisplayedValue('test-ai-importer')])
        AI_IMPORTER.clients.startrek.set_data((incident_ticket,))
        with session_scope() as session:
            SPI_IMPORTER.sync_issues(session, SYNC_TIME)
            component = session.query(Component).filter(Component.name == 'test-ai-importer').one_or_none()
            for incident in component.incidents:
                self.assertFalse(incident.solved)

        action_item_ticket = MockTicket('ACTION_ITEM', CREATED_AT, Status.closed, tags=['spi:actionitem'])
        create_ticket_link(action_item_ticket, incident_ticket)
        AI_IMPORTER.clients.startrek.set_data((action_item_ticket, incident_ticket))
        with session_scope() as session:
            AI_IMPORTER.sync_issues(session, SYNC_TIME)
            component = session.query(Component).filter(Component.name == 'test-ai-importer').one_or_none()
            for incident in component.incidents:
                self.assertTrue(incident.solved)
                self.assertEqual(len(incident.action_items), 1)

    def test_moved_action_items(self):
        incident_ticket = MockTicket('SPI', CREATED_AT, Status.in_work, components=[DisplayedValue('test-ai-importer')])
        action_item_ticket = MockTicket('ACTION_ITEM', CREATED_AT, Status.closed, tags=['spi:actionitem'])
        create_ticket_link(incident_ticket, action_item_ticket)
        AI_IMPORTER.clients.startrek.set_data((incident_ticket, action_item_ticket))
        with session_scope() as session:
            SPI_IMPORTER.sync_issues(session, SYNC_TIME)
            AI_IMPORTER.sync_issues(session, SYNC_TIME)

            action_item_ticket_moved = MockTicket('ACTION_ITEM', CREATED_AT, Status.closed, tags=['spi:actionitem'], aliases=[action_item_ticket.key])
            create_ticket_link(incident_ticket, action_item_ticket_moved)
            delete_ticket_link(incident_ticket, action_item_ticket)
            AI_IMPORTER.clients.startrek.set_data((action_item_ticket_moved, incident_ticket))

            AI_IMPORTER.sync_issues(session, SYNC_TIME)

            component = session.query(Component).filter(Component.name == 'test-ai-importer').one_or_none()
            for incident in component.incidents:
                self.assertEqual(len(incident.action_items), 1)
                for action_item in incident.action_items:
                    self.assertEqual(action_item.key, action_item_ticket_moved.key)

            action_item = session.query(ActionItem).filter(ActionItem.key == action_item_ticket.key).one_or_none()
            self.assertIsNone(action_item)

    def test_lsr_relations(self):
        lsr_ticket = MockTicket('LSR', CREATED_AT, Status.in_work_lsr, components=[DisplayedValue('test-ai-importer')])
        action_item_ticket = MockTicket('ACTION_ITEM', CREATED_AT, Status.closed, tags=['lsr:actionitem'])
        create_ticket_link(lsr_ticket, action_item_ticket)
        clients.startrek.set_data((lsr_ticket, action_item_ticket))

        self.assertTrue(isinstance(AI_IMPORTER.lsr_importer.clients, Clients))
        with session_scope() as session:
            LSR_IMPORTER.sync_issues(session, SYNC_TIME)
            AI_IMPORTER.sync_issues(session, SYNC_TIME)

            component = session.query(Component).filter(Component.name == 'test-ai-importer').one_or_none()
            self.assertEqual(len(component.lsrs), 1)
            for lsr in component.lsrs:  # type: LSR
                self.assertEqual(len(lsr.action_items), 1)
