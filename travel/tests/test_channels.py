# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from travel.rasp.bus.scripts.cache_reseter.channels import SegmentsChannel


def test_str():
    assert str(SegmentsChannel('some_partner')) == 'channel.task.some_partner.segments'
