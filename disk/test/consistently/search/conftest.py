# -*- coding: utf-8 -*-
import pytest

from mpfs.common.errors import APIError
from mpfs.common.errors import SearchError


@pytest.hookimpl(tryfirst=True, hookwrapper=True)
def pytest_runtest_makereport(item, call):
    """Игнорирует нестабильные тесты.

    Игнорирует упавшие тесты, если запрос в Поиск Диска завершится с ошибкой.
    """
    from mpfs.engine.process import get_default_log
    log = get_default_log()
    outcome = yield
    report = outcome.get_result()

    if (report.failed and
        call.excinfo.type in (SearchError, APIError)):
        msg = "Test %s failed when DiskSearch is not stable. Test will be skipped." % item.nodeid
        log.error(msg)
        report.outcome = "skipped"
        report.longrepr = (item.location[0], item.location[1], msg)
