# coding: utf-8

import socket
import time
import unittest
from datetime import datetime, timedelta
from six.moves.configparser import ConfigParser

from market.pylibrary.dyt import dyt

from mapreduce.yt.python.yt_stuff import YtStuff, YtConfig

from market.idx.datacamp.parser.lib.parser_engine.feed import FeedData
from market.idx.datacamp.parser.lib.parser_engine.yt_service import YtFastDynTable
from market.idx.datacamp.parser.lib.parser_engine.yt_service import update_qsession
from market.idx.datacamp.parser.lib.parser_engine.yt_service import setup_from_config
from market.idx.datacamp.parser.lib.parser_engine.yt_service import get_raw_upload_timestamps
from market.idx.datacamp.parser.lib.parser_engine.yt_service import update_raw_upload_touched_time
from market.idx.datacamp.parser.lib.parser_engine.yt_service import update_raw_upload_uploaded_time
from market.idx.datacamp.parser.lib.parser_engine.yt_service import update_monitoring_for_feed


YT_SERVER = None
SIMPLE_SCHEMA = [
    dict(name='key', type='uint32', sort_order='ascending'),
    dict(name='value', type='string')
]
YT_HOME = '//home'
TABLE_PATH = '/'.join([YT_HOME, 'table'])
QSESSION_TABLE = '/'.join([YT_HOME, 'qsession'])
RAW_UPLOAD_STATUS_TABLE = '/'.join([YT_HOME, 'raw_upload_status'])
MONITORING_TABLE = '/'.join([YT_HOME, 'monitoring'])


def setup_module(module):
    module.YT_SERVER = YtStuff(YtConfig(wait_tablet_cell_initialization=True))
    module.YT_SERVER.start_local_yt()


def teardown_module(module):
    if module.YT_SERVER:
        module.YT_SERVER.stop_local_yt()


def clear_home():
    yt = YT_SERVER.get_yt_client()

    for table in yt.list(YT_HOME):
        yt.remove('/'.join([YT_HOME, table]), recursive=True, force=True)


class TestYtFastDynTable(unittest.TestCase):
    def setUp(self):
        unittest.TestCase.setUp(self)
        self.now = datetime.utcnow()

    def tearDown(self):
        unittest.TestCase.tearDown(self)
        clear_home()

    def test_table_creator(self):
        yt = YT_SERVER.get_yt_client()

        table1 = YtFastDynTable(TABLE_PATH, SIMPLE_SCHEMA, 1000000, 500000, client=yt)
        assert(yt.exists(TABLE_PATH))
        assert(table1._check())

        table2 = YtFastDynTable(TABLE_PATH, SIMPLE_SCHEMA, 1000000, 500000, client=yt)
        assert(yt.exists(TABLE_PATH))
        assert(table1._check())
        assert(table2._check())

    def test_put(self):
        yt = YT_SERVER.get_yt_client()

        table = YtFastDynTable(TABLE_PATH, SIMPLE_SCHEMA, 1000000, 500000, client=yt)
        puts = [dict(key=1, value='DATA_1'), dict(key=2, value='DATA_2')]
        for put in puts:
            table.put(put)
        dyt.flush_dynamic_table(yt, TABLE_PATH)
        assert(sorted(list(yt.read_table(TABLE_PATH)), key=lambda x: x['key']) == sorted(puts, key=lambda x: x['key']))

    def test_alter_table(self):
        yt = YT_SERVER.get_yt_client()

        table = YtFastDynTable(TABLE_PATH, SIMPLE_SCHEMA, 1000000, 500000, client=yt)
        puts = [dict(key=1, value='DATA_1'), dict(key=2, value='DATA_2')]
        for put in puts:
            table.put(put)
        dyt.flush_dynamic_table(yt, TABLE_PATH)
        assert(sorted(list(yt.read_table(TABLE_PATH)), key=lambda x: x['key']) == sorted(puts, key=lambda x: x['key']))

        new_schema = [
            dict(name='key', type='uint32', sort_order='ascending'),
            dict(name='value', type='string'),
            dict(name='value2', type='string')
        ]
        table = YtFastDynTable(TABLE_PATH, new_schema, 1000000, 500000, client=yt)
        puts = [dict(key=1, value='DATA_1', value2='1'), dict(key=2, value='DATA_2', value2='2')]
        for put in puts:
            table.put(put)
        dyt.flush_dynamic_table(yt, TABLE_PATH)
        assert(sorted(list(yt.read_table(TABLE_PATH)), key=lambda x: x['key']) == sorted(puts, key=lambda x: x['key']))


