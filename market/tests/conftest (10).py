import os  # noqa
import json  # noqa
import pytest  # noqa
import yatest  # noqa
from _pytest.monkeypatch import MonkeyPatch  # noqa


@pytest.fixture(autouse=True)
def fixtures_dir(monkeypatch):  # type: (MonkeyPatch) -> unicode
    return ''
