import uuid

import pytest

from yamarec1.beans import settings
from yamarec1.statbox import StatboxClient
from yamarec1.statbox.exceptions import StatboxError


@pytest.fixture
def report_settings():
    return """
---
dimensions:
- fielddate: date
- one: string
- two: number
measures:
- three: string
"""


@pytest.fixture
def statbox_client():
    return StatboxClient(config=settings.statbox)


@pytest.yield_fixture
def report(statbox_client):
    report_name = "report_" + uuid.uuid4().hex
    try:
        yield report_name
    finally:
        try:
            statbox_client.delete(report_name)
        except StatboxError:
            pass


@pytest.fixture
def report_data():
    return [
        {
            "fielddate": "2018-01-01",
            "scale": "d",
            "one": "Privet",
            "two": 22,
            "three": "Hola",
        },
        {
            "fielddate": "2018-01-02",
            "scale": "d",
            "one": "Privet",
            "two": 23,
            "three": "Hola2",
        }
    ]