class TestMDSMonitorings(unittest.TestCase):
    def setUp(self):
        unittest.TestCase.setUp(self)
        self.mds_upload_error_red_threshold = 20
        self.mds_upload_error_yellow_threshold = 1
        self.now = datetime.utcnow()
        self.yt = YT_SERVER.get_yt_client()
        self.config = ConfigParser()
        self.config.add_section('yt')
        self.config.add_section('raw')
        self.config.add_section('general')
        self.config.set('yt', 'proxy', YT_SERVER.get_server())
        self.config.set('yt', 'qsession_table', QSESSION_TABLE)
        self.config.set('yt', 'monitoring_table', MONITORING_TABLE)
        self.config.set('raw', 'status_table', RAW_UPLOAD_STATUS_TABLE)
        self.config.set('general', 'monitoring_mds_upload_error_red_threshold', str(self.mds_upload_error_red_threshold))

    def tearDown(self):
        unittest.TestCase.tearDown(self)

    def test_setup(self):
        setup_from_config(self.config)
        assert(self.yt.exists(MONITORING_TABLE))

    def _upload_monitorings(self, errors, delta=0):
        feed_id = 1234
        for counter in range(self.mds_upload_error_red_threshold):
            feed_row = dict(
                feed_id=str(feed_id + counter),
                mbi_params=None,
                fulfillment=None,
                raw_data_write_time=None,
                refresh=0,
                processed_time=self.now,
                last_304_time=None
            )
            feed = FeedData.FromHBaseRow(feed_row, self.now, 'No-Such-Dir')
            feed.finish_time = self.now - timedelta(minutes=delta)
            if counter < errors:
                feed.metadata.mds_upload_errors_count = 1
            else:
                feed.metadata.mds_upload_errors_count = 0
            update_monitoring_for_feed(feed, self.config)
        dyt.flush_dynamic_table(self.yt, MONITORING_TABLE)

    def test_last_304_time(self):
        feed = FeedData()
        feed.feed_id = "1069"
        feed.is_304 = True

        update_monitoring_for_feed(feed, self.config)
        dyt.flush_dynamic_table(self.yt, MONITORING_TABLE)

        yt = YT_SERVER.get_yt_client()
        row = next(yt.select_rows("last_304_time from [{table}] where feed_id=1069".format(table=MONITORING_TABLE)))

        assert(time.gmtime(row['last_304_time']).tm_year == time.gmtime(time.time()).tm_year)


