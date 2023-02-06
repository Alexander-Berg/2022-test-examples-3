# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from config.common import *


MONITORING = dict(MONITORING, **{
    'common_labels': dict(MONITORING['common_labels'], **{
        'cluster': 'testing',
    })
})

YANDEX_BUS_API = getenv('YANDEX_BUS_API') or 'https://testing.api.internal.bus.yandex.net'

CONNECTOR_SECRET_NAME = 'bus-connectors-test'


class Log(Log):
    CONNECTOR_RESPONSE_LEVEL = 'DEBUG'
