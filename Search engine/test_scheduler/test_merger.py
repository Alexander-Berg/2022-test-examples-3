import uuid

from search.martylib.db_utils import session_scope, to_model, generate_field_name as F

from search.morty.proto.structures import rule_pb2, process_pb2, resource_pb2, event_pb2, executor_pb2
from search.morty.sqla.morty import model

from search.morty.src.scheduler.merger import ProcessMerger

from search.morty.tests.utils.test_case import MortyTestCase


class TestProcessMerger(MortyTestCase):
    def test_select(self):
        # test count <= 1
        rules = [
            rule_pb2.AppliedRule(
                id='1',
                rule='rule-1',
                type=rule_pb2.Rule.Type.UNION,
            ),
            rule_pb2.AppliedRule(
                id='2',
                rule='rule-2',
                type=rule_pb2.Rule.Type.UNION,
            ),
        ]
        with session_scope() as session:
            session.merge(to_model(rules[0], exclude=(F(model.AppliedRule.processes), )))
            session.merge(to_model(rules[1], exclude=(F(model.AppliedRule.processes), )))

        assert len(ProcessMerger.select()) == 0

        # test count > 1
        rules.extend((
            rule_pb2.AppliedRule(id='3', rule='rule-2', type=rule_pb2.Rule.Type.UNION),
        ))
        with session_scope() as session:
            session.merge(to_model(rules[2], exclude=(F(model.AppliedRule.processes), )))

        res = ProcessMerger.select()
        assert len(res) == 2
        assert {r.rule for r in res} == {'rule-2'}

        # test unusable
        rules[2].unusable = True
        with session_scope() as session:
            session.merge(to_model(rules[2], exclude=(F(model.AppliedRule.processes),)))

        assert len(ProcessMerger.select()) == 0

    def test_merge_one(self):
        # test empty rules -> no errors
        merger = ProcessMerger()
        merger.merge_one([])

        # test not valid processes -> unusable, no merge
        rules = [
            rule_pb2.AppliedRule(
                id='1',
                rule='rule',
                type=rule_pb2.Rule.Type.UNION,
            ),
            rule_pb2.AppliedRule(
                id='2',
                rule='rule',
                type=rule_pb2.Rule.Type.UNION,
                processes=[
                    process_pb2.Process(),
                    process_pb2.Process(),
                ]
            ),
        ]
        with session_scope() as session:
            session.merge(to_model(rules[0]))
            session.merge(to_model(rules[1]))

            ProcessMerger().merge()
            res = session.query(model.AppliedRule).all()
            assert all(r.unusable for r in res)
            session.query(model.AppliedRule).delete()

        # test acquired process -> unusable, no merge
        rules = [
            rule_pb2.AppliedRule(
                id='1',
                rule='rule',
                type=rule_pb2.Rule.Type.UNION,
                processes=[
                    process_pb2.Process()
                ]
            ),
            rule_pb2.AppliedRule(
                id='2',
                rule='rule',
                type=rule_pb2.Rule.Type.UNION,
                processes=[
                    process_pb2.Process(
                        lock=resource_pb2.ResourceLock(
                            acquired=True,
                        ),
                    ),
                ]
            ),
        ]

        with session_scope() as session:
            session.merge(to_model(rules[0]))
            session.merge(to_model(rules[1]))

            ProcessMerger().merge()
            res = session.query(model.AppliedRule).all()
            assert {(r.id, r.unusable) for r in res} == {('1', False), ('2', True)}
            session.query(model.AppliedRule).delete()

        # test merge
        with session_scope() as session:
            events = []
            for i in range(2):
                event_id = str(uuid.uuid4())
                events.append(
                    event_pb2.Event(
                        id=event_id,
                        process=process_pb2.Process(
                            id=str(uuid.uuid4()),
                            subprocesses=[
                                process_pb2.SubProcess(
                                    tasks=[
                                        executor_pb2.ExecutionTask(
                                            event_id=event_id,
                                            merge_id='1',
                                        )
                                    ]
                                ),
                                process_pb2.SubProcess(
                                    tasks=[
                                        executor_pb2.ExecutionTask(
                                            event_id=event_id,
                                            merge_id='2',
                                        )
                                    ]
                                )
                            ]
                        )
                    )
                )
                session.merge(to_model(events[-1]))

            rules = []
            for i in range(2):
                rules.append(
                    rule_pb2.AppliedRule(
                        id=str(i + 1),
                        type=rule_pb2.Rule.Type.UNION,
                        rule='rule',
                        created_at=i,
                        event_id=events[i].id,
                    ),
                )
                session.merge(to_model(rules[-1], exclude=(F(model.AppliedRule.processes), )))

            session.flush()
            for i in range(2):
                session.execute(
                    model.morty__AppliedRule__processes.insert(),
                    {
                        'morty__Process_id': events[i].process.id,
                        'morty__AppliedRule_id': rules[i].id,
                    }
                )
            session.commit()

            ProcessMerger().merge()
            res = session.query(model.AppliedRule).all()
            assert {(r.id, r.unusable) for r in res} == {('1', False), ('2', True)}

            tasks = session.query(model.ExecutionTask).all()
            assert len({str(t.morty__SubProcess_id) for t in tasks}) == 2

            events = session.query(model.Event).all()
            assert len({str(e.morty__Process_id) for e in events}) == 1

            processes = session.query(model.Process).all()
            assert {p.unusable for p in processes} == {True, False}
