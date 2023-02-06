from kazoo.client import KazooClient
from kazoo.exceptions import ZookeeperError
import os
import pytest


ZK_INSTANCE = 0


@pytest.fixture
def zk():
    zk = KazooClient('localhost:{}'.format(os.getenv('RECIPE_ZOOKEEPER_PORT')))

    global ZK_INSTANCE
    zk_prefix = '/{}'.format(ZK_INSTANCE)
    ZK_INSTANCE += 1

    try:
        zk.start()
        zk.ensure_path(zk_prefix)
        zk.chroot = zk_prefix
        yield zk
    finally:
        try:
            zk.chroot = '/'
            zk.delete(zk_prefix, recursive=True)
            zk.stop()
        except ZookeeperError:
            pass
