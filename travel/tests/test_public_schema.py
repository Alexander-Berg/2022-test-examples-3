# -*- coding: utf-8 -*-
import os

from travel.rasp.admin.lib.unittests.testcase import TestCase
from cysix.filters.cysix_xml_validation import validate_cysix_xml, CysixCheckError
from cysix.tests.utils import get_test_filepath


def force_to_unicode(text):
    return text if isinstance(text, unicode) else text.decode('utf8')


class SchemaTest(TestCase):
    class_fixtures = ['currency.yaml']

    def _test_dir(self, dir):
        for f in os.listdir(dir):
            if f.startswith('valid'):
                valid_xml = os.path.join(dir, f)
                try:
                    validate_cysix_xml(valid_xml)
                except CysixCheckError, e:
                    self.fail(u"Document %s must be valid:\n%s" % (f, repr(e)))

            elif f.startswith('invalid'):
                invalid_xml = os.path.join(dir, f)
                try:
                    validate_cysix_xml(invalid_xml)
                    self.fail(u"Document %s must be invalid" % f)
                except CysixCheckError, e:
                    print u"Expected error %s in %s" % (repr(e), os.path.join(dir, f))

    def test_channel(self):
        self._test_dir(get_test_filepath('data', 'test_public_schema', 'test_channel'))

    def test_group(self):
        self._test_dir(get_test_filepath('data', 'test_public_schema', 'test_group'))

    def test_station(self):
        self._test_dir(get_test_filepath('data', 'test_public_schema', 'test_station'))

    def test_carrier(self):
        self._test_dir(get_test_filepath('data', 'test_public_schema', 'test_carrier'))

    def test_fare(self):
        self._test_dir(get_test_filepath('data', 'test_public_schema', 'test_fare'))

    def test_thread_fare_link(self):
        self._test_dir(get_test_filepath('data', 'test_public_schema', 'test_thread_fare_link'))

    def test_vehicle(self):
        self._test_dir(get_test_filepath('data', 'test_public_schema', 'test_vehicle'))

    def test_thread(self):
        self._test_dir(get_test_filepath('data', 'test_public_schema', 'test_thread'))

    def test_real(self):
        self._test_dir(get_test_filepath('data', 'test_public_schema', 'test_real'))

    def test_timezone(self):
        self._test_dir(get_test_filepath('data', 'test_public_schema', 'test_timezone'))

    def test_consistency(self):
        self._test_dir(get_test_filepath('data', 'test_public_schema', 'test_consistency'))

    def test_comment(self):
        self._test_dir(get_test_filepath('data', 'test_public_schema', 'test_comment'))

    def test_density(self):
        self._test_dir(get_test_filepath('data', 'test_public_schema', 'test_density'))
