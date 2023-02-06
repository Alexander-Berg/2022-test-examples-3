import uuid

from search.martylib.core.date_utils import now
from search.martylib.db_utils import session_scope

from search.morty.proto.structures import common_pb2, executor_pb2, process_pb2
from search.morty.sqla.morty import model

from search.morty.src.executor.abstract import AbstractProcess, AbstractSubProcess
from search.morty.src.executor.tasks import EMPTY   # noqa
from search.morty.src.common.clients.nanny import MortyNannyClientMock

from search.morty.tests.utils.test_case import MortyTestCase


class TestAbstractProcess(MortyTestCase):
    def test_cancel(self):
        apr = AbstractProcess(
            process=process_pb2.Process(
                id=str(uuid.uuid4()),
                flags=common_pb2.ManipulationFlags(
                    cancelled=True,
                ),
                subprocesses=[
                    process_pb2.SubProcess(
                        id='1',
                        tasks=[
                            executor_pb2.ExecutionTask(
                                id=str(uuid.uuid4()),
                            )
                            for _ in range(5)
                        ]
                    ),
                    process_pb2.SubProcess(id='2'),
                ],
            ),
        )

        # no meta-recipe.
        # Should not send any request
        functions_calls_before = MortyNannyClientMock.functions_calls['set_taskgroup_status']
        apr.cancel()
        assert functions_calls_before == MortyNannyClientMock.functions_calls['set_taskgroup_status']

        # recipe exist. At least one request should be sent
        apr._proto.subprocesses[0].tasks[4].state.deploy_nanny_recipe.alemate_taskgroup = '1488'
        functions_calls_before = MortyNannyClientMock.functions_calls['set_taskgroup_status']
        apr.cancel()
        assert functions_calls_before < MortyNannyClientMock.functions_calls['set_taskgroup_status']

    def test_execute(self):
        with session_scope() as session:
            now_ = int(now().timestamp())
            apr = AbstractProcess(
                process=process_pb2.Process(
                    id=str(uuid.uuid4()),
                    subprocesses=[
                        process_pb2.SubProcess(id='1'),
                        process_pb2.SubProcess(id='2'),
                    ],
                ),
            )
            assert 0 == session.query(model.Notification).count()

            apr.execute([])
            assert apr._proto.state.execution_starts >= now_
            assert apr._proto.state.finished is False
            for sp in apr._subprocesses.values():
                assert sp._proto.state.execution_starts == 0

            assert 1 == session.query(model.Notification).count()

            apr.execute(['1'])
            assert apr._proto.state.execution_starts >= now_
            for sp in apr._subprocesses.values():
                if sp._proto.id == '1':
                    assert sp._proto.state.finished is True
                    assert sp._proto.state.execution_starts >= now_
                else:
                    assert sp._proto.state.execution_starts == 0

            apr.execute(['2'])
            assert apr._proto.state.finished is True
            for sp in apr._subprocesses.values():
                assert sp._proto.state.finished is True
                assert sp._proto.state.execution_starts >= now_

            apr = AbstractProcess(
                process=process_pb2.Process(
                    id='1',
                    flags=common_pb2.ManipulationFlags(cancelled=True),
                    subprocesses=[
                        process_pb2.SubProcess(id='1'),
                    ]
                )
            )
            apr.execute([])
            assert apr._proto.state.execution_starts >= now_
            assert apr._proto.state.finished is True


class TestAbstractSubProcess(MortyTestCase):
    def test_execute(self):
        now_ = int(now().timestamp())
        aspr = AbstractSubProcess(process_pb2.SubProcess())
        aspr.execute(executor_pb2.ExecutionTaskState())
        assert aspr._proto.state.finished is True
        assert aspr._proto.state.execution_starts >= now_

        aspr = AbstractSubProcess(
            process_pb2.SubProcess(
                id=str(uuid.uuid4()),
                tasks=[
                    executor_pb2.ExecutionTask(
                        type=executor_pb2.ExecutionTask.Type.EMPTY,
                    ),
                    executor_pb2.ExecutionTask(
                        type=executor_pb2.ExecutionTask.Type.EMPTY,
                    ),
                ]
            )
        )

        aspr.execute(executor_pb2.ExecutionTaskState())
        for task in aspr._tasks:
            assert task._proto.state.valid is True
            assert task._proto.state.done is False

        with session_scope() as session:
            assert 1 == session.query(model.Notification).count()
            n = session.query(model.Notification).first()
            assert n.message == 'empty_start'
            assert n.subprocess == aspr._proto.id

        aspr.execute(executor_pb2.ExecutionTaskState())
        for task in aspr._tasks:
            assert task._proto.state.valid is True
            assert task._proto.state.done is True

        with session_scope() as session:
            assert 2 == session.query(model.Notification).count()
            for n in session.query(model.Notification).all():
                assert n.message in ('empty_start', 'empty_end')
                assert n.subprocess == aspr._proto.id

        aspr = AbstractSubProcess(
            process_pb2.SubProcess(
                tasks=[
                    executor_pb2.ExecutionTask(
                        type=executor_pb2.ExecutionTask.Type.EMPTY,
                        params=executor_pb2.ExecutionTaskParams(
                        ),
                    ),
                ]
            )
        )
        aspr._tasks[0].NEED_VALIDATION = False
        aspr.execute(executor_pb2.ExecutionTaskState())
        assert aspr._tasks[0]._proto.state.done is True

        aspr = AbstractSubProcess(
            process_pb2.SubProcess()
        )
        aspr.cancel(executor_pb2.ExecutionTaskState())
        assert aspr._proto.state.finished is True
