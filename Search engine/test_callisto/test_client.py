# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from infra.callisto.protos.multibeta.slot_state_pb2 import InstanceInfo, Response

from search.martylib.test_utils import TestCase
from search.martylib.callisto import CallistoClient, CallistoClientMock
from search.martylib.callisto.exceptions import UnsupportedCallistoSlotId


class TestCallistoClient(TestCase):
    @classmethod
    def setUpClass(cls):
        super(TestCallistoClient, cls).setUpClass()
        cls.client = CallistoClient()
        cls.mock = CallistoClientMock()

    def test_get_url_prefix(self):
        with self.assertRaisesRegexp(UnsupportedCallistoSlotId, r'foo-bar-baz'):
            self.client.get_url_prefix('foo-bar-baz')

        with self.assertRaisesRegexp(UnsupportedCallistoSlotId, r'foo-bar-baz'):
            self.mock.get_url_prefix('foo-bar-baz')

        self.assertEqual(self.client.get_url_prefix('multibeta2'), '/web/multi_beta/multi_beta')
        self.assertEqual(self.mock.get_url_prefix('video_multibeta3'), '/video/multi_beta/video_multi_beta')

    def test_mock(self):
        test_response = Response(
            status=Response.WAIT_FOR_QUORUM,
            instances=(
                InstanceInfo(hostname='ocelot.search.yandex.net', port=9080),
            ),
        )
        self.mock.data['multibeta2', '0'] = test_response

        self.assertEqual(self.mock.get_slot_state(slot_id='multibeta2', revision='0'), test_response)
