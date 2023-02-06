from sqlalchemy.orm import Session

from search.martylib.core.date_utils import now
from search.martylib.db_utils import prepare_db, session_scope, to_model, clear_db
from search.martylib.test_utils import TestCase

from search.sawmill.proto import trace_pb2
from search.sawmill.sqla.sawmill import model
from search.sawmill.src.workers.gc import GarbageCollector


class TestGarbageCollector(TestCase):
    @classmethod
    def setUpClass(cls):
        prepare_db()
        cls.gc = GarbageCollector()

    @classmethod
    def tearDownClass(cls):
        clear_db()

    def test_garbage_collector(self):
        with session_scope() as session:
            session: Session

            added = now().timestamp()

            st1 = session.merge(to_model(trace_pb2.StoredTrace(ttl=0, enable_stream=True, added=added))).id  # should be deleted
            st2 = session.merge(to_model(trace_pb2.StoredTrace(ttl=1000, enable_stream=True, added=added))).id  # should not be deleted
            st3 = session.merge(to_model(trace_pb2.StoredTrace(ttl=1000, added=added))).id  # should not be deleted
            st4 = session.merge(to_model(trace_pb2.StoredTrace(ttl=0, added=added))).id  # should be deleted

            session.merge(to_model(trace_pb2.Frame(trace_id=st1)))
            session.merge(to_model(trace_pb2.Frame(trace_id=st1)))

            session.merge(to_model(trace_pb2.Frame(trace_id=st2)))
            session.merge(to_model(trace_pb2.Frame(trace_id=st2)))

            self.gc.clean()

            # check traces
            assert not session.query(model.StoredTrace).filter(model.StoredTrace.id == st1).count()
            assert session.query(model.StoredTrace).filter(model.StoredTrace.id == st2).count()
            assert session.query(model.StoredTrace).filter(model.StoredTrace.id == st3).count()
            assert not session.query(model.StoredTrace).filter(model.StoredTrace.id == st4).count()

            # check frames
            assert not session.query(model.Frame).filter(model.Frame.trace_id == st1).count()
            assert session.query(model.Frame).filter(model.Frame.trace_id == st2).count() == 2
