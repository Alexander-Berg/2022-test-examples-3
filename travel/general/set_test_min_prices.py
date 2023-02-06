# -*- coding: utf-8 -*-
import logging
from datetime import datetime, timedelta
from random import randint

from travel.avia.avia_api.avia.v1.email_dispenser.commands.decorators import forbidden_environments, stdout_logger
from travel.avia.avia_api.avia.v1.model.subscriber import MinPrice, Subscription

logger = logging.getLogger(__name__)


@stdout_logger
@forbidden_environments(['production'])
def set_test_min_price_for_direction(qkey):
    """
    Устанавливаем тестовые мин цены для направления без фильтров с вариантами
    :param str qkey:
    """
    _set_test_min_price_for_direction(qkey, include_variants=True, without_filters=True)


@stdout_logger
@forbidden_environments(['production'])
def set_test_min_price_for_direction_no_variants(qkey):
    """
    Устанавливаем тестовые мин цены для направления без фильтров без вариантов
    :param str qkey:
    """
    _set_test_min_price_for_direction(qkey, include_variants=False, without_filters=True)


@stdout_logger
@forbidden_environments(['production'])
def set_test_min_price_for_filtered_direction(qkey):
    """
    Устанавливаем тестовые мин цены для направления c фильтрами с вариантами
    :param str qkey:
    """
    _set_test_min_price_for_direction(qkey, include_variants=True, with_filters=True)


@stdout_logger
@forbidden_environments(['production'])
def set_test_min_price_for_filtered_direction_no_variants(qkey):
    """
    Устанавливаем тестовые мин цены для направления c фильтрами без вариантов
    :param str qkey:
    """
    _set_test_min_price_for_direction(qkey, include_variants=False, with_filters=True)


def _set_test_min_price_for_direction(
    qkey,
    include_variants=True,
    without_filters=False,
    with_filters=False
):
    logger.info(
        'Setting test minprice for %s %s %s %s',
        qkey,
        'with variants' if include_variants else 'without variants',
        'without filters' if without_filters else '',
        'with filters' if with_filters else '',
    )
    now = datetime.utcnow()
    s = Subscription.objects.get(qkey=qkey)  # type: Subscription
    mp1, mp2 = randint(999, 9999), randint(999, 9999)
    min_prices = [
        MinPrice(time=now - timedelta(days=1), value=mp1, currency='RUR'),
        MinPrice(
            time=now, value=mp2, currency='RUR',
            variants=generate_variants() if include_variants else None
        ),
    ]
    if without_filters:
        s.min_prices = min_prices
        logger.info('Updated %s %s', mp1, mp2)
    if with_filters:
        for bundle in s.filtered_minprices:
            bundle.min_prices = min_prices
            logger.info('Updated for filter %s', bundle.filter)
    s.save()
    logger.info('Update finished')


