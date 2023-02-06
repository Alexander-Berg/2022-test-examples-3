import multiprocessing
import os
import signal

from edera.daemon import Daemon
from edera.daemon import DaemonWorkflowDescriptor
from edera.daemon import RunDaemonWorkflow


def test_daemon_works_as_expected(correct_fly_task, correct_prepare_task):

    def daemonize():
        prime = DaemonWorkflowDescriptor(runner=RunDaemonWorkflow(task=correct_fly_task))
        adhoc = DaemonWorkflowDescriptor(runner=RunDaemonWorkflow(task=correct_prepare_task))
        Daemon([prime], adhoc=adhoc).run()

    process = multiprocessing.Process(target=daemonize)
    process.start()
    process.join(1.0)
    assert process.is_alive()
    os.kill(process.pid, signal.SIGTERM)
    process.join(1.0)
    assert not process.is_alive()
    with open(os.path.join(correct_prepare_task.root, "preparation.log")) as stream:
        assert stream.read() == "QU"
    with open(os.path.join(correct_fly_task.root, "flight.log")) as stream:
        assert stream.read() == "KU"


def test_daemon_ensures_adhoc_completed(correct_fly_task, incorrect_prepare_task):

    def daemonize():
        prime = DaemonWorkflowDescriptor(runner=RunDaemonWorkflow(task=correct_fly_task))
        adhoc = DaemonWorkflowDescriptor(runner=RunDaemonWorkflow(task=incorrect_prepare_task))
        Daemon([prime], adhoc=adhoc).run()

    process = multiprocessing.Process(target=daemonize)
    process.start()
    process.join(1.0)
    assert process.is_alive()
    os.kill(process.pid, signal.SIGTERM)
    process.join(1.0)
    assert not process.is_alive()
    assert not os.path.exists(os.path.join(incorrect_prepare_task.root, "preparation.log"))
    assert not os.path.exists(os.path.join(correct_fly_task.root, "flight.log"))
