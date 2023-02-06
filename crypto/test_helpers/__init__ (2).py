from crypta.lib.python.bt import workflow
from crypta.lib.python import zk


def execute(task):
    with zk.fake_zk_client() as fake_zk:
        workflow.execute_sync(task, fake_zk, do_fork=False)
