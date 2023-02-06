# -*- coding: utf-8 -*-
import pytest
from mock import Mock

from travel.avia.library.python.tester.factories import create_partner, create_instance_by_abstract_class

from travel.avia.ticket_daemon.ticket_daemon.lib.yt_loggers.min_price_logger import MinPriceLogger
from travel.avia.ticket_daemon.ticket_daemon.lib.yt_loggers.abstract_yt_logger import IObjectLogger
from travel.avia.ticket_daemon.ticket_daemon.lib.min_prices import MinPriceVariantsSameStops
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon.ticket_daemon.daemon_tester import (
    create_query, create_price, create_variant, create_flight
)


def setup_params():
    reset_all_caches()

    partner = create_partner(
        code='dohop'
    )

    query = create_query()

    variant = create_variant(
        query=query,
        partner=partner,
        forward_flights=[
            create_flight(iata_from='V1-F1-FROM', iata_to='V1-F1-FROM'),
            create_flight(iata_from='V1-F2-FROM', iata_to='V1-F2-FROM')
        ]
    )
    other_variant = create_variant(
        query=query,
        partner=partner,
        forward_flights=[
            create_flight(iata_from='V2-F1-FROM', iata_to='V2-F1-FROM'),
            create_flight(iata_from='V2-F2-FROM', iata_to='V2-F2-FROM')
        ]
    )

    fake_logger = create_instance_by_abstract_class(IObjectLogger)
    actual_logger_calls = []
    fake_logger.log_many = Mock(side_effect=actual_logger_calls.append)

    min_price_logger = MinPriceLogger(
        logger=fake_logger
    )

    common_logged_data = {
        'forward_routes': 'V2-F1-FROM V2-F1-FROM,V2-F2-FROM V2-F2-FROM;V1-F1-FROM V1-F1-FROM,V1-F2-FROM V1-F2-FROM',
        'passengers': '1_0_0',
        'direction': '{}_{}'.format(query.point_from.point_key, query.point_to.point_key),
        'original_price': 100,
        'partners': 'dohop',
        'service': 'ticket',
        'national_version': 'ru',
        'original_currency': 'USD',
        'qid': '{}'.format(query.id),
        'price': 1000,
        'date_backward': '',
        'stops': '2-0',
        'backward_routes': '',
        'currency': 'RUR',
        'klass': 'economy',
        'date_forward': '2017-09-01',
    }

    return (
        min_price_logger, fake_logger, query, partner,
        variant, other_variant, common_logged_data, actual_logger_calls
    )


@pytest.mark.dbuser
def test_empty():
    (
        min_price_logger, fake_logger, query,
        partner, variant, other_variant,
        common_logged_data, actual_logger_calls
    ) = setup_params()
    min_price_logger.log(
        query=query,
        same_stops_price_importations=[]
    )

    assert len(actual_logger_calls) == 1
    assert list(actual_logger_calls[0]) == []


@pytest.mark.dbuser
def test_common():
    (
        min_price_logger, fake_logger, query,
        partner, variant, other_variant,
        common_logged_data, actual_logger_calls
    ) = setup_params()

    min_price_logger.log(
        query=query,
        same_stops_price_importations=[
            MinPriceVariantsSameStops(
                variants=[variant, other_variant],
                stops='2-0',
                national_tariff=create_price(),
                tariff=create_price(100, 'USD')
            )
        ]
    )

    assert len(actual_logger_calls) == 1
    assert list(actual_logger_calls[0]) == [common_logged_data]
