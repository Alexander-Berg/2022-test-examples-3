import multiprocessing
import os
import os.path
import signal

from edera.daemon import Daemon
from edera.daemon import DaemonWorkflowDescriptor
from edera.daemon import RunDaemonWorkflow
from edera.monitoring import MonitoringSnapshot
from edera.monitoring.tasks import WatchMonitor
from edera.storages import SQLiteStorage


def test_watcher_produces_correct_snapshot(correct_fly_task, correct_prepare_task, tmpdir):

    class RunWorkflow(RunDaemonWorkflow):

        @property
        def _monitor(self):
            return monitor

    class Watch(WatchMonitor):

        @property
        def _monitor(self):
            return monitor

    def daemonize():
        prime = DaemonWorkflowDescriptor(runner=RunWorkflow(task=correct_fly_task))
        adhoc = DaemonWorkflowDescriptor(runner=RunWorkflow(task=correct_prepare_task))
        offline = RunWorkflow(task=Watch(delay="PT1S"))
        Daemon([prime], adhoc=adhoc, offline=offline).run()

    monitor = SQLiteStorage(os.path.join(str(tmpdir), "monitor.db"))
    process = multiprocessing.Process(target=daemonize)
    process.start()
    process.join(8.0)
    os.kill(process.pid, signal.SIGTERM)
    process.join(1.0)
    assert not process.is_alive()
    snapshot = MonitoringSnapshot.deserialize(monitor.get("snapshot", limit=1)[0][1])
    assert len(snapshot.tasks) == 5
