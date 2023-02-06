#!/usr/bin/env python
# -*- coding: utf-8 -*-

import unittest

from market.idx.pylibrary.mindexer_core.qbid.yt_storage import (
    BidsSaver,
    BidsSaverError,
    upload_to_cluster,
)
from market.idx.pylibrary.mindexer_core.qbid.yt_cluster import BidsCluster
from market.idx.pylibrary.mindexer_core.qbid.bids_service import BIDS_TYPE_OFFER


class MockClusterException(Exception):
    pass


class MockCluster(object):
    @classmethod
    def make_good_cluster(cls):
        return cls(True)

    @classmethod
    def make_bad_cluster(cls):
        return cls(False)

    def __init__(self, good):
        self.good = good
        self.called_upload = False
        self.proxy = 'mock'
        self.table_dir = 'mock'
        self.token_path = 'mock'


def mock_upload_to_cluster(cluster, *args, **kwargs):
    cluster.called_upload = True
    if not cluster.good:
        raise MockClusterException()


class MockLog(object):
    def __init__(self):
        self.called_warn = False
        self.called_error = False

    def info(self, *args, **kwargs):
        pass

    def warn(self, *args, **kwargs):
        self.called_warn = True

    def error(self, *args, **kwargs):
        self.called_error = True


class StubYtError(Exception):
    """Raised by MockYt when its operations fail.
    """
    pass


class MockYt(object):
    """Mock YT wrapper that supports operations that may or may not fail
    depending on how it's initialized.
    """
    def __init__(self, failures=frozenset()):
        """Initializes with a set of failures: unbound methods
        from the class.
        """
        self.failures = failures
        self.called_create_table = False
        self.called_alter_table = False

    def create(
        self,
        mode,
        table_path,
        recursive=False,
        ignore_existing=False,
        attributes=None,
    ):
        """Mock table creation.
        """
        self.called_create_table = True

        if type(self).create in self.failures:
            raise StubYtError('create_table failed')

    def alter_table(
        self,
        table_path,
        schema=None,
    ):
        """Mock table alteration.
        """
        self.called_alter_table = True

        if type(self).alter_table in self.failures:
            raise StubYtError('create_table failed')


class MockBidsUploader(object):
    RESULT_OK = 0
    RESULT_WARN = 1
    RESULT_ERROR = 2

    def __init__(self, result):
        self.result = result
        self.called_run = False

    def run(self, **kwargs):
        self.called_run = True

        if self.result == self.RESULT_ERROR:
            raise StubYtError('MockBidsUploader.run failed')

        return self.result

    def check_retcode_warn(self, retcode):
        return retcode == self.RESULT_WARN


class TestBidsSaver(unittest.TestCase):
    def test_good(self):
        good_cluster = MockCluster.make_good_cluster()
        bad_cluster = MockCluster.make_bad_cluster()
        saver = self.make_saver(good_cluster, bad_cluster)
        log = MockLog()
        elog = MockLog()

        saver.upload_bids(
            source_file='foo',
            bids_type='foo',
            pub_date=1234,
            mbi_id=12345,
            generation='20170303_0303',
            delta_generation='20170303_030303',
            upload_func=mock_upload_to_cluster,
            log=log,
            event_log=elog,
        )

        self.assertTrue(good_cluster.called_upload)
        self.assertFalse(bad_cluster.called_upload)
        self.assertFalse(log.called_warn)
        self.assertFalse(elog.called_warn)

    def test_warn_cluster(self):
        good_cluster = MockCluster.make_good_cluster()
        bad_cluster = MockCluster.make_bad_cluster()
        saver = self.make_saver(bad_cluster, good_cluster)
        log = MockLog()
        elog = MockLog()

        saver.upload_bids(
            source_file='foo',
            bids_type='foo',
            pub_date=1234,
            mbi_id=12345,
            generation='20170303_0303',
            delta_generation='20170303_030303',
            upload_func=mock_upload_to_cluster,
            log=log,
            event_log=elog,
        )

        self.assertTrue(bad_cluster.called_upload)
        self.assertTrue(good_cluster.called_upload)
        self.assertTrue(log.called_warn)
        self.assertTrue(elog.called_warn)

    def test_bad_clusters(self):
        cluster1 = MockCluster.make_bad_cluster()
        cluster2 = MockCluster.make_bad_cluster()
        saver = self.make_saver(cluster1, cluster2)
        log = MockLog()
        elog = MockLog()

        with self.assertRaises(BidsSaverError):
            saver.upload_bids(
                source_file='foo',
                bids_type='foo',
                pub_date=1234,
                mbi_id=12345,
                generation='20170303_0303',
                delta_generation='20170303_030303',
                upload_func=mock_upload_to_cluster,
                log=log,
                event_log=elog,
            )

        self.assertTrue(cluster1.called_upload)
        self.assertTrue(cluster2.called_upload)
        self.assertTrue(log.called_warn)
        self.assertTrue(elog.called_warn)

    def test_bad_generation(self):
        cluster = MockCluster.make_good_cluster()
        saver = self.make_saver(cluster)
        log = MockLog()
        elog = MockLog()

        with self.assertRaises(ValueError):
            saver.upload_bids(
                source_file='foo',
                bids_type='foo',
                pub_date=1234,
                mbi_id=12345,
                generation='20170303',
                delta_generation='20170303_030303',
                upload_func=mock_upload_to_cluster,
                log=log,
                event_log=elog,
            )

    def make_saver(self, *clusters):
        uploader = MockBidsUploader(MockBidsUploader.RESULT_OK)
        saver = BidsSaver(uploader)
        for cluster in clusters:
            saver.add_cluster(cluster)
        return saver


