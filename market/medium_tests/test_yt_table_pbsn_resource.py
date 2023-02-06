# -*- coding: utf8 -*-
from tempfile import NamedTemporaryFile

from unittest import TestCase

from mapreduce.yt.python.yt_stuff import YtStuff

from getter.service.yt_resource.yt_table import YtTablePbsnResource
from getter.service.yt_express_warehouses import ExpressWarehousesPbsnFormatter

import market.pylibrary.snappy_protostream as snappy_protostream
import market.proto.delivery.ExpressWarehouses_pb2 as ExpressWarehousesProto


def download_and_read(source, resent_result):
    with NamedTemporaryFile() as fd:
        result = source.download(fd.name, None, resent_result)

        data = []
        try:
            root = next(message for message in snappy_protostream.pbsn_reader(fd.name, "EXWH", ExpressWarehousesProto.Root))
            for warehouse_id, express_warehouse in root.express_warehouses.iteritems():
                data.append({
                    'warehouse_id': warehouse_id,
                    'location_id': express_warehouse.region_id,
                    'latitude': express_warehouse.latitude,
                    'longitude': express_warehouse.longitude,
                })
        except:
            pass

        return result, data


VALID_TABLE_CONTENTS = [
    {
        'warehouse_id': 101,
        'location_id': 213,
        'latitude': 57.5,
        'longitude': 37.1,
    },
    {
        'warehouse_id': 102,
        'location_id': 214,
        'latitude': 57.8,
        'longitude': 37.4,
    },
]

INVALID_TABLE_CONTENTS_1 = [
    {
        'warehouse_id': None,
        'location_id': 213,
        'latitude': 57.5,
        'longitude': 37.1,
    },
]

INVALID_TABLE_CONTENTS_2 = [
    {
        'warehouse_id': 103,
        'location_id': None,
        'latitude': 57.5,
        'longitude': 37.1,
    },
    {
        'warehouse_id': 104,
        'location_id': -1,
        'latitude': 57.5,
        'longitude': 37.1,
    },
    {
        'warehouse_id': 105,
        'location_id': 213,
        'latitude': None,
        'longitude': None,
    },
    {
        'warehouse_id': 0x100000000,
        'location_id': 213,
        'latitude': 57.5,
        'longitude': 37.1,
    },
]


def keyify_data(data, key_name):
    result = dict()
    for item in data:
        key = item.get(key_name)
        result[key] = item
    return result


def create_tables(yt_client):
    yt_client.create('table', "//tmp/empty_table/latest", recursive=True)

    # See data schema here:
    # https://yt.yandex-team.ru/hahn/navigation?navmode=schema&path=//home/market/production/indexer/combinator/express.v2/recent

    yt_client.create(
        'table',
        "//tmp/table_with_data/2019-11-20",
        recursive=True,
        attributes={
            'schema': [
                {'name': 'warehouse_id', 'type': 'int64', 'sort_order': 'ascending'},
                {'name': 'location_id', 'type': 'int32'},
                {'name': 'latitude', 'type': 'double'},
                {'name': 'longitude', 'type': 'double'},
            ],
            'strict': True,
        },
    )
    yt_client.write_table(
        "<sorted_by=[warehouse_id]>//tmp/table_with_data/2019-11-20",
        INVALID_TABLE_CONTENTS_1 + VALID_TABLE_CONTENTS + INVALID_TABLE_CONTENTS_2,  # two chunks of invalid data because of sorting restriction
        format='json',
        raw=False,
    )
    yt_client.link("//tmp/table_with_data/2019-11-20", "//tmp/table_with_data/recent")


class TestYtTablePbsnResource(TestCase):
    @classmethod
    def setUpClass(cls):
        cls._yt_stuff = YtStuff()
        cls._yt_stuff.start_local_yt()

        cls._yt_client = cls._yt_stuff.get_yt_client()

        create_tables(cls._yt_client)

    @classmethod
    def tearDownClass(cls):
        cls._yt_stuff.stop_local_yt()

    def test_nonexistent_table_reading(self):
        """Попытка чтения из несуществующей таблицы в YT"""
        table = YtTablePbsnResource(
            yt=self._yt_client,
            path="//home/nonexistent/table",
            link_name="latest",
            name="nonexistent_table",
            magic="EXWH",
            formatter=ExpressWarehousesPbsnFormatter(),
        )

        result, _ = download_and_read(table, {})

        self.assertDictEqual(result, {'code': 500})

    def test_empty_table_reading(self):
        """Чтение из пустой таблицы в YT"""
        table = YtTablePbsnResource(
            yt=self._yt_client,
            path="//tmp/empty_table",
            link_name="latest",
            name="empty_table",
            magic="EXWH",
            formatter=ExpressWarehousesPbsnFormatter(),
        )

        result, data = download_and_read(table, {})

        self.assertDictEqual(result, {'yt_gen': "latest", 'code': 200})
        self.assertEqual(data, [])

    def test_table_link(self):
        """Чтение из таблицы по ссылке"""
        table = YtTablePbsnResource(
            yt=self._yt_client,
            path="//tmp/table_with_data",
            link_name="recent",
            name="recent",
            magic="EXWH",
            formatter=ExpressWarehousesPbsnFormatter(),
        )

        result, data = download_and_read(table, {})

        self.assertDictEqual(result, {'yt_gen': "2019-11-20", 'code': 200})
        self.assertEqual(
            keyify_data(data, 'warehouse_id'),
            keyify_data(VALID_TABLE_CONTENTS, 'warehouse_id'),
        )
