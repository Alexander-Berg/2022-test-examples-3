# -*- coding: utf-8 -*-

import ujson
from library.python import resource

from travel.avia.library.python.ticket_daemon.memo import memoize


class SampleSearchResultProvider(object):
    @memoize(keyfun=lambda _: True)
    def get(self):
        filename = 'resfs/file/travel/avia/ticket_daemon/tests/samples/sample_search_result.json'
        return ujson.loads(resource.find(filename))
