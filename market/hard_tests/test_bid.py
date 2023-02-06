# -*- coding: utf-8 -*-

import os
import time
import unittest

import context
from market.idx.marketindexer.marketindexer import miconfig
from market.idx.pylibrary.mindexer_core.qbid.bids_service import (
    BIDS_SNAPSHOT_FILENAME,
    SlowBidsService,
    unixtime2generation,
)
from market.idx.pylibrary.mindexer_core.qbid.mbidding import MBiddingCredentials, MockCurl


class TestBid(context.HbaseTestCase):

    def setUp(self):
        self.indexer_workdir = os.path.realpath(miconfig.default().working_dir)
        self.wd = os.path.join(self.indexer_workdir, 'qbid', 'slow')
        context.create_workdir_test_environment(self.indexer_workdir)

    def test_bid(self):
        slow_bids = SlowBidsService(curl=MockCurl())
        slow_bids.clear()
        slow_bids.update()

        recent_dir = os.path.join(self.indexer_workdir,
                                  'qbid',
                                  'slow',
                                  'recent')
        self.assertTrue(os.path.exists(recent_dir))
        self.assertTrue(os.path.realpath(recent_dir))
        self.assertTrue(os.path.exists(os.path.join(recent_dir, BIDS_SNAPSHOT_FILENAME)))

    def test_clear_bid(self):
        slow_bids = SlowBidsService(curl=MockCurl())
        now_seconds = time.time()

        latest = os.path.join(self.wd, unixtime2generation(now_seconds - 60))
        os.mkdir(latest)
        os.mkdir(os.path.join(self.wd, unixtime2generation(now_seconds - 120)))
        os.mkdir(os.path.join(self.wd, unixtime2generation(now_seconds - 180)))
        os.mkdir(os.path.join(self.wd, unixtime2generation(now_seconds - 240)))

        slow_bids.clear()
        self.assertFalse(os.path.exists(latest))

        os.mkdir(latest)
        os.mkdir(os.path.join(self.wd, unixtime2generation(now_seconds - 360)))
        os.symlink(latest, os.path.join(self.wd, slow_bids.RECENT))
        slow_bids.clear()

        remained = os.listdir(self.wd)
        self.assertEqual(2, len(remained))
        self.assertTrue('recent' in remained)

        slow_bids.update()
        self.assertTrue(3, len(os.listdir(self.wd)))

    def test_copy_bid(self):
        slow_bids = SlowBidsService(curl=MockCurl())
        slow_bids.update()

        target_dir = os.path.join(self.wd, 'target')
        os.mkdir(target_dir)
        slow_bids.copy_recent(target_dir)

        self.assertTrue(os.path.exists(os.path.join(target_dir, BIDS_SNAPSHOT_FILENAME)))


class TestCredentials(context.HbaseTestCase):

    def setUp(self):
        credfile = os.path.join(context.rootdir, 'mbi.credentials')
        with open(credfile, 'w') as cf:
            cf.write('mbidding.username = marketindexer\n')
            cf.write('mbidding.password = password_of_marketindexer')
        miconfig.default().mbi_credentials = credfile

    def test_credentials(self):
        mbi_credentials = MBiddingCredentials()
        self.assertEqual('marketindexer', mbi_credentials.user())
        self.assertEqual('password_of_marketindexer', mbi_credentials.password())


if '__main__' == __name__:
    unittest.main()
