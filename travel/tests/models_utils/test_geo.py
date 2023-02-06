# -*- coding: utf-8 -*-
import pytest
from freezegun import freeze_time

from travel.avia.ticket_daemon_api.jsonrpc.models_utils.geo import PointInterface


class FakePoint(PointInterface):
    def __init__(self, time_zone):
        self.time_zone = time_zone


@freeze_time("2012-01-14")
def test__local_time__raise_error():
    point = FakePoint('US/Pacific-New')
    with pytest.raises(ValueError):
        _ = point.local_time
