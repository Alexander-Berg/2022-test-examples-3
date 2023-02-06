from typing import Type

import pytest
from pytest_mock import MockFixture
from solomon import BasePushApiReporter
from requests import HTTPError

from travel.avia.ad_feed.ad_feed.metrics import record_counter


@pytest.fixture()
def reporter(mocker: MockFixture):
    return mocker.create_autospec(BasePushApiReporter)


def test_record_counter(reporter):
    for _ in record_counter([1, 2, 3], sensor='some_name', labels={'a': 'b'}, reporter=reporter):
        pass
    reporter.set_value.assert_called_once_with(sensor='some_name', labels={'a': 'b'}, value=3)


@pytest.mark.parametrize(('exc_type', 'ignore'), [(HTTPError, True), (ValueError, False)])
def test_record_counter_suppress_errors(reporter, exc_type: Type[Exception], ignore: bool):
    reporter.set_value.side_effect = exc_type
    stream = record_counter([1, 2, 3], sensor='some_name', labels={'a': 'b'}, reporter=reporter)
    if ignore:
        list(stream)
    else:
        with pytest.raises(exc_type):
            list(stream)
    reporter.set_value.assert_called_once_with(sensor='some_name', labels={'a': 'b'}, value=3)
