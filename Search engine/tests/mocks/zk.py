# coding: utf-8

from components_app.component.zk import Zk
import zake.fake_client


class ZkMock(Zk):
    def __init__(self):
        super(Zk, self).__init__()
        self.client = None

    def _stop(self):
        self.client.stop()

    def _start(self):
        self.client.start()

    def _load_config(self, hosts):
        self.client = zake.fake_client.FakeClient()

