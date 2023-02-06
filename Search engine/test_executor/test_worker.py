from search.martylib.core.date_utils import now, mock_now
from search.martylib.db_utils import session_scope, to_model

from search.morty.src.executor.worker import ProcessExecutorWorker
from search.morty.src.executor.master import ProcessExecutorMaster
from search.morty.proto.structures import process_pb2, resource_pb2, common_pb2
from search.morty.sqla.morty import model

from search.morty.tests.utils.test_case import MortyTestCase

ONE_DAY = 24 * 60 * 60


class TestProcessExecutorWorker(MortyTestCase):
    def get_master(self) -> ProcessExecutorMaster:
        master = ProcessExecutorMaster()
        master.worker_processes.clear()
        master.worker_reports.clear()
        return master

    def test_run_once(self):
        master = self.get_master()
        worker = ProcessExecutorWorker()
        now_ = int(now().timestamp())
        with session_scope() as session:
            for _ in range(2):
                session.merge(to_model(process_pb2.Process(
                    lock=resource_pb2.ResourceLock(acquired=True),
                    subprocesses=[
                        process_pb2.SubProcess(
                            lock=resource_pb2.ResourceLock(acquired=True),
                        ),
                        process_pb2.SubProcess(),
                    ]
                )))

            session.commit()
            worker.run_once()
            processes = session.query(model.Process).all()
            for pr in processes:
                assert pr.state.finished is False
                assert pr.state.execution_starts == 0
                assert list(sp.state.finished for sp in pr.subprocesses) == [False, False]
                assert list(sp.state.execution_starts for sp in pr.subprocesses) == [0, 0]

            master.run_once()
            worker.run_once()
            processes = session.query(model.Process).all()
            for pr in processes:
                assert pr.state.finished is False
                assert pr.state.execution_starts >= now_
                assert sorted(sp.state.finished for sp in pr.subprocesses) == [False, True]
                assert max(sp.state.execution_starts for sp in pr.subprocesses) >= now_
                assert min(sp.state.execution_starts for sp in pr.subprocesses) == 0

    def test_process_pause(self):
        master = self.get_master()
        worker = ProcessExecutorWorker()
        now_ = int(now().timestamp())

        with session_scope() as session:
            session.merge(to_model(process_pb2.Process(
                lock=resource_pb2.ResourceLock(acquired=True),
                subprocesses=[
                    process_pb2.SubProcess(
                        lock=resource_pb2.ResourceLock(acquired=True),
                    )
                ],
                flags=common_pb2.ManipulationFlags(
                    paused_until=now_ + ONE_DAY
                )
            )))

        # let master know that the process is alive
        worker.report_state()

        master.run_once()
        worker.run_once()

        with session_scope() as session:
            process = session.query(model.Process).first()
            assert process.state.finished is False
            assert process.state.execution_starts == 0
            assert process.subprocesses[0].state.finished is False
            assert process.subprocesses[0].state.execution_starts == 0

        mocked_time = int(now().timestamp()) + ONE_DAY
        with mock_now(mocked_time):
            worker.report_state()
            master.run_once()
            worker.run_once()

        with session_scope() as session:
            process = session.query(model.Process).first()
            assert process.state.execution_starts >= mocked_time
            assert process.subprocesses[0].state.finished is True
            assert process.subprocesses[0].state.execution_starts >= mocked_time
