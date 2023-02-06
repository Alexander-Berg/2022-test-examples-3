# -*- coding: utf-8 -*-
from __future__ import absolute_import, division, print_function, unicode_literals

from pytest import mark

from travel.rasp.mysql_dumper.lib.protos.rthread_pb2 import TRThread
from travel.rasp.mysql_dumper.lib.dumpers import RThreadDumper
from travel.rasp.mysql_dumper.lib.factory import Factory


@mark.parametrize('title_common, expected', [
    (
        '{"type":"default","title_parts":["c22149","c24208"]}',
        Factory(
            TRThread.TRThreadTitle(),
            Factory.set_field('type', TRThread.TRThreadTitle.DEFAULT),
            Factory.add_to('title_parts', Factory.set_field('settlement', 22149)),
            Factory.add_to('title_parts', Factory.set_field('settlement', 24208)),
        ).get()
    ),

    (
        '{"type":"mta","title_parts":["c216","c34246"]}',
        Factory(
            TRThread.TRThreadTitle(),
            Factory.set_field('type', TRThread.TRThreadTitle.MTA),
            Factory.add_to('title_parts', Factory.set_field('settlement', 216)),
            Factory.add_to('title_parts', Factory.set_field('settlement', 34246)),
        ).get()
    ),

    (
        '{"type":"suburban","title_parts":["s9600215","s2000007"]}',
        Factory(
            TRThread.TRThreadTitle(),
            Factory.set_field('type', TRThread.TRThreadTitle.SUBURBAN),
            Factory.add_to('title_parts', Factory.set_field('station', 9600215)),
            Factory.add_to('title_parts', Factory.set_field('station', 2000007)),
        ).get()
    ),

    (
        '{"type":"suburban","is_combined":true,'
        '"title_parts":["s2000345","s9601027","s9601461"],"t_type":"suburban"}',
        Factory(
            TRThread.TRThreadTitle(),
            Factory.set_field('type', TRThread.TRThreadTitle.SUBURBAN),
            Factory.set_field('is_combined', True),
            Factory.set_field('t_type', TRThread.SUBURBAN),
            Factory.add_to('title_parts', Factory.set_field('station', 2000345)),
            Factory.add_to('title_parts', Factory.set_field('station', 9601027)),
            Factory.add_to('title_parts', Factory.set_field('station', 9601461)),
        ).get(),
    ),

    (
        '{"add_ring":true,"type":"suburban","title_parts":["s9614088"]}',
        Factory(
            TRThread.TRThreadTitle(),
            Factory.set_field('type', TRThread.TRThreadTitle.SUBURBAN),
            Factory.set_field('is_ring', True),
            Factory.add_to('title_parts', Factory.set_field('station', 9614088))
        ).get(),
    ),

    (
        '{"add_circular_mta":true,"type":"mta","title_parts":["c217"]}',
        Factory(
            TRThread.TRThreadTitle(),
            Factory.set_field('type', TRThread.TRThreadTitle.MTA),
            Factory.set_field('is_ring', True),
            Factory.add_to('title_parts', Factory.set_field('settlement', 217))
        ).get(),
    ),

    (
        '{"type":"mta","title_parts":'
        '[{"settlement":"c215","station":"s9739657","type":"mta_station_with_city"},"s9822081"]}',
        Factory(
            TRThread.TRThreadTitle(),
            Factory.set_field('type', TRThread.TRThreadTitle.MTA),
            Factory.add_to(
                'title_parts',
                Factory.set_field('settlement', 215),
                Factory.set_field('station', 9739657),
            ),
            Factory.add_to('title_parts', Factory.set_field('station', 9822081))
        ).get(),
    )

])
def test_parse_title_common(title_common, expected):
    result = TRThread()
    RThreadDumper()._parse_title_common(result, title_common)
    assert result.title_common == expected