def generate_variants():
    return {
        'min_price_variant_data': [
            {
                'national_price': 800000,
                'national_currency_id': 1,
                'forward_segments': [
                    {
                        'airline_id': 30,
                        'arrival_offset': 10800,
                        'arrival_station_id': 9600216,
                        'arrival_station_transport_type_id': 2,
                        'arrival_time': 1562991000,
                        'baggage': '1p1p10d',
                        'company_id': 30,
                        'departure_offset': 21600,
                        'departure_station_id': 9600390,
                        'departure_station_transport_type_id': 2,
                        'departure_time': 1562977800,
                        'route': 'U6 388',
                    },
                ],
                'backward_segments': [],
            },
        ],
        'popular_section_variants_data': [
            {
                'national_price': 900000,
                'national_currency_id': 1,
                'forward_segments': [
                    {
                        'airline_id': 30,
                        'arrival_offset': 10800,
                        'arrival_station_id': 9600216,
                        'arrival_station_transport_type_id': 2,
                        'arrival_time': 1562991000,
                        'baggage': '1p1p10d',
                        'company_id': 30,
                        'departure_offset': 21600,
                        'departure_station_id': 9600390,
                        'departure_station_transport_type_id': 2,
                        'departure_time': 1562977800,
                        'route': 'U6 388',
                    },
                    {
                        'airline_id': 30,
                        'arrival_offset': 10800,
                        'arrival_station_id': 9623572,
                        'arrival_station_transport_type_id': 2,
                        'arrival_time': 1563015900,
                        'baggage': '1p1p10d',
                        'company_id': 30,
                        'departure_offset': 10800,
                        'departure_station_id': 9600216,
                        'departure_station_transport_type_id': 2,
                        'departure_time': 1563007500,
                        'route': 'U6 157',
                    },
                ],
                'backward_segments': [],
            },
            {
                'national_price': 1000000,
                'national_currency_id': 1,
                'forward_segments': [
                    {
                        'airline_id': 30,
                        'arrival_offset': 10800,
                        'arrival_station_id': 9600216,
                        'arrival_station_transport_type_id': 2,
                        'arrival_time': 1562991000,
                        'baggage': '1p1p10d',
                        'company_id': 30,
                        'departure_offset': 21600,
                        'departure_station_id': 9600390,
                        'departure_station_transport_type_id': 2,
                        'departure_time': 1562977800,
                        'route': 'U6 388',
                    },
                    {
                        'airline_id': 30,
                        'arrival_offset': 10800,
                        'arrival_station_id': 9623572,
                        'arrival_station_transport_type_id': 2,
                        'arrival_time': 1563015900,
                        'baggage': '1p1p10d',
                        'company_id': 30,
                        'departure_offset': 10800,
                        'departure_station_id': 9600216,
                        'departure_station_transport_type_id': 2,
                        'departure_time': 1563007500,
                        'route': 'U6 157',
                    },
                    {
                        'airline_id': 30,
                        'arrival_offset': 10800,
                        'arrival_station_id': 9623572,
                        'arrival_station_transport_type_id': 2,
                        'arrival_time': 1563115900,
                        'baggage': '1p1p10d',
                        'company_id': 30,
                        'departure_offset': 10800,
                        'departure_station_id': 9600216,
                        'departure_station_transport_type_id': 2,
                        'departure_time': 1563015900,
                        'route': 'U6 157',
                    },
                ],
                'backward_segments': [
                    {
                        'airline_id': 30,
                        'arrival_offset': 10800,
                        'arrival_station_id': 9623572,
                        'arrival_station_transport_type_id': 2,
                        'arrival_time': 1563115900,
                        'baggage': '1p1p10d',
                        'company_id': 30,
                        'departure_offset': 10800,
                        'departure_station_id': 9600216,
                        'departure_station_transport_type_id': 2,
                        'departure_time': 1563015900,
                        'route': 'U6 157',
                    },
                ],
            },
        ],
        'other_data': [
            {
                'national_price': 1100000,
                'national_currency_id': 1,
                'forward_segments': [
                    {
                        'airline_id': 30,
                        'arrival_offset': 10800,
                        'arrival_station_id': 9600216,
                        'arrival_station_transport_type_id': 2,
                        'arrival_time': 1562991000,
                        'baggage': '1p1p10d',
                        'company_id': 30,
                        'departure_offset': 21600,
                        'departure_station_id': 9600390,
                        'departure_station_transport_type_id': 2,
                        'departure_time': 1562977800,
                        'route': 'U6 388',
                    },
                    {
                        'airline_id': 30,
                        'arrival_offset': 10800,
                        'arrival_station_id': 9623572,
                        'arrival_station_transport_type_id': 2,
                        'arrival_time': 1563015900,
                        'baggage': '1p1p10d',
                        'company_id': 30,
                        'departure_offset': 10800,
                        'departure_station_id': 9600216,
                        'departure_station_transport_type_id': 2,
                        'departure_time': 1563007500,
                        'route': 'U6 157',
                    },
                ],
                'backward_segments': [],
            },
        ],
    }
