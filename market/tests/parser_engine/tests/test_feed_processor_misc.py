# -*- coding: utf-8 -*-

import base64
import logging
import mock
import os
import shutil
import six
import unittest
import zlib

from six.moves.configparser import (
    RawConfigParser,
    NoOptionError,
    NoSectionError,
)
from datetime import (
    datetime,
    timedelta,
)

from market.idx.pylibrary.downloader.downloader.arcacher import (
    ArcacherFeedData,
    ArcacherNoFeedModificationError,
    CacheType,
    MetaInfo,
    RobotArcacher,
)
from market.idx.datacamp.parser.lib.parser_engine.feed import FeedData
from market.idx.datacamp.parser.lib.parser_engine import feed_processor as fp
from market.idx.datacamp.proto.api.UpdateTask_pb2 import FeedParsingTask
from market.pylibrary.mindexerlib import util
from market.pylibrary.s3.yatf.utils.s3_client import create_s3_test_client
from market.pylibrary.s3.s3.stub.s3_bucket_emulation import (
    StubConfig,
    StubS3Client,
    S3BucketEmulation,
)
from market.pylibrary.s3.s3.s3_api import clean_bucket

import yatest.common


def uc_bin_path():
    return yatest.common.binary_path('tools/uc/uc')


class ConfigParserStub(RawConfigParser):
    """ ConfigParser with safe 'get' method: if 'section : option' is not found
        adds 'section : 0' to itself and goes on.
        '0' is default because it works for getboolean(), getint() and get() methods.
        Pass predefined set of options (as dict) if you don't want them defaulted.
    """

    def __init__(self, options=None):
        RawConfigParser.__init__(self)

        default_options = {
            'general': {
                'feedparser_bin': 'echo test',
                'use_deliverycalc': 'false'
            },
        }

        if options:
            default_options.update(options)
        for section, opts in list(default_options.items()):
            RawConfigParser.add_section(self, section)
            for k, v in list(opts.items()):
                RawConfigParser.set(self, section, k, v)

    def get(self, section, option, **kwargs):
        default = '0'
        try:
            return RawConfigParser.get(self, section, option)
        except NoSectionError:
            RawConfigParser.add_section(self, section)
            RawConfigParser.set(self, section, option, default)
            return default
        except NoOptionError:
            RawConfigParser.set(self, section, option, default)
            return default


def create_feed_data_temps(feed, create_cached_and_headers=False, headers=None):
    if not os.path.exists(feed.session_dir):
        os.makedirs(feed.session_dir)
    open(os.path.join(feed.session_dir, 'fetched.xml'), 'w').close()
    if create_cached_and_headers:
        open(os.path.join(feed.session_dir, 'cached'), 'w').close()
        with open(os.path.join(feed.session_dir, 'headers.xml'), 'w') as h:
            if headers is not None:
                h.write(headers)


def feed_data_stub(feed_id=1,
                   start_time=None,
                   fulfillment=None,
                   raw_data_write_time=None,
                   create_temps=False,
                   mbi_xml=None,
                   create_cached_and_headers=False,
                   headers=None,
                   contents_hash=None,
                   published_contents_hash=None,
                   feed_url=None,
                   download_date=None):
    download_dir = yatest.common.output_path()
    if start_time is None:
        start_time = datetime.utcnow()
    feed_row = dict(
        feed_id=str(feed_id),
        mbi_params=mbi_xml,
        fulfillment=fulfillment,
        raw_data_write_time=raw_data_write_time,
        refresh=0,
        processed_time=start_time,
        last_304_time=None,
    )
    feed = FeedData.FromHBaseRow(feed_row, start_time, download_dir)

    if contents_hash is not None:
        feed.metadata.contents_hash = six.ensure_binary(contents_hash)

    if published_contents_hash is not None:
        feed.publish_metadata.contents_hash = six.ensure_binary(published_contents_hash)

    if feed_url is not None:
        feed.metadata.feed_url = feed_url

    if download_date is not None:
        feed.metadata.download_date = download_date

    if create_temps:
        create_feed_data_temps(feed=feed, create_cached_and_headers=create_cached_and_headers, headers=headers)

    return feed


