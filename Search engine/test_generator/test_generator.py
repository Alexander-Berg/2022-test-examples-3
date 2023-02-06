import uuid

import yatest.common

from search.martylib.db_utils import session_scope, to_model

from search.morty.proto.structures import event_pb2, rule_pb2
from search.morty.sqla.morty import model

from search.morty.src.scheduler.generator import ProcessGenerator

from search.morty.tests.utils.test_case import MortyTestCase


data_path = yatest.common.source_path('search/morty/tests/test_data/test_nanny/recipes')


class TestGenerator(MortyTestCase):
    def test_link_one(self):
        rules = [
            rule_pb2.Rule(
                id=str(uuid.uuid4()),
                predicates=rule_pb2.PredicateList(
                    objects=[
                        rule_pb2.Predicate(
                            alias='1',
                            component=rule_pb2.ComponentPredicate(
                                parent_component_name='parent',
                                component_name='component',
                            ),
                        ),
                        rule_pb2.Predicate(
                            alias='2',
                            tag='tag',
                        ),
                    ],
                ),
            ),
            rule_pb2.Rule(
                id=str(uuid.uuid4()),
                predicates=rule_pb2.PredicateList(
                    objects=[
                        rule_pb2.Predicate(
                            alias='1',
                            component=rule_pb2.ComponentPredicate(
                                parent_component_name='parent-2',
                                component_name='component-2',
                            ),
                        ),
                        rule_pb2.Predicate(
                            alias='2',
                            tag='tag-2',
                        ),
                    ],
                ),
            ),
        ]
        with session_scope() as session:
            session.merge(to_model(rules[0]))
            session.merge(to_model(rules[1]))

            events = [
                event_pb2.Event(id=str(uuid.uuid4()), config=event_pb2.EventConfig(parent_component_name='parent', component_name='component')),
                event_pb2.Event(id=str(uuid.uuid4()), config=event_pb2.EventConfig(parent_component_name='parent-3', component_name='component-3')),
            ]
            session.merge(to_model(events[0]))
            session.merge(to_model(events[1]))

        generator = ProcessGenerator()
        with session_scope() as session:
            assert session.query(model.morty__Rule__events).count() == 0
            assert session.query(model.AppliedRule).count() == 0

        # test link exists
        generator.link_one(events[0], rules)
        with session_scope() as session:
            assert session.query(model.morty__Rule__events).count() == 1
            assert session.query(model.AppliedRule).count() == 1

        # test link not exists
        generator.link_one(events[1], rules)
        with session_scope() as session:
            assert session.query(model.morty__Rule__events).count() == 1
            assert session.query(model.AppliedRule).count() == 1
