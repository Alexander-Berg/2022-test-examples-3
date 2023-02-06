# -*- coding: utf-8 -*-

import unittest
import os
import stat
import subprocess
import tempfile

from mock import patch
from parameterized import parameterized, param

from market.idx.pylibrary.downloader.downloader.download import (
    ARGS,
    create_downloader,
    DownloadError,
    ZoraClientError,
)


class TestZoraDownloader(unittest.TestCase):

    def setUp(self):
        self._fake_zora_client = tempfile.NamedTemporaryFile()
        os.chmod(
            self._fake_zora_client.name,
            os.stat(self._fake_zora_client.name).st_mode | stat.S_IEXEC
        )
        self._downloader = create_downloader(True, self._fake_zora_client.name)

    @parameterized.expand([
        param(
            effect=DownloadError('yes, we can'),
            retry_count=5,
            expected_count=6,
            expected_exception=False
        ),
        param(
            effect=ZoraClientError('no, i was wrong'),
            retry_count=5,
            expected_count=6,
            expected_exception=False
        ),
        param(
            effect=subprocess.TimeoutExpired('who is care', 'what', 'in this params'),
            retry_count=5,
            expected_count=6,
            expected_exception=False
        ),
        param(
            effect=DownloadError('I am so sorry'),
            retry_count=0,
            expected_count=1,
            expected_exception=False
        ),
        param(
            effect=ZoraClientError('but this evening'),
            retry_count=0,
            expected_count=1,
            expected_exception=False
        ),
        param(
            effect=subprocess.TimeoutExpired('at 21:58', 'I skip', 'my train'),
            retry_count=0,
            expected_count=1,
            expected_exception=False
        ),
        param(
            effect=RuntimeError('FUUUuu'),
            retry_count=3,
            expected_count=1,
            expected_exception=True
        ),
    ])
    def test_zora_retry_count(self, effect, retry_count, expected_count, expected_exception):
        url = 'http://ya.ru/'
        args = ARGS(
            url=url,
            destination='',
            header_answer_destination='',
            retry_count=retry_count
        )
        with patch(
            'market.idx.pylibrary.downloader.downloader.download.ZoraDownloader._do_download',
            autospec=True,
            side_effect=effect
        ) as do_download_mock:
            if expected_exception:
                with self.assertRaises(type(effect)):
                    self._downloader.download(args)
            else:
                self._downloader.download(args)
            self.assertEqual(expected_count, do_download_mock.call_count)
