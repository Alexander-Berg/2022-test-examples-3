import random
from datetime import timezone, datetime

from search.martylib.db_utils import prepare_db, session_scope, clear_db
from search.martylib.db_utils import to_model, generate_field_name as F
from search.martylib.test_utils import TestCase
from search.mon.warden.proto.structures import component_pb2, lsr_pb2, incident_pb2

from search.mon.warden.sqla.warden.model import Incident, LSR
from search.mon.warden.src.services.lsr import LSRService
from search.mon.warden.tests.utils.creators import create_components

REDUCER = LSRService()
CREATED_AT = datetime(2020, 9, 1, 12, tzinfo=timezone.utc)
COMPONENT = 'test-calendar-reducer'
INVITED = ['test-login1', 'test-login2']
LSR_KEY = 'LSR-0'
INCIDENT_KEY = 'SPI-0'
STATUS = 'В работе'
SUMMARY = 'Название тикета'

LSR_AVAILABLE_FOR_UPDATE = (
    F(LSR.key),
    F(LSR.status),
    F(LSR.summary),
    F(LSR.invited_to_meeting),
)

INCIDENT_AVAILABLE_FOR_UPDATE = (
    F(Incident.key),
    F(Incident.status),
    F(Incident.summary),
)


class CalendarMock:
    def get_layer_events(self, *args, **kwargs):
        return [
            {
                'name': '"Live Site Review"',
                'id': 1,
                'startTs': '2021-04-09T16:00:00',
                'endTs': '2021-04-09T17:00:00',
                'instanceStartTs': '2021-04-09T16:00:00'
            },
            {
                'name': 'randname',
                'id': 2,
                'startTs': '2021-04-06T10:00:00',
                'endTs': '2021-04-06T11:00:00',
                'instanceStartTs': '2021-04-06T16:00:00'
            },
        ]

    def get_holidays(self, start_date: str, end_date: str):
        return []

    def create_event(self, *args, **kwargs):
        return random.randint(2, 1000)

    def get_event(self, event_id: int):
        return {
            'description': 'test desc',
            'startTs': 0,
            'id': event_id,
        }

    def update_event(self, *args, **kwargs):
        pass


REDUCER.clients.calendar = CalendarMock()


class TestLSREvents(TestCase):

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        prepare_db()
        create_components(
            component_pb2.CreateComponentRequest(component=component_pb2.Component(slug=COMPONENT, abc_service_slug='test-lsr-importer-abc')),
        )
        with session_scope() as session:
            session.merge(
                to_model(lsr_pb2.LSR(
                    key=LSR_KEY,
                    status=STATUS,
                    summary=SUMMARY,
                    invited_to_meeting=INVITED,
                ), include=LSR_AVAILABLE_FOR_UPDATE))

    @classmethod
    def tearDownClass(cls) -> None:
        clear_db()

    def test_suggest_lsr(self):
        response = REDUCER.suggest_event_fields(lsr_pb2.SuggestFieldsRequest(
            ticket_key=LSR_KEY
        ), None)

        self.assertEqual(response.result.event_type, lsr_pb2.LSREventType.LSR_EVENT)
        self.assertTrue(len(response.result.participants) == 2)
        self.assertEqual(sorted(response.result.participants), sorted(INVITED))

    def test_suggest_incident(self):
        with session_scope() as session:
            model = session.merge(to_model(
                incident_pb2.Incident(
                    key=INCIDENT_KEY,
                    status=STATUS,
                    summary=SUMMARY,
                )
            ))
            response = REDUCER.suggest_event_fields(lsr_pb2.SuggestFieldsRequest(
                ticket_key=INCIDENT_KEY
            ), None)
            session.delete(model)

        self.assertFalse(bool(response.error), msg=f'unexpected field value: {response.error}')
        self.assertEqual(response.result.name, f'MINILSR {SUMMARY}')

    def test_suggest_errors(self):
        suggest = lambda t: REDUCER.suggest_event_fields(lsr_pb2.SuggestFieldsRequest(ticket_key=t), None)

        resp = suggest('QUEUE-1')
        self.assertEqual(resp.error, 'Unknown ticket queue: QUEUE. Expected one of: SPI, LSR')

        resp = suggest(INCIDENT_KEY)
        self.assertEqual(resp.error, f'Ticket {INCIDENT_KEY} not found or was not synced yet.')
