# -*- coding: utf-8 -*-
from collections import defaultdict

from test.helpers.stubs.services import (
    ClckStub,
    SearchIndexerStub,
    KladunStub,
    PassportStub,
    PushServicesStub,
    DirectoryServiceStub,
    AbookStub,
    HbfServiceStub,
    DiskSearchStub,
    UAASStub,
    NewUAASStub,
    SpamCheckerStub,
    SendEmailStub,
)


class StubScope:
    CLASS = "class"
    FUNCTION = "function"


class StubsManager(object):
    """Управляет заглушками."""
    DEFAULT_CLASS_STUBS = (ClckStub, SearchIndexerStub, DiskSearchStub, PassportStub, DirectoryServiceStub, AbookStub,
                           HbfServiceStub, UAASStub, NewUAASStub, SpamCheckerStub, SendEmailStub)
    DEFAULT_METHOD_STUBS = (KladunStub, PushServicesStub)

    def __init__(self, class_stubs=DEFAULT_CLASS_STUBS, method_stubs=DEFAULT_METHOD_STUBS):
        self._stubs = {StubScope.CLASS: class_stubs,
                       StubScope.FUNCTION: method_stubs}
        self._enabled_stubs = defaultdict(list)

    def enable_stubs(self, scope=StubScope.FUNCTION):
        """Включает заглушки на указанной области действия.

        :param scope: область действия заглушек
        """
        for stub_class in self._stubs[scope]:
            stub = stub_class()
            stub.start()
            self._enabled_stubs[scope].append(stub)

    def disable_stubs(self, scope=StubScope.FUNCTION):
        """Выключает все заглушки из указанной области действия.

        :param scope: область действия заглушек
        """
        while self._enabled_stubs[scope]:
            stub = self._enabled_stubs[scope].pop()
            stub.stop()
