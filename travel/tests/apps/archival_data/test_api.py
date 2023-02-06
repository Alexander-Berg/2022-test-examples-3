# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from hamcrest import assert_that, has_entries, contains_inanyorder

from common.apps.archival_data.api import get_archival_data, get_archival_segments
from common.apps.archival_data.factories import ArchivalSearchDataFactory, ArchivalSettlementsDataFactory
from common.models.transport import TransportType
from common.tester.factories import create_station, create_settlement


pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


def test_get_archival_data():
    settlement_from = create_settlement(slug='settlement_slug_from')
    settlement_to = create_settlement(slug='settlement_slug_to')
    station_from = create_station(settlement=settlement_from, slug='slug_from')
    station_to = create_station(settlement=settlement_to, slug='slug_to')

    archival_data = get_archival_data(point_from=station_from, point_to=station_to)
    assert archival_data is None

    ArchivalSearchDataFactory(
        point_from=station_from.point_key,
        point_to=station_to.point_key,
        transport_type=TransportType.TRAIN_ID
    )

    archival_data = get_archival_data(point_from=station_from, point_to=station_to)
    assert_that(archival_data,
                has_entries({
                    'transport_types': ['train'],
                    'canonical': has_entries({
                        'point_from': station_from,
                        'point_to': station_to,
                        'transport_type': 'train'
                    })
                }))

    ArchivalSearchDataFactory(
        point_from=station_from.point_key,
        point_to=station_to.point_key,
        transport_type=TransportType.BUS_ID
    )
    archival_data = get_archival_data(point_from=station_from, point_to=station_to)
    assert_that(archival_data,
                has_entries({
                    'transport_types': ['train', 'bus'],
                    'canonical': has_entries({
                        'point_from': station_from,
                        'point_to': station_to,
                        'transport_type': None
                    })
                }))

    archival_data = get_archival_data(point_from=station_from, point_to=station_to, valid_transport_types={'bus'})
    assert_that(archival_data,
                has_entries({
                    'transport_types': ['bus'],
                    'canonical': has_entries({
                        'transport_type': 'bus'
                    })
                }))

    archival_data = get_archival_data(point_from=station_from, point_to=station_to, valid_transport_types={'train'})
    assert_that(archival_data,
                has_entries({
                    'transport_types': ['train'],
                    'canonical': has_entries({
                        'transport_type': 'train'
                    })
                }))

    archival_data = get_archival_data(point_from=station_from, point_to=station_to, valid_transport_types={'plane'})
    assert archival_data is None

    archival_data = get_archival_data(point_from=settlement_from, point_to=settlement_to)
    assert archival_data is None

    ArchivalSearchDataFactory(
        point_from=settlement_from.point_key,
        point_to=settlement_to.point_key,
        transport_type=TransportType.PLANE_ID
    )
    archival_data = get_archival_data(point_from=settlement_from, point_to=settlement_to)
    assert_that(archival_data,
                has_entries({
                    'transport_types': ['plane'],
                    'canonical': has_entries({
                        'point_from': settlement_from,
                        'point_to': settlement_to,
                        'transport_type': 'plane'
                    })
                }))


def test_get_archival_segments():
    settlement_from = create_settlement(slug='settlement_slug_from')
    settlement_to = create_settlement(slug='settlement_slug_to')
    station_from = create_station(settlement=settlement_from, slug='slug_from')
    station_to = create_station(settlement=settlement_to, slug='slug_to')

    segments = get_archival_segments(point_from=settlement_from, point_to=settlement_to)
    assert segments is None

    ArchivalSettlementsDataFactory(
        point_from=settlement_from.point_key,
        point_to=settlement_to.point_key,
        transport_type=TransportType.TRAIN_ID,
        segments=[
            {
                'title': 'old_data',
                'station_from': {'id': station_from.id},
                'station_to': {'id': station_to.id},
                'transport_type': {
                    'id': TransportType.TRAIN_ID,
                    'code': 'train'
                }
            }
        ]
    )

    segments = get_archival_segments(point_from=settlement_from, point_to=settlement_to)
    assert_that(segments, contains_inanyorder(
        has_entries({
            'title': 'old_data',
            'station_from': {'id': station_from.id},
            'station_to': {'id': station_to.id},
            'transport_type': {
                'id': TransportType.TRAIN_ID,
                'code': 'train'
            }
        })
    ))

    segments = get_archival_segments(point_from=settlement_from, point_to=settlement_to, valid_transport_types=['bus'])
    assert segments is None

    segments = get_archival_segments(point_from=settlement_from, point_to=settlement_to, valid_transport_types=['train'])
    assert_that(segments, contains_inanyorder(
        has_entries({
            'title': 'old_data',
            'transport_type': {
                'id': TransportType.TRAIN_ID,
                'code': 'train'
            }
        })
    ))


