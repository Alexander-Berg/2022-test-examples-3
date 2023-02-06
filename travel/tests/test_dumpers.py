# -*- coding: utf-8 -*-
from __future__ import absolute_import

from datetime import timedelta
from itertools import izip

from mock import Mock, patch
from pytest import mark

from travel.rasp.mysql_dumper.lib.dumpers import BaseDumper, RThreadDumper, RTStationDumper, SettlementDumper
from travel.rasp.mysql_dumper.lib.normalizer import CACHE
from travel.rasp.mysql_dumper.lib.protos.settlement_pb2 import TSettlement
from travel.rasp.mysql_dumper.lib.protos.rthread_pb2 import TRThread
from travel.rasp.mysql_dumper.lib.protos.rtstation_pb2 import TThreadStation, TThreadStationPack
from travel.rasp.mysql_dumper.lib.factory import Factory


@mark.parametrize('fields, values', [
    (['x'], [1]),
    (['x', 'y'], ['a', 2]),
])
def test_base_init_object(fields, values, proto_class=Mock, expected=None, rules=dict()):
    class TestDumper(BaseDumper):
        _rules = rules
        FIELDS = fields
    dumper = TestDumper(proto_class, object)
    proto_object = proto_class()
    dumper._init_object(proto_object, values)
    for name, value in izip(fields, expected if expected is not None else values):
        assert getattr(proto_object, name) == value


@mark.parametrize('values, expected', [
    (
        [
            2, 'Санкт-Петербург', 'Санкт-Петербурге', 'Санкт-Петербурга', 'Санкт-Петербург',
            'Санкт-Петербургу', None, 'Saint-Petersburg', 'Спб', 'в', 'Санкт-Петербурга',
            'Санкт-Петербург', 'Санкт-Петербурге', 'Санкт-Петербург', '', '', 2, 225, 10174,
            None, 'Europe/Moscow', 2, 0, 'saint-petersburg', 30.315868, 59.939095,
        ],

        [
            2, 'Санкт-Петербург', 'Санкт-Петербурге', 'Санкт-Петербурга', 'Санкт-Петербург',
            'Санкт-Петербургу', '', 'Saint-Petersburg', 'Спб', 'в', 'Санкт-Петербурга',
            'Санкт-Петербург', 'Санкт-Петербурге', 'Санкт-Петербург', '', '', TSettlement.REGION_CAPITAL_ID, 225, 10174,
            0, 1, 2, 0, 'saint-petersburg', 30.315868, 59.939095,
        ]
    ),

    (
        [
            4, 'Белгород', 'Белгороде', 'Белгорода', 'Белгород', 'Белгороду', None, 'Belgorod',
            'Блг', 'в', 'Белгорода', 'Белгород', 'Белгороде', 'Белгород', None, None, 2, 225, 10645,
            None, 'Europe/Moscow', 4, 0, 'belgorod', 36.588849, 50.597467
        ],

        [
            4, 'Белгород', 'Белгороде', 'Белгорода', 'Белгород', 'Белгороду', '', 'Belgorod',
            'Блг', 'в', 'Белгорода', 'Белгород', 'Белгороде', 'Белгород', '', '', TSettlement.REGION_CAPITAL_ID, 225,
            10645, 0, 1, 4, 0, 'belgorod', 36.588849, 50.597467
        ]
    ),
])
def test_settlement_init(values, expected):
    with patch.dict(CACHE.timezone_ids, {'Europe/Moscow': 1}):
        test_base_init_object(
            SettlementDumper.FIELDS,
            values,
            TSettlement,
            expected,
            SettlementDumper._rules
        )


@mark.parametrize('values, expected', [
    (
        (916742644, 9776097, 0, None, 'Europe/Astrakhan', 1, 1, 0, 0),
        (916742644, 9776097, 0, 0, 4, 1, 1, 0, 0)
    ),
    (
        (916742645, 9633186, 5, 4, 'Europe/Astrakhan', 0, 1, 0, 0),
        (916742645, 9633186, 5, 4, 4, 0, 1, 0, 0),
    )
])
def test_rtstation_init(values, expected):
    with patch.dict(CACHE.timezone_ids, {'Europe/Astrakhan': 4}):
        test_base_init_object(
            RTStationDumper.FIELDS,
            values,
            TThreadStation,
            expected,
            RTStationDumper._rules
        )


