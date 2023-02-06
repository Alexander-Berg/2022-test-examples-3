from search.martylib.db_utils import session_scope, to_model

from search.morty.tests.utils.test_case import MortyTestCase

from search.morty.proto.structures import rule_pb2, process_pb2

from search.morty.src.scheduler.cache import SchedulerCache


class TestSchedulerCache(MortyTestCase):
    def test_fetch(self):
        with session_scope() as session:
            session.merge(to_model(
                rule_pb2.AppliedRule(
                    id='test',
                    processes=[
                        process_pb2.Process(),
                        process_pb2.Process(),
                    ]
                )
            ))
            session.merge(to_model(process_pb2.Process()))

        cache = SchedulerCache()
        assert len(cache.processes) == 3
        assert len(cache.rules) == 1

        assert len(cache.rules[0].processes) == 2
        assert not {pr.id for pr in cache.rules[0].processes}.difference({pr.id for pr in cache.processes})
