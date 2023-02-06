from search.martylib.core.date_utils import now
from search.martylib.db_utils import prepare_db, session_scope, to_model
from search.martylib.http.exceptions import NotFound
from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import curator_meeting_pb2
from search.mon.warden.proto.structures.component import common_pb2
from search.mon.warden.sqla.warden.model import Component, CuratorMeeting
from search.mon.warden.src.services.curator_meeting import CuratorMeetingApiService

CURATOR_MEETING_API = CuratorMeetingApiService()
MeetingStatus = curator_meeting_pb2.CuratorMeeting.MeetingStatus


class TestGetMeeting(TestCase):

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        prepare_db()
        with session_scope() as session:
            functionality_analysis1 = curator_meeting_pb2.FunctionalityAnalysis(
                id='testuuid-0123-4567-89ab-cdefghijklmn',
                comment='functionality comment 1',
            )
            functionality_analysis2 = curator_meeting_pb2.FunctionalityAnalysis(
                id='testuuid-0123-4567-8910-cdefghijklmn',
                comment='functionality comment 2',
            )
            func_analysis_group = curator_meeting_pb2.FunctionalityAnalysisGroup(
                name='top functionalities',
                functionality_list=curator_meeting_pb2.FunctionalityAnalysisList(items=[functionality_analysis1, functionality_analysis2]),
            )
            incident_review1 = curator_meeting_pb2.IncidentReview(key='SPI-01234', comment='spi comment 1', is_ok=True)
            incident_review2 = curator_meeting_pb2.IncidentReview(key='SPI-56789', comment='spi comment 2', is_ok=False)
            incident_review_group = curator_meeting_pb2.IncidentReviewGroup(
                name='top spis',
                incident_list=curator_meeting_pb2.IncidentReviewList(items=[incident_review1, incident_review2]),
            )
            meeting_performed_last = curator_meeting_pb2.CuratorMeeting(
                status=MeetingStatus[MeetingStatus.finished],
                meeting_time=int(now().timestamp() - 1000),
                action_items=['action items'],
                news='news',
                feedback='feedback',
                functionality_groups=curator_meeting_pb2.FunctionalityAnalysisGroupList(items=[func_analysis_group]),
                incident_groups=curator_meeting_pb2.IncidentReviewGroupList(items=[incident_review_group]),
            )
            meeting_performed_old = curator_meeting_pb2.CuratorMeeting(
                status=MeetingStatus[MeetingStatus.finished],
                meeting_time=int(now().timestamp() - 100000),
                action_items=['old action items'],
                news='old news',
                feedback='old feedback',
            )
            meeting_not_performed = curator_meeting_pb2.CuratorMeeting(
                status=MeetingStatus[MeetingStatus.planned],
                meeting_time=int(now().timestamp()) + 100000,
            )

            component = Component(name='test')
            session.add(component)
            component.meetings += list(map(to_model, [meeting_performed_old, meeting_performed_last, meeting_not_performed]))

    @classmethod
    def tearDownClass(cls):
        with session_scope() as session:
            session.query(CuratorMeeting).delete()
            session.query(Component).delete()

    def test_get_last_performed_meeting(self):
        request = curator_meeting_pb2.GetClosestMeetingRequest(
            component=common_pb2.ComponentFilter(
                component_name='test',
                parent_component_name='',
            ),
            was_performed=True,
        )
        response = CURATOR_MEETING_API.get_closest_meeting(request, context=None)
        self.assertEqual(response.news, 'news')
        self.assertEqual(response.functionality_groups.items[0].functionality_list.items[1].comment, 'functionality comment 2')
        self.assertEqual(response.incident_groups.items[0].incident_list.items[0].is_ok, True)

    def test_get_last_scheduled_meeting(self):
        request = curator_meeting_pb2.GetClosestMeetingRequest(
            component=common_pb2.ComponentFilter(
                component_name='test',
                parent_component_name='',
            ),
            was_performed=False,
        )
        response = CURATOR_MEETING_API.get_closest_meeting(request, context=None)
        self.assertEqual(response.news, '')

    def test_get_wrong_component_meeting(self):
        request = curator_meeting_pb2.GetClosestMeetingRequest(
            component=common_pb2.ComponentFilter(
                component_name='wrong_test',
                parent_component_name='wrong_parent',
            ),
            was_performed=True,
        )
        try:
            CURATOR_MEETING_API.get_closest_meeting(request, context=None)
        except NotFound as e:
            self.assertEqual(str(e), 'HTTP 404: <url not available>: CuratorMeeting for component wrong_parent/wrong_test not found: -')
