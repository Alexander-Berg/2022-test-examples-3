from datetime import datetime, timezone

from search.martylib.db_utils import session_scope
from search.mon.warden.proto.structures import component_pb2

from search.mon.warden.sqla.warden.model import Component, LSR, ActionItem
from search.mon.warden.src.services.reducers.st_importers.action_item_importer import ActionItemImporter
from search.mon.warden.src.services.reducers.st_importers.live_site_review_importer import LSRImporter
from search.mon.warden.tests.utils.base_test_case import BaseTestCase
from search.mon.warden.tests.utils.clients import Clients
from search.mon.warden.tests.utils.clients.startrek import MockTicket, Status, DisplayedValue, Resolution
from search.mon.warden.tests.utils.creators import create_components
from search.mon.warden.tests.utils.startrek import create_ticket_link

CREATED_AT = datetime(2020, 9, 1, 12, tzinfo=timezone.utc)

_clients = Clients()
LSR_IMPORTER = LSRImporter('test_lsr_importer')
AI_IMPORTER = ActionItemImporter('test_lsr_importer')
LSR_IMPORTER.clients = _clients
AI_IMPORTER.clients = _clients
SYNC_DATETIME = datetime(2020, 8, 30).astimezone()
SYNC_TIME = SYNC_DATETIME.strftime('%Y-%m-%d %H:%M:%S')

_LSR_DESCRIPTION = """

===Кого позвать на разбор инцидента
* parsed-login-repeated@
- parsed-login-1@, @parsed-login-2
* кто:not-parsed-1@, staff:parsed-login-3, кого:parsed-login-4, staff:not-parsed-2(LSR), not-parsed-2@s


kucha texta

кого:parsed-login-repeated
"""

_SHOULD_PARSE = set('parsed-login-repeated parsed-login-1 parsed-login-2 parsed-login-3 parsed-login-4'.split())


class TestLiveSiteReviewImporter(BaseTestCase):

    @staticmethod
    def load_to_db():
        create_components(
            component_pb2.CreateComponentRequest(component=component_pb2.Component(slug='test-lsr-importer', abc_service_slug='test-lsr-importer-abc')),
            component_pb2.CreateComponentRequest(component=component_pb2.Component(slug='test-moved-lsr', abc_service_slug='test-lsr-importer-abc-other')),
            component_pb2.CreateComponentRequest(component=component_pb2.Component(slug='zen', abc_service_slug='test-zen')),
        )
        create_components(
            component_pb2.CreateComponentRequest(component=component_pb2.Component(slug='test-lsr-importer__test-lsr-importer-service'))
        )

    def tearDown(self) -> None:
        with session_scope() as session:
            lsrs = session.query(LSR).all()
            for lsr in lsrs:
                session.delete(lsr)

            action_items = session.query(ActionItem).all()
            for ai in action_items:
                session.delete(ai)

    def test_lsr_sync(self):
        lsr_ticket = MockTicket('LSR', CREATED_AT, Status.closed, resolution=Resolution.solved, description=_LSR_DESCRIPTION, components=[DisplayedValue('test-lsr-importer')])
        LSR_IMPORTER.clients.startrek.set_data((lsr_ticket,))
        with session_scope() as session:
            LSR_IMPORTER.sync_issues(session, SYNC_TIME)
            component = session.query(Component).filter(Component.name == 'test-lsr-importer').one_or_none()
            self.assertTrue(bool(component.lsrs))
            for lsr in component.lsrs:
                invited = lsr.invited_to_meeting.split(',')
                invited = [i.strip(' []"') for i in invited]
                self.assertEqual(_SHOULD_PARSE, set(invited))
                self.assertEqual(len(_SHOULD_PARSE), len(invited))

    def test_lsr_not_solved(self):
        lsr_ticket = MockTicket('LSR', CREATED_AT, status=Status.in_work_lsr, components=[DisplayedValue('test-lsr-importer')])
        action_item_ticket = MockTicket('ACTION_ITEM', CREATED_AT, Status.in_work, tags=['lsr:actionitem'])
        create_ticket_link(action_item_ticket, lsr_ticket)
        LSR_IMPORTER.clients.startrek.set_data((action_item_ticket, lsr_ticket))
        with session_scope() as session:
            LSR_IMPORTER.sync_issues(session, SYNC_TIME)
            component = session.query(Component).filter(Component.name == 'test-lsr-importer').one_or_none()
            for lsr in component.lsrs:
                self.assertFalse(lsr.solved)

    def test_lsr_solved(self):
        lsr_ticket = MockTicket('LSR', CREATED_AT, status=Status.closed, resolution=Resolution.solved, components=[DisplayedValue('test-lsr-importer')])
        action_item_ticket = MockTicket('ACTION_ITEM', CREATED_AT, Status.closed, resolution=Resolution.solved, tags=['lsr:actionitem'])
        create_ticket_link(action_item_ticket, lsr_ticket)
        LSR_IMPORTER.clients.startrek.set_data((action_item_ticket, lsr_ticket))
        with session_scope() as session:
            LSR_IMPORTER.sync_issues(session, SYNC_TIME)
            component = session.query(Component).filter(Component.name == 'test-lsr-importer').one_or_none()
            for lsr in component.lsrs:
                self.assertTrue(lsr.solved)

    def test_moved_lsr(self):
        # Do not run this test with "-F TestLiveSiteReviewImporter::*" filter. It won't work. (I don't know why)

        lsr_ticket = MockTicket('LSR', CREATED_AT, Status.closed, components=[DisplayedValue('test-moved-lsr')])
        LSR_IMPORTER.clients.startrek.set_data((lsr_ticket,))
        with session_scope() as session:
            LSR_IMPORTER.sync_issues(session, SYNC_TIME)

            lsr_ticket_moved = MockTicket('LSR', CREATED_AT, Status.closed, components=[DisplayedValue('test-moved-lsr')], aliases=[lsr_ticket.key])
            LSR_IMPORTER.clients.startrek.set_data((lsr_ticket_moved,))
            LSR_IMPORTER.sync_issues(session, SYNC_TIME)
            LSR_IMPORTER.process_moved_lsrs(session, SYNC_DATETIME)
            # have to flush session to save changes from processing moved tickets
            session.commit()

            component = session.query(Component).filter(Component.name == 'test-moved-lsr').one_or_none()
            self.assertEqual(len(component.lsrs), 1)
            for lsr in component.lsrs:
                self.assertEqual(lsr.key, lsr_ticket_moved.key)

            lsr = session.query(LSR).filter(LSR.key == lsr_ticket.key).one_or_none()
            self.assertIsNone(lsr)
