# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from io import BytesIO

from travel.proto.dicts.rasp.carrier_pb2 import TCarrier
from travel.library.python.dicts.base_repository import BaseRepository, BaseListRepository


def test_base_repository():

    for baseClass in [BaseRepository, BaseListRepository]:

        class TestRepository(baseClass):
            _PB = TCarrier

        carrier1 = TCarrier(Id=0, Title='title1', Url='url1')
        carrier2 = TCarrier(Id=1, Title='title2', Url='url2')

        repository1 = TestRepository()
        repository1.add_object(carrier1)
        repository1.add_object(carrier2)

        assert repository1.size() == 2
        assert repository1.get(carrier1.Id) == carrier1

        stream = BytesIO()
        repository1.dump_to_stream(stream)
        repository2 = TestRepository()
        repository2.load_from_string(stream.getvalue())

        assert repository1.size() == repository2.size()
        for record1 in repository1.itervalues():
            record2 = repository2.get(record1.Id)
            assert record1 == record2