class TestYtService(unittest.TestCase):
    def setUp(self):
        unittest.TestCase.setUp(self)
        self.now = datetime.utcnow()
        feed_row = dict(
            feed_id='1234',
            mbi_params=None,
            fulfillment=None,
            raw_data_write_time=None,
            refresh=0,
            processed_time=self.now,
            last_304_time=None
        )
        self.feed = FeedData.FromHBaseRow(feed_row, self.now, 'No-Such-Dir')
        self.yt = YT_SERVER.get_yt_client()
        self.config = ConfigParser()
        self.config.add_section('yt')
        self.config.add_section('raw')
        self.config.set('yt', 'proxy', YT_SERVER.get_server())
        self.config.set('yt', 'qsession_table', QSESSION_TABLE)
        self.config.set('raw', 'status_table', RAW_UPLOAD_STATUS_TABLE)
        self.config.set('yt', 'monitoring_table', MONITORING_TABLE)

    def tearDown(self):
        unittest.TestCase.tearDown(self)

    def test_setup(self):
        setup_from_config(self.config)
        assert (self.yt.exists(QSESSION_TABLE))
        assert (self.yt.exists(RAW_UPLOAD_STATUS_TABLE))

    def test_update_qsession(self):
        update_qsession(self.feed, self.config)
        assert (self.yt.exists(QSESSION_TABLE))
        dyt.flush_dynamic_table(self.yt, QSESSION_TABLE)
        row = next(self.yt.lookup_rows(QSESSION_TABLE, [{'feed_id': int(self.feed.feed_id)}]))
        assert(row['feed_id'] == int(self.feed.feed_id))
        assert(row['session_id'] == int(self.feed.unixepoch_sessionid))
        assert(row['meta'] == str(self.feed.metadata.SerializeToString()))

    def test_raw_upload_status(self):
        update_raw_upload_touched_time(self.feed, 1, self.config)
        update_raw_upload_uploaded_time(self.feed, 2, self.config)
        dyt.flush_dynamic_table(self.yt, RAW_UPLOAD_STATUS_TABLE)
        assert((1, 2) == get_raw_upload_timestamps(self.feed, self.config))

    def _update_monitoring(self):
        update_monitoring_for_feed(self.feed, self.config)
        dyt.flush_dynamic_table(self.yt, MONITORING_TABLE)
        return next(self.yt.lookup_rows(MONITORING_TABLE, [{'feed_id': int(self.feed.feed_id), 'hostname': socket.gethostname()}]))

    def test_monitoring(self):
        def update_and_check_err(retcode, err_count):
            self.feed.metadata.download_retcode = retcode
            row = self._update_monitoring()
            excpected = dict(
                feed_id=int(self.feed.feed_id),
                download_errors_count=err_count
            )
            self.assertLessEqual(excpected['feed_id'], row['feed_id'])
            self.assertLessEqual(excpected['download_errors_count'], row['download_errors_count'])

        for counter in range(1, 3):
            update_and_check_err(10, counter)

        self.feed.metadata.download_retcode = 0
        self.feed.finish_time = self.now
        self.feed.metadata.parser_retcode = 1
        row = self._update_monitoring()
        excpected = dict(
            feed_id=int(self.feed.feed_id),
            download_errors_count=0,
            finish_time=int((self.feed.finish_time -
                             datetime.utcfromtimestamp(0)).total_seconds()),
            feedparser_retcode=self.feed.metadata.parser_retcode
        )
        self.assertLessEqual(excpected['feed_id'], row['feed_id'])
        self.assertLessEqual(excpected['download_errors_count'], row['download_errors_count'])
        self.assertLessEqual(excpected['finish_time'], row['finish_time'])
        self.assertLessEqual(excpected['feedparser_retcode'], row['feedparser_retcode'])

    def test_mds_upload_errors_monitoring(self):
        """test correct aggregation of field mds_upload_errors_count"""
        def update_mds_upload_errors_count():
            self.feed.metadata.mds_upload_errors_count = 1
            row = self._update_monitoring()
            return row['mds_upload_errors_count']

        old_mds_upload_errors_count = update_mds_upload_errors_count()
        new_mds_upload_errors_count = update_mds_upload_errors_count()
        assert(new_mds_upload_errors_count - old_mds_upload_errors_count == 1)

    def test_monitoring_mds_upload_errors_refresh(self):
        """test that mds_upload_errors_count monitoring is refreshed correctly
        during refresh other monitorings aren't changed"""
        # fill with data
        self.feed.metadata.download_retcode = 10
        self.feed.finish_time = self.now
        self.feed.metadata.mds_upload_errors_count = 1
        self._update_monitoring()

        # mds_upload_errors_count should be refreshed
        self.feed.metadata.mds_upload_errors_count = 0
        row = self._update_monitoring()
        assert(row['download_errors_count'] > 0)
        self.assertEqual(row['mds_upload_errors_count'], 0)

    def test_monitoring_download_errors_refresh(self):
        """test that download_errors_count monitoring is refreshed correctly
        during refresh other monitorings aren't changed"""
        # fill with data
        self.feed.metadata.download_retcode = 10
        self.feed.finish_time = self.now
        self.feed.metadata.mds_upload_errors_count = 1
        self._update_monitoring()

        # download_errors_count should be refreshed
        self.feed.metadata.download_retcode = 0
        row = self._update_monitoring()
        self.assertEqual(row['download_errors_count'], 0)
        assert(row['mds_upload_errors_count'] > 0)


if '__main__' == __name__:
    unittest.main()
