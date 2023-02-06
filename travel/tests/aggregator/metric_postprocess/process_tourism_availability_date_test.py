import datetime
from freezegun import freeze_time

from travel.avia.country_restrictions.aggregator.metric_postprocess.process_tourism_availability_date import processor
from travel.avia.country_restrictions.lib.types.metric_type import TOURISM_AVAILABILITY, \
    TOURISM_AVAILABLE_FROM_METRIC_TYPE


def test_everything_is_ok():
    freeze_time(datetime.date(year=2000, month=4, day=1))
    date = datetime.date(year=2060, month=4, day=12)

    initial = {
        TOURISM_AVAILABILITY.name: TOURISM_AVAILABILITY.generate_metric(False),
        TOURISM_AVAILABLE_FROM_METRIC_TYPE.name: TOURISM_AVAILABLE_FROM_METRIC_TYPE.generate_metric(date),
    }

    result = processor('l1', initial, None)

    expected = {
        TOURISM_AVAILABILITY.name: TOURISM_AVAILABILITY.generate_metric(False),
        TOURISM_AVAILABLE_FROM_METRIC_TYPE.name: TOURISM_AVAILABLE_FROM_METRIC_TYPE.generate_metric(date),
    }

    assert result == expected


def test_country_is_opened():
    date = datetime.date(year=2000, month=4, day=12)

    initial = {
        TOURISM_AVAILABILITY.name: TOURISM_AVAILABILITY.generate_metric(True),
        TOURISM_AVAILABLE_FROM_METRIC_TYPE.name: TOURISM_AVAILABLE_FROM_METRIC_TYPE.generate_metric(date),
    }

    result = processor('l1', initial, None)

    expected = {
        TOURISM_AVAILABILITY.name: TOURISM_AVAILABILITY.generate_metric(True),
    }

    assert result == expected


def test_country_is_opened_by_date_metric():
    freeze_time(datetime.date(year=2000, month=4, day=12))
    date = datetime.date(year=2000, month=4, day=1)

    initial = {
        TOURISM_AVAILABILITY.name: TOURISM_AVAILABILITY.generate_metric(False),
        TOURISM_AVAILABLE_FROM_METRIC_TYPE.name: TOURISM_AVAILABLE_FROM_METRIC_TYPE.generate_metric(date),
    }

    result = processor('l1', initial, None)

    expected = {
        TOURISM_AVAILABILITY.name: TOURISM_AVAILABILITY.generate_metric(True),
    }

    assert result == expected
