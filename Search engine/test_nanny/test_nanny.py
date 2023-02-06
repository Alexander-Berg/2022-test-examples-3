# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import unittest
import mock

from requests import ReadTimeout
from requests_mock.exceptions import MockException
from search.martylib.core.decorators import stop_trying
from search.martylib.core.exceptions import MaxRetriesReached

from search.martylib.nanny import parse_instance_tags, NannyClient
from search.martylib.proto.structures import nanny_pb2


FIRST_BEGEMOT_INSTANCE_TAGS = [
    'MAN_BEGEMOT_PRODUCTION_WORKER_GRUNWALD_HAMSTER',
    'a_ctype_hamster',
    'a_dc_man',
    'a_geo_man',
    'a_itype_begemot',
    'a_line_man2',
    'a_metaprj_web',
    'a_prj_grunwald',
    'a_prj_test1',
    'a_tier_none',
    'a_topology_cgset-memory.limit_in_bytes=8694792192',
    'a_topology_cgset-memory.low_limit_in_bytes=8589934592',
    'a_topology_group-MAN_BEGEMOT_PRODUCTION_WORKER_GRUNWALD_HAMSTER',
    'a_topology_stable-112-r109',
    'a_topology_version-stable-112-r109',
    'cgset_memory_recharge_on_pgfault_1',
    'enable_hq_report',
    'enable_hq_poll'
]

SECOND_BEGEMOT_INSTANCE_TAGS = [
    'SAS_BEGEMOT_PRODUCTION_WORKER_GRUNWALD_HAMSTER',
    'a_ctype_hamster',
    'a_dc_sas',
    'a_geo_sas',
    'a_itype_begemot',
    'a_line_sas-1.4.4',
    'a_metaprj_web',
    'a_prj_grunwald',
    'a_prj_test2',
    'a_prj_none',
    'a_tier_none',
    'a_topology_cgset-memory.limit_in_bytes=8694792192',
    'a_topology_cgset-memory.low_limit_in_bytes=8589934592',
    'a_topology_group-SAS_BEGEMOT_PRODUCTION_WORKER_GRUNWALD_HAMSTER',
    'a_topology_stable-112-r109',
    'a_topology_version-stable-112-r109',
    'cgset_memory_recharge_on_pgfault_1',
    'enable_hq_report',
    'enable_hq_poll'
]


class TestInstanceCrawler(unittest.TestCase):
    def test_instance_tags_parsing(self):
        tags = parse_instance_tags(
            FIRST_BEGEMOT_INSTANCE_TAGS,
        )

        self.assertEqual(
            nanny_pb2.InstanceTags(
                itype='begemot',
                ctype='hamster',
                metaprj='web',
                prj=['grunwald', 'test1'],
                itag=[],
                geo=['man']
            ),
            tags,
        )

        parse_instance_tags(
            SECOND_BEGEMOT_INSTANCE_TAGS,
            dest=tags,
        )

        self.assertEqual(
            nanny_pb2.InstanceTags(
                itype='begemot',
                ctype='hamster',
                metaprj='web',
                prj=['grunwald', 'test1', 'test2'],
                itag=[],
                geo=['man', 'sas']
            ),
            tags,
        )

        another_order_tags = nanny_pb2.InstanceTags()

        parse_instance_tags(
            SECOND_BEGEMOT_INSTANCE_TAGS,
            dest=another_order_tags,
        )
        parse_instance_tags(
            FIRST_BEGEMOT_INSTANCE_TAGS,
            dest=another_order_tags,
        )

        self.assertEqual(
            tags,
            another_order_tags
        )


class NannyClientTestCase(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.nanny = NannyClient(auth_required=False, retry_hooks={MockException: stop_trying})
        cls.send_patch = mock.patch.object(
            cls.nanny.session,
            'send',
            side_effect=MockException('session.send mock not configured'),
        )

    def setUp(self):
        self.send = self.send_patch.start()

    def tearDown(self):
        self.send_patch.stop()

    def test_copy_service_read_timeout_no_retries(self):
        self.send.side_effect = ReadTimeout()
        self.assertRaises(
            ReadTimeout,
            self.nanny.copy_service,
            'src',
            'tgt',
        )

    def test_copy_service_exception_retries(self):
        self.send.side_effect = Exception()
        self.assertRaises(
            MaxRetriesReached,
            self.nanny.copy_service,
            'src',
            'tgt',
        )
