# coding: utf-8

import os
import tempfile
import market.pylibrary.lenval_stream as lenval_stream

import yatest.common

from market.idx.yatf.resources.resource import FileResource
from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.test_envs.base_env import BaseEnv
from market.idx.yatf.utils.mmap.mmapviewer import json_view
from market.pylibrary.proto_utils import message_from_data

from market.proto.feedparser.deprecated.OffersData_pb2 import Offer
from market.proto.content.pictures_pb2 import Picture


def picToLenval(pic):
    with tempfile.TemporaryFile() as temp:
        lenval_stream.write(temp, [pic.SerializeToString()])
        temp.seek(0)
        return temp.read()


class GiftsTable(YtTableResource):
    def __init__(self, yt_stuff, path, data):
        self._data = [{
            'feed_id': d.get('feed_id', 1),
            'session_id': d.get('session_id', 1),
            'offer_id': d.get('offer_id', '1'),
            'offer': message_from_data(d['offer'], Offer()).SerializeToString(),
            'pic': picToLenval(message_from_data(d['pic'], Picture())),
        } for d in data]
        self.yt_client = yt_stuff.get_yt_client()

        super(GiftsTable, self).__init__(
            yt_stuff=yt_stuff,
            path=path,
            data=self._data,
            attributes=dict(
                dynamic=True,
                external=False,
                schema=[
                    dict(name="feed_id", type="uint64"),
                    dict(name="session_id", type="uint64"),
                    dict(name="offer_id", type="string"),
                    dict(name="offer", type="string"),
                    dict(name="pic", type="string"),
                ]
            )
        )


def _cmp_strings(a, b):
    if a > b:
        return 1
    elif a < b:
        return -1
    else:
        return 0


def _cmp_gifts(a, b):
    res = _cmp_strings(a['feed_id'], b['feed_id'])
    if res == 0:
        return _cmp_strings(a['gift_id'], b['gift_id'])
    return res


class GiftsResult(FileResource):
    def __init__(self, path):
        super(GiftsResult, self).__init__(path)
        self._result = []

    def load(self):
        self._result = []
        if os.path.exists(self.path):
            code, self._result = json_view(self.path)
            if code:
                raise RuntimeError('mmapviwer reaturned code {}'.format(code))
            self._result = sorted(self._result['promo_gifts'], cmp=_cmp_gifts)

    @property
    def result(self):
        return self._result


class GiftsIndexerTestEnv(BaseEnv):

    def __init__(self, yt_stuff, **resources):
        super(GiftsIndexerTestEnv, self).__init__(**resources)
        self._yt_stuff = yt_stuff

    def __enter__(self):
        super(GiftsIndexerTestEnv, self).__enter__()
        return self

    def __exit__(self, *args):
        pass

    @property
    def description(self):
        return 'gifts_indexer'

    @property
    def gifts_table(self):
        return self.resources['gifts_table']

    @property
    def gifts(self):
        if not self.outputs['gifts'].result:
            self.outputs['gifts'].load()
        return self.outputs['gifts'].result

    def execute(self, path=None):
        self.do_execute(path=path)

    def do_execute(self, path=None):
        if path is None:
            relative_path = os.path.join('market', 'idx', 'promos', 'gifts_indexer', 'bin',
                                         'gifts_indexer')
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        if not os.path.exists(self.output_dir):
            os.makedirs(self.output_dir)

        mmap_file = os.path.join(self.output_dir, 'gifts.mmap')
        cmd = [
            path,
            '--yt-proxy', self._yt_stuff.get_server(),
            '--yt-token-path', '',
            '--yt-src', self.gifts_table.get_path(),
            '--dst', mmap_file,
        ]
        self.exec_result = self.try_execute_under_gdb(
            cmd,
            cwd=self.output_dir,
            check_exit_code=True
        )

        self.outputs.update({
            'gifts': GiftsResult(mmap_file)
        })