def feed_data_fromdatacamp_stub(mbi_info, feed_parsing_task, create_temps=False):
    download_dir = yatest.common.output_path()

    feed = FeedData.FromDatacampData(
        feed_id=5106456,
        real_feed_id=5106456,
        feed_status='published',
        mbi_params=mbi_info,
        download_dir=download_dir,
        feed_parsing_task=feed_parsing_task,
        fulfillments=None
    )

    if create_temps:
        create_feed_data_temps(feed=feed)

    return feed


BUCKET = 'mock-market-indexer-long-store'


class TestArcacher(unittest.TestCase):
    def setUp(self):
        self._session_expiration = 24
        self._now = datetime.utcnow()
        self._feed_filename = None
        self._headers_filename = None
        self._force_redownload = False
        self._feed = None
        self._feed_new_session = None
        self._config = None
        self._config_passive = None
        self._mi_type = "test.indexer.type"

        self.log = logging.getLogger()

        if os.getenv('RUN_REAL_S3CLIENT', 'false').lower() == 'true':
            self.log.info('Run real s3client')
            self.s3_client = create_s3_test_client()
        else:
            self.log.info('Run stub s3client')
            S3StubConfig = StubConfig()
            S3StubConfig.s3_bucket_emulation = S3BucketEmulation()
            self.s3_client = StubS3Client(S3StubConfig, self.log)
        clean_bucket(self.s3_client, BUCKET)

    def tearDown(self):
        clean_bucket(self.s3_client, BUCKET)

    def _fetched_threshold(self, config):
        delta = timedelta(hours=config.getint('session', 'fetch_interval'))
        return util.to_iso(self._now - delta)

    def _get_arcacher_feed_data_from_or_feed(self, feed):
        return ArcacherFeedData(
            feed.feed_id,
            feed.session_name,
            feed.session_dir,
            published_contents_hash=feed.publish_metadata.contents_hash
        )

    def _generate_archacher_for_upload(self, feed_url=None, download_date=None, start_time=None, compress_feed=False,
                                       compression_codec='gz'):
        self._feed = feed_data_stub(
            feed_id=1069,
            create_temps=True,
            create_cached_and_headers=True,
            headers='Status: 200',
            contents_hash='TestHash',
            feed_url=feed_url,
            download_date=download_date,
            start_time=start_time
        )

        arcacher_feed_data = self._get_arcacher_feed_data_from_or_feed(self._feed)
        c = {
            'arcacher': {
                'mode': 'force_upload',
                's3_bucket_name': BUCKET,
                'feed_archive_path_prefix': 'feed-archive',
            }
        }
        if compress_feed:
            c['arcacher']['compress_feed'] = 'true'
        c['arcacher']['compression_codec'] = compression_codec
        self._config = ConfigParserStub(c)

        return RobotArcacher(
            feed=arcacher_feed_data,
            config=self._config,
            s3_client=self.s3_client,
            mi_type=self._mi_type,
            uc_bin_path=uc_bin_path()
        )

    def _generate_archacher_for_download(
            self,
            published_hash_value,
            publish_start_time_delta,
            start_time=None
    ):
        self._feed_new_session = feed_data_stub(
            feed_id=1069,
            create_temps=True,
            published_contents_hash=published_hash_value,
            start_time=start_time
        )
        self._feed_new_session.publish_start_time = util.to_iso(
            self._now - timedelta(hours=publish_start_time_delta)
        )
        self._config_passive = ConfigParserStub({
            'arcacher': {
                'mode': 'force_download',
                's3_bucket_name': BUCKET,
                'feed_archive_path_prefix': 'feed-archive',
            },
            'session': {
                'fetch_interval': self._session_expiration,
            }
        })

        self._feed_filename = os.path.join(self._feed_new_session.session_dir, 'cached')
        self._headers_filename = os.path.join(self._feed_new_session.session_dir, 'headers.xml')
        self._force_redownload = self._feed_new_session.publish_start_time < self._fetched_threshold(self._config_passive)
        arcacher_feed_data = self._get_arcacher_feed_data_from_or_feed(self._feed_new_session)
        return RobotArcacher(
            feed=arcacher_feed_data,
            config=self._config_passive,
            s3_client=self.s3_client,
            uc_bin_path=uc_bin_path()
        )

    def _check_feed_files_downloaded(self, arcacher_passive, cache_type=CacheType.RECENT):
        src_headers = arcacher_passive.get_cached_headers()
        self.assertRegexpMatches(src_headers, 'Status: 200')
        self.assertFalse(os.path.isfile(self._feed_filename))
        self.assertFalse(os.path.isfile(self._headers_filename))
        dst_headers = arcacher_passive.download_headers(
            self._headers_filename,
            self._force_redownload,
            cache_type=cache_type
        )
        arcacher_passive.download_feed(
            self._feed_filename,
            self._force_redownload,
            cache_type=cache_type
        )
        self.assertTrue(os.path.isfile(self._feed_filename))
        self.assertTrue(os.path.isfile(self._headers_filename))
        self.assertEqual(src_headers, dst_headers)

    def _test_upload_download(self, compress_feed=False, compression_codec='gz'):
        """Test checks normal uploading and downloading of feed from s3-mds"""
        arcacher = self._generate_archacher_for_upload(compress_feed=compress_feed,
                                                       compression_codec=compression_codec)
        self.assertRaises(
            Exception,
            arcacher.get_cached_headers
        )
        self.assertTrue(arcacher.active_mode())
        self.assertFalse(arcacher.passive_mode())
        arcacher.upload_to_mds(
            self._feed.metadata.contents_hash,
            self._feed.metadata.download_date,
            self._feed.metadata.feed_url
        )

        shutil.rmtree(self._feed.session_dir)
        arcacher_passive = self._generate_archacher_for_download(
            published_hash_value='PublishedTestHash',
            publish_start_time_delta=1
        )
        self.assertFalse(arcacher_passive.active_mode())
        self.assertTrue(arcacher_passive.passive_mode())

        year_and_month = self._feed.session_name[0:6]
        day_of_month = self._feed.session_name[6:8]
        self.assertRegexpMatches(
            arcacher_passive.get_cached_feed_url(),
            '{h}/{b}/archive/{y}/{d}/{f}/feeds/{s}-{t}-cached'.format(
                h=self.s3_client.get_host(),
                b=BUCKET,
                y=year_and_month,
                d=day_of_month,
                f=self._feed.feed_id,
                s=self._feed.session_name,
                t=self._mi_type
            )
        )
        self.assertEquals(
            arcacher_passive.get_cached_contents_hash(),
            six.ensure_str(base64.b64encode(six.ensure_binary('TestHash')))
        )
        self._check_feed_files_downloaded(arcacher_passive)

    def test_upload_download_uncompressed(self):
        self._test_upload_download()

    def test_upload_download_gz(self):
        self._test_upload_download(compress_feed=True)

    def test_upload_download_zstd_11(self):
        self._test_upload_download(compress_feed=True, compression_codec='zstd_11')

    def test_upload_download_zstd_15(self):
        self._test_upload_download(compress_feed=True, compression_codec='zstd_15')

    def test_download_not_changed_feed(self):
        """Test checks that not changed and not expired feed wasn't downloaded from s3-mds"""
        arcacher = self._generate_archacher_for_upload()
        arcacher.upload_to_mds(
            self._feed.metadata.contents_hash,
            self._feed.metadata.download_date,
            self._feed.metadata.feed_url
        )

        shutil.rmtree(self._feed.session_dir)
        arcacher_passive = self._generate_archacher_for_download(
            published_hash_value='TestHash',
            publish_start_time_delta=1
        )
        self.assertRaises(
            ArcacherNoFeedModificationError,
            arcacher_passive.download_headers,
            self._headers_filename,
            self._force_redownload
        )

    def test_download_not_changed_expired_feed(self):
        """Test checks that not changed but expired feed was downloaded from s3-mds"""
        arcacher = self._generate_archacher_for_upload()
        arcacher.upload_to_mds(
            self._feed.metadata.contents_hash,
            self._feed.metadata.download_date,
            self._feed.metadata.feed_url
        )

        shutil.rmtree(self._feed.session_dir)
        arcacher_passive = self._generate_archacher_for_download(
            published_hash_value='TestHash',
            publish_start_time_delta=self._session_expiration + 1
        )
        self._check_feed_files_downloaded(arcacher_passive)

    def test_download_samovar_feeds(self):
        def mds_downloader_side_effect(url, output_filename, *args, **kwargs):
            with open(output_filename, 'wb') as out:
                out.write(zlib.compress(six.ensure_binary('nothing\n')))

        feed = feed_data_stub()
        create_feed_data_temps(feed)
        feed_parsing_task = FeedParsingTask(
            part_urls=[
                'http://samovar1',
                'http://samovar2'
            ]
        )
        feed.feed_parsing_task = feed_parsing_task
        with mock.patch('market.pylibrary.mds_downloader.download_file', side_effect=mds_downloader_side_effect) as mock_mds_downloader:
            fp.download_chunked_feeds_from_mds(feed)
            self.assertEqual(0, feed.metadata.download_retcode)
            mock_mds_downloader.assert_any_call('http://samovar1', mock.ANY, verify_etag=False)
            mock_mds_downloader.assert_any_call('http://samovar2', mock.ANY, verify_etag=False)

    def test_metadata_passing(self):
        arcacher = self._generate_archacher_for_upload(
            feed_url='https://svn.yandex.ru:443/market/market/trunk/testshops/testdontdelete.xml',
            download_date='2018-10-11T13:01:23'
        )

        arcacher.upload_to_mds(
            self._feed.metadata.contents_hash,
            self._feed.metadata.download_date,
            self._feed.metadata.feed_url
        )

        shutil.rmtree(self._feed.session_dir)
        arcacher_passive = self._generate_archacher_for_download(
            published_hash_value='TestHash',
            publish_start_time_delta=self._session_expiration + 1
        )
        meta = arcacher_passive.load_saved_meta_by_type(CacheType.RECENT)
        self.assertEquals(meta.session_name, self._feed.session_name)
        self.assertEquals(meta.feed_original_url, self._feed.metadata.feed_url)
        self.assertEquals(meta.feed_download_date, self._feed.metadata.download_date)

    def test_unicode_feed(self):
        arcacher = self._generate_archacher_for_upload(
            feed_url=six.ensure_text('http://магазин.рф/yandex.xml'),
            download_date='2018-10-11T13:01:23'
        )

        arcacher.upload_to_mds(
            self._feed.metadata.contents_hash,
            self._feed.metadata.download_date,
            self._feed.metadata.feed_url
        )

        shutil.rmtree(self._feed.session_dir)
        arcacher_passive = self._generate_archacher_for_download(
            published_hash_value='TestHash',
            publish_start_time_delta=self._session_expiration + 1
        )
        self._check_feed_files_downloaded(arcacher_passive)

    def test_upload_completed_meta(self):
        arcacher = self._generate_archacher_for_upload(
            feed_url=six.ensure_text('http://магазин.рф/yandex.xml'),
            download_date='2018-10-11T13:01:23'
        )

        arcacher.upload_to_mds(
            self._feed.metadata.contents_hash,
            self._feed.metadata.download_date,
            self._feed.metadata.feed_url
        )
        arcacher.upload_completed_meta(
            self._feed.metadata.contents_hash,
            self._feed.metadata.download_date,
            self._feed.metadata.feed_url
        )
        shutil.rmtree(self._feed.session_dir)

        arcacher_passive = self._generate_archacher_for_download(
            published_hash_value='TestHash',
            publish_start_time_delta=self._session_expiration + 1
        )
        self._check_feed_files_downloaded(arcacher_passive, CacheType.COMPLETED)

    def test_recent_feed_updated__old_feed_deleted(self):
        cur_dt = datetime.utcnow()
        first_session_st = cur_dt.replace(hour=10)
        second_session_st = cur_dt.replace(hour=15)
        arcacher = self._generate_archacher_for_upload(
            feed_url=six.ensure_text('http://магазин.рф/yandex.xml'),
            start_time=first_session_st
        )

        arcacher.upload_to_mds(
            self._feed.metadata.contents_hash,
            self._feed.metadata.download_date,
            self._feed.metadata.feed_url
        )
        shutil.rmtree(self._feed.session_dir)
        new_arcacher = self._generate_archacher_for_upload(
            feed_url=six.ensure_text('http://магазин.рф/yandex.xml'),
            start_time=second_session_st
        )
        new_arcacher.upload_to_mds(
            self._feed.metadata.contents_hash,
            self._feed.metadata.download_date,
            self._feed.metadata.feed_url
        )
        shutil.rmtree(new_arcacher._feed.session_dir)

        year_and_month = new_arcacher._feed.session_name[0:6]
        day_of_month = new_arcacher._feed.session_name[6:8]
        feed_id = new_arcacher._feed.feed_id
        saved_feeds = self.s3_client.list(
            BUCKET,
            "archive/{}/{}/{}/feeds/".format(year_and_month, day_of_month, feed_id)
        )
        assert len(saved_feeds) == 1
        assert os.path.basename(saved_feeds[0]).startswith(new_arcacher._feed.session_name)

        arcacher_passive = self._generate_archacher_for_download(
            published_hash_value='TestHash',
            publish_start_time_delta=self._session_expiration + 1
        )
        meta = arcacher_passive.load_saved_meta_by_type(CacheType.RECENT)
        self.assertEquals(meta.cached, saved_feeds[0])

    def test_same_day_completed_feed_updated__old_feed_deleted(self):
        cur_dt = datetime.utcnow()
        first_session_st = cur_dt.replace(hour=10)
        second_session_st = cur_dt.replace(hour=15)
        arcacher = self._generate_archacher_for_upload(
            feed_url=six.ensure_text('http://магазин.рф/yandex.xml'),
            start_time=first_session_st
        )

        arcacher.upload_to_mds(
            self._feed.metadata.contents_hash,
            self._feed.metadata.download_date,
            self._feed.metadata.feed_url
        )
        arcacher.upload_completed_meta(
            self._feed.metadata.contents_hash,
            self._feed.metadata.download_date,
            self._feed.metadata.feed_url
        )
        shutil.rmtree(self._feed.session_dir)
        new_arcacher = self._generate_archacher_for_upload(
            feed_url=six.ensure_text('http://магазин.рф/yandex.xml'),
            start_time=second_session_st
        )
        new_arcacher.upload_to_mds(
            self._feed.metadata.contents_hash,
            self._feed.metadata.download_date,
            self._feed.metadata.feed_url
        )
        new_arcacher.upload_completed_meta(
            self._feed.metadata.contents_hash,
            self._feed.metadata.download_date,
            self._feed.metadata.feed_url
        )
        shutil.rmtree(new_arcacher._feed.session_dir)

        year_and_month = new_arcacher._feed.session_name[0:6]
        day_of_month = new_arcacher._feed.session_name[6:8]
        feed_id = new_arcacher._feed.feed_id
        saved_feeds = self.s3_client.list(
            BUCKET,
            "archive/{}/{}/{}/feeds/".format(year_and_month, day_of_month, feed_id)
        )
        assert len(saved_feeds) == 1
        assert os.path.basename(saved_feeds[0]).startswith(new_arcacher._feed.session_name)

        arcacher_passive = self._generate_archacher_for_download(
            published_hash_value='TestHash',
            publish_start_time_delta=self._session_expiration + 1
        )
        meta = arcacher_passive.load_saved_meta_by_type(CacheType.COMPLETED)
        self.assertEquals(meta.cached, saved_feeds[0])

    def test_different_day_completed_feed_updated__old_feed_not_deleted(self):
        cur_dt = datetime.utcnow()
        first_session_st = cur_dt.replace(day=10)
        second_session_st = cur_dt.replace(day=11)
        arcacher = self._generate_archacher_for_upload(
            feed_url=six.ensure_text('http://магазин.рф/yandex.xml'),
            start_time=first_session_st
        )

        arcacher.upload_to_mds(
            self._feed.metadata.contents_hash,
            self._feed.metadata.download_date,
            self._feed.metadata.feed_url
        )
        arcacher.upload_completed_meta(
            self._feed.metadata.contents_hash,
            self._feed.metadata.download_date,
            self._feed.metadata.feed_url
        )
        shutil.rmtree(self._feed.session_dir)
        new_arcacher = self._generate_archacher_for_upload(
            feed_url=six.ensure_text('http://магазин.рф/yandex.xml'),
            start_time=second_session_st
        )
        new_arcacher.upload_to_mds(
            self._feed.metadata.contents_hash,
            self._feed.metadata.download_date,
            self._feed.metadata.feed_url
        )
        new_arcacher.upload_completed_meta(
            self._feed.metadata.contents_hash,
            self._feed.metadata.download_date,
            self._feed.metadata.feed_url
        )
        shutil.rmtree(new_arcacher._feed.session_dir)

        year_and_month = new_arcacher._feed.session_name[0:6]
        day_of_month = new_arcacher._feed.session_name[6:8]
        feed_id = new_arcacher._feed.feed_id
        saved_feeds = self.s3_client.list(
            bucket=BUCKET,
            path="archive/{}/{}/{}/feeds/".format(year_and_month, day_of_month, feed_id),
        )
        assert len(saved_feeds) == 1
        assert os.path.basename(saved_feeds[0]).startswith(new_arcacher._feed.session_name)

        arcacher_passive = self._generate_archacher_for_download(
            published_hash_value='TestHash',
            publish_start_time_delta=self._session_expiration + 1
        )
        meta = arcacher_passive.load_saved_meta_by_type(CacheType.COMPLETED)
        self.assertEquals(meta.cached, saved_feeds[0])

        year_and_month = arcacher._feed.session_name[0:6]
        day_of_month = arcacher._feed.session_name[6:8]
        feed_id = arcacher._feed.feed_id
        saved_feeds = self.s3_client.list(
            bucket=BUCKET,
            path="archive/{}/{}/{}/feeds/".format(year_and_month, day_of_month, feed_id),
        )
        assert len(saved_feeds) == 1
        assert os.path.basename(saved_feeds[0]).startswith(arcacher._feed.session_name)

    def test_new_month_download__old_month_meta_downloaded(self):
        first_session_st = datetime(year=2018, month=12, day=20)
        second_session_st = datetime(year=2019, month=1, day=5)

        arcacher = self._generate_archacher_for_upload(
            feed_url=six.ensure_text('http://магазин.рф/yandex.xml'),
            start_time=first_session_st
        )

        arcacher.upload_to_mds(
            self._feed.metadata.contents_hash,
            self._feed.metadata.download_date,
            self._feed.metadata.feed_url
        )
        arcacher.upload_completed_meta(
            self._feed.metadata.contents_hash,
            self._feed.metadata.download_date,
            self._feed.metadata.feed_url
        )
        shutil.rmtree(self._feed.session_dir)

        arcacher_passive = self._generate_archacher_for_download(
            published_hash_value='TestHash',
            publish_start_time_delta=self._session_expiration + 1,
            start_time=second_session_st
        )

        with mock.patch('market.idx.pylibrary.downloader.downloader.arcacher.datetime') as mock_date:
            mock_date.utcnow.return_value = second_session_st
            self._check_feed_files_downloaded(arcacher_passive)

    def test_save_meta_unicode(self):
        """Тест проверяет, что если в урле есть не ascii символы,
           то Meta корректно сериализуется и десериализуется
        """
        test_path = "test_save_meta_unicode"

        meta = MetaInfo(
            session_name='20181218_1234',
            feed_original_url=six.ensure_text('http://нам_нужно_больше_юникода.нет/feed.yml')
        )
        self.s3_client.write(bucket=BUCKET, path=test_path, content=str(meta))
        content = self.s3_client.read(bucket=BUCKET, path=test_path)
        meta_read = MetaInfo.from_str(content)

        self.assertEqual(str(meta), str(meta_read))

if __name__ == '__main__':
    unittest.main()
