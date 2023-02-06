# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import marshmallow
import pytz
from marshmallow import Schema

from common.utils.date import MSK_TZ
from travel.rasp.morda_backend.morda_backend.serialization.fields import AwareDateTime, FlagField


def test_flag_field():
    class TestSchema(marshmallow.Schema):
        flag = FlagField()

    result = TestSchema().dump({})
    assert not result.errors
    assert 'flag' not in result.data

    result = TestSchema().dump({'flag': True})
    assert not result.errors
    assert result.data['flag'] is True

    result = TestSchema().dump({'flag': False})
    assert not result.errors
    assert 'flag' not in result.data


def test_aware_datetime():
    """
    Тестируем перевод времени в UTC.
    Если время = None (например, для интервальных рейсов) - возвращаем None.
    """
    class AwareDateTimeSchema(Schema):
        dt = AwareDateTime()
        none = AwareDateTime()

    schema = AwareDateTimeSchema()
    local_dt = MSK_TZ.localize(datetime(2016, 1, 1, 10, 0))

    dts = schema.dump({'dt': local_dt, 'none': None}).data
    assert dts['dt'] == datetime(2016, 1, 1, 7, 0, tzinfo=pytz.UTC).isoformat()
    assert dts['none'] is None