class TestUploadToCluster(unittest.TestCase):
    """Checks how cluster uploader behaves when various stages fail
    by providing it with mock YT wrapper and checking that it flags
    partial failure with warnings and total failure with exceptions.
    """

    def test_success(self):
        log = MockLog()
        elog = MockLog()
        uploader = MockBidsUploader(MockBidsUploader.RESULT_OK)
        mock_yt = MockYt()
        cluster = BidsCluster(
            table_dir='bar',
            proxy='baz',
        )

        upload_to_cluster(
            cluster=cluster,
            uploader=uploader,
            source_file='foo.pbuf.sn',
            bids_type=BIDS_TYPE_OFFER,
            pub_date=1,
            mbi_id=1,
            generation='20170405_1927',
            delta_generation=None,
            yt=mock_yt,
            log=log,
            event_log=elog,
        )

        self.assertTrue(uploader.called_run)
        self.assertTrue(mock_yt.called_create_table)
        self.assertTrue(mock_yt.called_alter_table)

    def test_uploader_warn(self):
        log = MockLog()
        elog = MockLog()
        uploader = MockBidsUploader(MockBidsUploader.RESULT_WARN)
        mock_yt = MockYt()
        cluster = BidsCluster(
            table_dir='bar',
            proxy='baz',
        )

        upload_to_cluster(
            cluster=cluster,
            uploader=uploader,
            source_file='foo.pbuf.sn',
            bids_type=BIDS_TYPE_OFFER,
            pub_date=1,
            mbi_id=1,
            generation='20170405_1927',
            delta_generation=None,
            yt=mock_yt,
            log=log,
            event_log=elog,
        )

        self.assertTrue(log.called_warn)
        self.assertTrue(elog.called_warn)
        self.assertTrue(uploader.called_run)
        self.assertTrue(mock_yt.called_create_table)
        self.assertTrue(mock_yt.called_alter_table)

    def test_uploader_fail(self):
        log = MockLog()
        elog = MockLog()
        uploader = MockBidsUploader(MockBidsUploader.RESULT_ERROR)
        mock_yt = MockYt()
        cluster = BidsCluster(
            table_dir='bar',
            proxy='baz',
        )

        with self.assertRaises(StubYtError):
            upload_to_cluster(
                cluster=cluster,
                uploader=uploader,
                source_file='foo.pbuf.sn',
                bids_type=BIDS_TYPE_OFFER,
                pub_date=1,
                mbi_id=1,
                generation='20170405_1927',
                delta_generation=None,
                yt=mock_yt,
                log=log,
                event_log=elog,
            )

        self.assertTrue(uploader.called_run)
        self.assertTrue(mock_yt.called_create_table)
        self.assertTrue(mock_yt.called_alter_table)

    def test_table_creation_fail(self):
        log = MockLog()
        elog = MockLog()
        uploader = MockBidsUploader(MockBidsUploader.RESULT_OK)
        mock_yt = MockYt({
            MockYt.create
        })
        cluster = BidsCluster(
            table_dir='bar',
            proxy='baz',
        )

        with self.assertRaises(StubYtError):
            upload_to_cluster(
                cluster=cluster,
                uploader=uploader,
                source_file='foo.pbuf.sn',
                bids_type=BIDS_TYPE_OFFER,
                pub_date=1,
                mbi_id=1,
                generation='20170405_1927',
                delta_generation=None,
                yt=mock_yt,
                log=log,
                event_log=elog,
            )

        self.assertFalse(log.called_warn)
        self.assertFalse(elog.called_warn)
        self.assertFalse(uploader.called_run)
        self.assertTrue(mock_yt.called_create_table)
        self.assertFalse(mock_yt.called_alter_table)

    def test_schema_change_fail(self):
        log = MockLog()
        elog = MockLog()
        uploader = MockBidsUploader(MockBidsUploader.RESULT_OK)
        mock_yt = MockYt({
            MockYt.alter_table,
        })
        cluster = BidsCluster(
            table_dir='bar',
            proxy='baz',
        )

        with self.assertRaises(StubYtError):
            upload_to_cluster(
                cluster=cluster,
                uploader=uploader,
                source_file='foo.pbuf.sn',
                bids_type=BIDS_TYPE_OFFER,
                pub_date=1,
                mbi_id=1,
                generation='20170405_1927',
                delta_generation=None,
                yt=mock_yt,
                log=log,
                event_log=elog,
            )

        self.assertFalse(uploader.called_run)
        self.assertTrue(mock_yt.called_create_table)
        self.assertTrue(mock_yt.called_alter_table)


if __name__ == '__main__':
    unittest.main()
