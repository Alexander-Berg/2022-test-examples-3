from mail.devpack.lib.components.base import FakeRootComponent
from components.apq_tester import ApqTester


class ApqTesterService(FakeRootComponent):
    NAME = 'apq_teste'
    DEPS = [ApqTester]
