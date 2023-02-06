# -*- coding: utf-8 -*-
from rtcc.core.dataobjects.testinfo import ERROR
from rtcc.core.dataobjects.testinfo import FAILED
from rtcc.core.dataobjects.testinfo import SUCCESS
from rtcc.core.dataobjects.testinfo import TestInfo
from rtcc.core.dataobjects.testinfo import WARNING


class TestInfoList(object):
    def __init__(self, list_=None):
        self._list = list_ or []

    @property
    def list(self):
        return self._list

    @property
    def success(self):
        return filter(lambda x: x.status == SUCCESS, self._list)

    @property
    def failed(self):
        return filter(lambda x: x.status == FAILED, self._list)

    @property
    def errors(self):
        return filter(lambda x: x.status == ERROR, self._list)

    @property
    def warnings(self):
        return filter(lambda x: x.status == WARNING, self._list)

    def add_test_result(self, name, result=SUCCESS, message="", output=""):
        self._list.append(TestInfo(name, message, output, result))

    def add_test_success(self, name):
        self._list.append(TestInfo(name, "", "", SUCCESS))

    def add_test_error(self, name, message, output=""):
        self._list.append(TestInfo(name, message, output, ERROR))

    def add_test_fail(self, name, message, output=""):
        self._list.append(TestInfo(name, message, output, FAILED))

    def add_test_warning(self, name, message, output=""):
        self._list.append(TestInfo(name, message, output, WARNING))

    def to_json(self):
        return [item.to_json() for item in self._list]

    @staticmethod
    def from_json(json_):
        return TestInfoList(
            [TestInfo(result["name"], result["message"], result["output"], result["status"]) for result in json_])
