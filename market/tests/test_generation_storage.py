# -*- coding: utf-8 -*-
import os
import time

from pyb.qpipe.generation_storage import GenerationStorage, _Generation
from market.pylibrary.mi_util import util


def ts_to_gen_name(ts):
    return time.strftime('%Y%m%d_%H%M%S', time.localtime(ts))


def test_generation(tmpdir):
    ts = 1483221661  # 20170101_010101
    NAME = ts_to_gen_name(ts)
    g = _Generation(str(tmpdir / NAME))
    assert g == g
    assert g.file_path('data/shard.pbuf.sn') == str(tmpdir / NAME / 'data/shard.pbuf.sn')
    assert str(g) == str(tmpdir / NAME)
    assert g.timestamp == ts
    assert g.name == NAME


def test_add_generation(tmpdir):
    gs = GenerationStorage(str(tmpdir / 'storage'))
    generation = gs.add('20170101_010101')
    assert generation.name in gs
    assert os.path.exists(generation.path)
    assert generation == gs['20170101_010101']


def test_rotate(tmpdir):
    now = util.now()
    SECONDS_IN_HOUR = 3600
    gs = GenerationStorage(str(tmpdir))
    generation1 = gs.add(ts_to_gen_name(now))
    generation2 = gs.add(ts_to_gen_name(now - SECONDS_IN_HOUR * 2))
    generation3 = gs.add(ts_to_gen_name(now - SECONDS_IN_HOUR * 3))
    gs.rotate(SECONDS_IN_HOUR)
    assert generation1 in gs
    assert generation2 not in gs
    assert generation3 not in gs


def test_timestamp(tmpdir):
    gs = GenerationStorage(str(tmpdir / 'storage'))
    generation = gs.add('20170101_010101')
    tmpdir.join('storage/20170101_010101/timestamp').write('1000')
    assert generation.timestamp == 1000
    generation.timestamp = 2000
    assert generation.timestamp == 2000
    generation.timestamp = '3000'
    assert generation.timestamp == 3000


def test_finished(tmpdir):
    gs = GenerationStorage(str(tmpdir / 'storage'))
    generation = gs.add('20170101_010101')
    assert generation.finished is False
    generation.finished = True
    assert generation.finished is True
    generation.finished = False
    assert generation.finished is False
