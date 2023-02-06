from search.martylib.db_utils import session_scope, to_model

from search.morty.proto.structures import process_pb2, resource_pb2, executor_pb2
from search.morty.sqla.morty import model

from search.morty.src.executor.master import ProcessExecutorMaster

from search.morty.tests.utils.test_case import MortyTestCase


class TestProcessExecutorMaster(MortyTestCase):
    def get_master(self):
        master = ProcessExecutorMaster()
        master.worker_reports.clear()   # singleton
        master.worker_processes.clear()
        return master

    def test_select(self):
        master = self.get_master()

        with session_scope() as session:
            assert len(master.select_processes(session)) == 0

            session.merge(to_model(
                process_pb2.Process(
                    subprocesses=[
                        process_pb2.SubProcess()
                    ],
                ),
            ))
            session.commit()
            assert len(master.select_processes(session)) == 0

            session.merge(to_model(
                process_pb2.Process(
                    lock=resource_pb2.ResourceLock(acquired=True),
                    subprocesses=[
                        process_pb2.SubProcess()
                    ],
                ),
            ))
            session.commit()
            assert len(master.select_processes(session)) == 1

            session.merge(to_model(
                process_pb2.Process(
                    lock=resource_pb2.ResourceLock(acquired=True),
                    subprocesses=[
                        process_pb2.SubProcess(
                            lock=resource_pb2.ResourceLock(acquired=True),
                        )
                    ],
                ),
            ))
            session.commit()
            assert len(master.select_processes(session)) == 2

            session.query(model.ProcessState).update({'finished': True})
            session.commit()
            assert len(master.select_processes(session)) == 0

    def test_report(self):
        master = self.get_master()
        master.report_worker(executor_pb2.ExecutorWorkerReport(fqdn='fqdn'))
        assert len(master.worker_reports.values()) == 1
        assert list(master.worker_reports.keys())[0] == 'fqdn'

        master.worker_processes.replace({'fqdn': {'process'}})
        res = master.report_worker(executor_pb2.ExecutorWorkerReport(fqdn='fqdn'))
        assert len(res.processes) == 1
        assert res.processes[0] == 'process'

    def test_run_once(self):
        master = self.get_master()
        assert list(master.alive_workers()) == []

        with session_scope() as session:
            session.merge(to_model(
                process_pb2.Process(
                    lock=resource_pb2.ResourceLock(acquired=True),
                    subprocesses=[
                        process_pb2.SubProcess(
                            lock=resource_pb2.ResourceLock(acquired=True),
                        ),
                        process_pb2.SubProcess(
                        )
                    ],
                ),
            ))

        master.run_once()
        res = master.report_worker(executor_pb2.ExecutorWorkerReport(fqdn='fqdn'))
        assert len(res.processes) == 0

        master.run_once()
        res = master.report_worker(executor_pb2.ExecutorWorkerReport(fqdn='fqdn'))
        assert len(res.processes) == 1

        with session_scope() as session:
            session.query(model.ProcessState).update({'finished': True})

        master.run_once()
        res = master.report_worker(executor_pb2.ExecutorWorkerReport(fqdn='fqdn'))
        assert len(res.processes) == 0
