# -*- coding: utf-8 -*-

import logging
import unittest
from datetime import datetime
from functools import wraps

from travel.avia.admin.lib.exceptions import SimpleUnicodeException
from travel.avia.admin.lib.logs import get_collector, remove_collector
from travel.avia.admin.lib.mask_description import make_dates_from_mask_description, get_description_from_mask, get_description_from_mask_in_range
from travel.avia.library.python.common.utils.date import RunMask


class UnicodeAssertionError(SimpleUnicodeException, AssertionError):
    pass


unittest.TestCase.failureException = UnicodeAssertionError


class MaskComparisonMixIn(unittest.TestCase):
    def assertMaskEqualDescription(self, mask, mask_description, msg=None):
        try:
            self.assertListEqual(mask.dates(), make_dates_from_mask_description(mask_description), msg)
        except self.failureException:
            self.fail(self.get_assert_mask_equal_description_fail_msg(mask, mask_description))

    def get_assert_mask_equal_description_fail_msg(self, mask, mask_description, mask_range=None):
        if mask_range:
            template = u"Маска в диапазоне %s %s\n%%s\n -- не совпадает с описанием -- \n%%s\n" % tuple(mask_range)
        else:
            template = u"Маска \n%s\n -- не совпадает с описанием -- \n%s\n"

        def verbose(m):
            if mask_range:
                return get_description_from_mask_in_range(m, mask_range)
            else:
                return get_description_from_mask(m)

        dates = make_dates_from_mask_description(mask_description)
        expected_mask = RunMask(days=dates, today=dates[0])

        return template % (verbose(mask), verbose(expected_mask))

    def assertMaskEqualDescriptionInRange(self, mask, mask_description, mask_range, msg=None):
        start, end = mask_range

        mask_dates = filter(lambda d: start <= d <= end, mask.dates())
        mask_description_dates = filter(lambda d: start <= d <= end, make_dates_from_mask_description(mask_description))

        try:
            self.assertListEqual(mask_dates, mask_description_dates, msg)
        except self.failureException:
            self.fail(self.get_assert_mask_equal_description_fail_msg(mask, mask_description, mask_range))


class AbstractLogHasMessageMixIn(unittest.TestCase):
    def assertLogHasMessage(self, message):
        messages = self.get_log_messages()

        assert message in messages

    def assertLogHasNoMessage(self, message):
        messages = self.get_log_messages()

        assert message not in messages

    def log_has_this_in_messages(self, part):
        return any(part in m for m in self.get_log_messages())


class LogHasMessageMixIn(AbstractLogHasMessageMixIn):
    def setUp(self):
        self.log_collector = get_collector('', u'%(levelname)s: %(message)s', logging.DEBUG)

        self.addCleanup(remove_collector, '', self.log_collector)

        super(LogHasMessageMixIn, self).setUp()

    def get_log_messages(self):
        return self.log_collector.get_collected(clean=False).strip().splitlines(False)


class ClassLogHasMessageMixIn(AbstractLogHasMessageMixIn):
    @classmethod
    def setUpClass(cls):
        cls.log_collector = get_collector('', u'%(levelname)s: %(message)s', logging.DEBUG)

    @classmethod
    def tearDownClass(cls):
        remove_collector('', cls.log_collector)

    @classmethod
    def get_log_messages(cls):
        return cls.log_collector.get_collected(clean=False).strip().splitlines(False)


def replace_now(now):
    from django.conf import settings

    if isinstance(now, basestring):
        now = datetime.strptime(now, "%Y-%m-%d %H:%M:%S")

    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            old_now = settings.ENVIRONMENT_NOW
            settings.ENVIRONMENT_NOW = now
            try:
                result = func(*args, **kwargs)
            finally:
                settings.ENVIRONMENT_NOW = old_now

            return result

        return wrapper

    return decorator
