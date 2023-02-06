from search.martylib.db_utils import session_scope

from search.morty.proto.structures import revision_pb2, event_pb2
from search.morty.sqla.morty import model

from search.morty.src.services.tools.revision import RevisionManager
from search.morty.tests.utils.test_case import MortyTestCase


class TestRevision(MortyTestCase):
    pass
    # def test_create_revision(self):
    #     RevisionManager.create_revision(
    #         rtype=revision_pb2.Revision.Type.EVENT,
    #         related_id='test',
    #         action='test',
    #         state=revision_pb2.State(
    #             event=event_pb2.EventRequest(
    #                 id='test',
    #             )
    #         ),
    #         request=revision_pb2.Request(
    #             event=event_pb2.EventRequest(
    #                 id='test',
    #             )
    #         ),
    #     )
    #
    #     with session_scope() as session:
    #         revision: revision_pb2.Revision = session.query(model.Revision).first().to_protobuf()
    #
    #         assert revision.type == revision_pb2.Revision.Type.EVENT
    #         assert revision.related_id == 'test'
    #         assert revision.action == 'test'
    #         assert revision.state.event.id == 'test'
    #         assert revision.request.event.id == 'test'