def test_get_archival_segments_extended_settlements():
    settlement_from = create_settlement(slug='settlement_slug_from')
    settlement_to = create_settlement(slug='settlement_slug_to')
    station_from = create_station(slug='slug_from')
    station_from_2 = create_station(settlement=settlement_from, slug='slug_from_2')
    station_to = create_station(settlement=settlement_to, slug='slug_to')
    station_to_2 = create_station(settlement=settlement_to, slug='slug_to_2')

    ArchivalSettlementsDataFactory(
        point_from=settlement_from.point_key,
        point_to=settlement_to.point_key,
        transport_type=TransportType.TRAIN_ID,
        segments=[
            {
                'title': 'basic_segment',
                'station_from': {'id': station_from.id},
                'station_to': {'id': station_to.id}
            },
            {
                'title': 'filtered_segment_from',
                'station_from': {'id': station_from_2.id},
                'station_to': {'id': station_to.id}
            },
            {
                'title': 'filtered_segment_to',
                'station_from': {'id': station_from.id},
                'station_to': {'id': station_to_2.id}
            }
        ]
    )

    segments = get_archival_segments(point_from=station_from, point_to=station_to)
    assert segments is None

    station_from.settlement = settlement_from
    station_from.save()
    segments = get_archival_segments(point_from=station_from, point_to=station_to)
    assert_that(segments, contains_inanyorder(
        has_entries({
            'title': 'basic_segment',
            'station_from': {'id': station_from.id},
            'station_to': {'id': station_to.id}
        })
    ))

    segments = get_archival_segments(point_from=settlement_from, point_to=settlement_to)
    assert_that(segments, contains_inanyorder(
        has_entries({'title': 'basic_segment'}),
        has_entries({'title': 'filtered_segment_from'}),
        has_entries({'title': 'filtered_segment_to'})
    ))


def test_get_archival_segments_several_types():
    settlement_from = create_settlement(slug='settlement_slug_from')
    settlement_to = create_settlement(slug='settlement_slug_to')
    station_from = create_station(settlement=settlement_from, slug='slug_from')
    station_to = create_station(settlement=settlement_to, slug='slug_to')

    for t_type_id in [TransportType.TRAIN_ID, TransportType.PLANE_ID, TransportType.BUS_ID]:
        ArchivalSettlementsDataFactory(
            point_from=settlement_from.point_key,
            point_to=settlement_to.point_key,
            transport_type=t_type_id,
            segments=[
                {
                    'title': 'old_data',
                    'station_from': {'id': station_from.id},
                    'station_to': {'id': station_to.id},
                    'transport_type': {
                        'id': t_type_id,
                        'code': TransportType.objects.get(id=t_type_id).code
                    }
                }
            ]
        )

    segments = get_archival_segments(point_from=settlement_from, point_to=settlement_to)
    assert_that(segments, contains_inanyorder(
        has_entries({
            'transport_type': {
                'id': TransportType.TRAIN_ID,
                'code': 'train'
            }
        }),
        has_entries({
            'transport_type': {
                'id': TransportType.PLANE_ID,
                'code': 'plane'
            }
        }),
        has_entries({
            'transport_type': {
                'id': TransportType.BUS_ID,
                'code': 'bus'
            }
        })
    ))

    segments = get_archival_segments(point_from=settlement_from, point_to=settlement_to, valid_transport_types=['bus'])
    assert_that(segments, contains_inanyorder(
        has_entries({
            'transport_type': {
                'id': TransportType.BUS_ID,
                'code': 'bus'
            }
        })
    ))