@mark.parametrize('values, expected', [
    (
        [
            323485305,
            '0100001010000101000010100001010'
            '0001010000101000010100001010000'
            '0001010000101000000000000000000'
            '0000000000000000000000000000000'
            '0000001010000101000010100001010'
            '0001010000101000010100001010000'
            '0101000010100001010000101000010'
            '1000010100001010000101000010100'
            '0010100001010000101000010100000'
            '1010000101000010100001010000101'
            '0000101000010100001010000101000'
            '0010100001010000101000010100001',
            timedelta(hours=14, minutes=25),
            1,
            '{"type":"default","title_parts":["c22149","c24208"]}',
            '',
            1
        ],

        Factory(
            TRThread(),
            Factory.set_field('id', 323485305),
            Factory.extend(
                'year_days',
                558007562,
                169093200,
                169082880,
                0,
                21136650,
                169093200,
                676372802,
                1116015124,
                338186400,
                1352745605,
                84546600,
                338186401
            ),
            Factory.set_field('tz_start_time', (14 * 60 + 25) * 60),
            Factory.set_field('type_id', TRThread.BASIC_ID),
            Factory.set_field('number', ''),
            Factory.set_field_attr(
                'title_common',
                Factory.set_field('type', TRThread.TRThreadTitle.DEFAULT),
                Factory.add_to('title_parts', Factory.set_field('settlement', 22149)),
                Factory.add_to('title_parts', Factory.set_field('settlement', 24208))
            ),
            Factory.set_field('t_type_id', TRThread.TRAIN)
        ).get()
    )
])
def test_rthread(values, expected):
    dumper = RThreadDumper()
    proto_object = TRThread()
    dumper._init_object(proto_object, values)
    assert proto_object == expected


@mark.parametrize('values, expected', [
    (
        [
            [   # wrong type and type_id
                323485305,
                '0100001010000101000010100001010'
                '0001010000101000010100001010000'
                '0001010000101000000000000000000'
                '0000000000000000000000000000000'
                '0000001010000101000010100001010'
                '0001010000101000010100001010000'
                '0101000010100001010000101000010'
                '1000010100001010000101000010100'
                '0010100001010000101000010100000'
                '1010000101000010100001010000101'
                '0000101000010100001010000101000'
                '0010100001010000101000010100001',
                timedelta(hours=14, minutes=25),
                1,
                '{"type":"urban","title_parts":["c22149","c24208"]}',
                '',
                8
            ],
        ],

        []
    ),
])
def test_rthread_bad_data(values, expected):
    dumper = RThreadDumper()
    proto_object = dumper.get_objects_from_data(values)
    assert proto_object == expected


@mark.parametrize('dumper_class, data, expected', [
    (
        RTStationDumper,
        [
            (916742644, 9776097, 0, None, 'Europe/Astrakhan', 1, 1, 0, 0, 323485305),
            (916742645, 9633186, None, 4, 'Europe/Astrakhan', 0, 1, 0, 0, 323485305),
        ],

        Factory(
            TThreadStationPack(),
            Factory.add_to(
                'items',
                Factory.set_field('id', 916742644),
                Factory.set_field('station_id', 9776097),
                Factory.set_field('has_departure', True),
                Factory.set_field('tz_departure', 0),
                Factory.set_field('has_arrival', False),
                Factory.set_field('tz_arrival', 0),
                Factory.set_field('time_zone', 4),
                Factory.set_field('is_searchable_from', True),
                Factory.set_field('is_searchable_to', True),
                Factory.set_field('departure_code_sharing', False),
                Factory.set_field('arrival_code_sharing', False),
                Factory.set_field('thread_id', 323485305),
            ),
            Factory.add_to(
                'items',
                Factory.set_field('id', 916742645),
                Factory.set_field('station_id', 9633186),
                Factory.set_field('has_departure', False),
                Factory.set_field('tz_departure', 0),
                Factory.set_field('has_arrival', True),
                Factory.set_field('tz_arrival', 4),
                Factory.set_field('time_zone', 4),
                Factory.set_field('is_searchable_from', False),
                Factory.set_field('is_searchable_to', True),
                Factory.set_field('departure_code_sharing', False),
                Factory.set_field('arrival_code_sharing', False),
                Factory.set_field('thread_id', 323485305),
            )
        ).get()
    )
])
def test_dumps(dumper_class, data, expected):
    with patch.dict(CACHE.timezone_ids, {'Europe/Astrakhan': 4}):
        dumper = dumper_class()
        result = dumper.pack_class()
        result.ParseFromString(dumper.dumps(data))
        assert result == expected
