# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from common.tester.factories import ModelFactory, factories
from stationschedule.models import ZTablo2


class ZTablo2Factory(ModelFactory):
    Model = ZTablo2
    default_kwargs = {
        'station': {},
    }

create_ztablo = ZTablo2Factory()
factories[ZTablo2] = create_ztablo
