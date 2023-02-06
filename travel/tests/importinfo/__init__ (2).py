# -*- coding: utf-8 -*-

import logging
from django.contrib.contenttypes.models import ContentType

from common.models.geo import Settlement
from travel.rasp.admin.importinfo.models import RelatedLog
from travel.rasp.admin.importinfo.related_log import save_log_for_first_arg_object

from travel.rasp.admin.lib.unittests.testcase import TestCase

module_fixtures = [
    'travel.rasp.admin.tester.fixtures.www:countries.yaml',
    'travel.rasp.admin.tester.fixtures.www:regions.yaml',
    'travel.rasp.admin.tester.fixtures.www:settlements.yaml',
    'travel.rasp.admin.tester.fixtures.www:stations.yaml'
]


class ObjectRelatedLogTest(TestCase):
    class_fixtures = module_fixtures

    def testCreateAndGet(self):
        settlement = Settlement.objects.all()[0]

        RelatedLog.objects.filter(content_type=ContentType.objects.get_for_model(settlement.__class__),
                                  object_id=settlement.id).delete()

        related_log = RelatedLog.create_from_object(settlement)

        related_log2 = RelatedLog.get_by_object(settlement)

        self.assertEqual(related_log.emitter, settlement)
        self.assertEqual(related_log, related_log2)

    message = u"Test message"

    def testSaveLogFromFunctionDecotator(self):
        settlement = Settlement.objects.all()[0]

        @save_log_for_first_arg_object
        def test_func(settlement):
            log = logging.getLogger('rasp.test')
            log.info(self.message)

        test_func(settlement)

        related_log = RelatedLog.get_by_object(settlement)

        self.assertTrue(related_log.log.strip().endswith(self.message))

    @save_log_for_first_arg_object(is_method=True)
    def _func(self, settlement):
        log = logging.getLogger('rasp.test')
        log.info(self.message)

    def testSaveLogFromFunctionBoundMethodDecotator(self):
        settlement = Settlement.objects.all()[0]

        self._func(settlement)

        related_log = RelatedLog.get_by_object(settlement)

        self.assertTrue(related_log.log.strip().endswith(self.message))

    def testCreateAndGetWithTags(self):
        tag1 = '1'
        tag2 = '2'
        settlement = Settlement.objects.all()[0]

        related_log2 = RelatedLog.get_by_object(settlement)

        related_log_with_tag1 = RelatedLog.get_by_object(settlement, tag1)

        related_log2_with_tag1 = RelatedLog.get_by_object(settlement, tag1)

        related_log2_with_tag2 = RelatedLog.get_by_object(settlement, tag2)

        self.assertEqual(related_log_with_tag1, related_log2_with_tag1)
        self.assertNotEqual(related_log2, related_log_with_tag1)
        self.assertNotEqual(related_log2_with_tag2, related_log_with_tag1)

    def testSaveLogFromFunctionDecotatorWithTag(self):
        settlement = Settlement.objects.all()[0]

        related_empty_log = RelatedLog.get_by_object(settlement, "test_empty_tag")

        @save_log_for_first_arg_object(tag="test_tag")
        def test_func(settlement):
            log = logging.getLogger('rasp.test')
            log.info(self.message)

        test_func(settlement)

        related_full_log = RelatedLog.get_by_object(settlement, "test_tag")

        self.assertTrue(related_full_log.log.strip().endswith(self.message))
        self.assertEqual(related_empty_log.log, u"")
