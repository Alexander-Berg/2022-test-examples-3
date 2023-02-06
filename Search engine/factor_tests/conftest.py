import os

import pytest
from startrek_client import Startrek
from jinja2 import Environment, FileSystemLoader

from runtime.simcity import STARTREK_OAUTH


def pytest_configure(config):
    stserver = config.option.startrek_server
    stticket = config.option.startrek_ticket
    # prevent opening xmllog on slave nodes (xdist)
    if stserver and stticket and not hasattr(config, 'slaveinput'):
        config._startreck = StartreckReportPlugin(stserver, stticket)
        config.pluginmanager.register(config._startreck)


def pytest_internalerror(excrepr, excinfo):
    """ called for internal errors. """
    pass


def pytest_namespace():
    return {"metainfo": MetainfoNamespace()}


@pytest.mark.tryfirst
def pytest_runtest_makereport(item, call, __multicall__):
    rep = __multicall__.execute()
    if hasattr(item.function, "metainfo"):
        rep.metainfo = item.function.metainfo.copy()
        rep.params = {}
    if hasattr(item, "metainfo"):
        rep.metainfo.update(item.metainfo)
    if hasattr(item, "callspec"):
        rep.params = item.callspec.params
    return rep


def pytest_runtest_call(item):
    pytest.metainfo.current_item = item


Metainfo = dict


class MetainfoNamespace(object):
    @property
    def current_item(self):
        return self._item

    @current_item.setter
    def current_item(self, value):
        self._item = value
        pass

    def set(self, **kwargs):
        def __set(f):
            f.metainfo = Metainfo(**kwargs)
            return f

        return __set

    def update(self, *args, **kwargs):
        if not hasattr(self.current_item, "metainfo"):
            self.current_item.metainfo = Metainfo(**kwargs)
        self.current_item.metainfo.update(**kwargs)


def pytest_unconfigure(config):
    startreck = getattr(config, '_startreck', None)
    if startreck:
        del config._startreck
        config.pluginmanager.unregister(startreck)


class TestReport(object):
    def __init__(self, id):
        self.__id = id
        self.__result = None
        self.__status = None
        self.__metainfo = None
        self.__params = None

    @property
    def id(self):
        return self.__id

    @property
    def metainfo(self):
        return self.__metainfo

    @metainfo.setter
    def metainfo(self, value):
        self.__metainfo = value

    @property
    def params(self):
        return self.__params

    @params.setter
    def params(self, value):
        self.__params = value

    @property
    def name(self):
        try:
            return self.metainfo["name"].format(**self.params)
        except:
            return "unknown"

    @property
    def description(self):
        try:
            return self.metainfo["description"].format(**self.params)
        except:
            return "unknown"

    @property
    def result(self):
        return self.__result

    @result.setter
    def result(self, value):
        self.__result = value

    @property
    def status(self):
        return self.__status

    @status.setter
    def status(self, value):
        self.__status = value

    @property
    def message(self):
        return ""

    @property
    def stdout(self):
        return ""

    @property
    def stderr(self):
        return ""


class SessionReport(object):
    def __init__(self):
        self.tests = []
        self._title = ""

    @property
    def title(self):
        return self._title

    @title.setter
    def title(self, value):
        self._title = value

    def add_test_report(self, id, status='ready', result='in progress'):
        test_report = TestReport(id)
        test_report.status = status
        test_report.result = result
        self.tests += [test_report]
        return test_report

    def has_test_report(self, id):
        try:
            self.get_test_report(id)
        except IndexError:
            return False
        return True

    def get_test_report(self, id):
        return filter(lambda report: report.id == id, self.tests)[0]


class StartreckReportPlugin(object):
    def __init__(self, server, ticket):
        self.client = Startrek('startrek_client', token=STARTREK_OAUTH, base_url=server, timeout=5.0)
        self.ticket = ticket
        self._startreck_comment = None
        self.sessionreport = SessionReport()

    @property
    def comment(self):
        if self._startreck_comment:
            return self._startreck_comment.text

    @comment.setter
    def comment(self, value):
        if self._startreck_comment is None:
            self._startreck_comment = self.client.issues[self.ticket].comments.create(text=value)
        else:
            self._startreck_comment.update(text=value)

    def _resolve_funcards(self, item):
        return {}

    def pytest_runtest_logreport(self, report):
        if not self.sessionreport.has_test_report(report.nodeid):
            test_report = self.sessionreport.add_test_report(report.nodeid)
        else:
            test_report = self.sessionreport.get_test_report(report.nodeid)
        if report.when == 'call':
            test_report.result = report.outcome
            test_report.status = "done"
            if hasattr(report, "metainfo"):
                test_report.metainfo = report.metainfo
                test_report.params = report.params
            self.comment = self.generate_comment()

    def pytest_sessionstart(self):
        self.sessionreport.title = "Session in progress..."
        self.comment = self.generate_comment()

    def pytest_sessionfinish(self):
        self.sessionreport.title = "Session done"
        self.comment = self.generate_comment()

    def generate_comment(self):
        env = Environment(loader=FileSystemLoader(os.path.dirname(__file__)))
        template = env.get_template("comment.html")
        return template.render(report=self.sessionreport)
