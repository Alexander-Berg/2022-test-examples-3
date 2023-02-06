# -*- coding: utf-8 -*-

from StringIO import StringIO
import itertools
from itertools import chain
import os
import shutil
import unittest

from getter import currency_rates
from getter import util
from getter.service.currency import _patch_currency_rates as patch
from getter.service.currency import NEW_BEL_CURRENCY, OLD_BEL_CURRENCY, NEW_OLD_BEL_RATE

RATES1 = """\
<?xml version="1.0" encoding="utf-8"?>
<exchange>
  <currencies>
    <currency name="KZT"/>
    <currency name="USD"/>
    <currency name="RUR">
      <alias>RUB</alias>
    </currency>
    <currency name="UE"/>
  </currencies>
  <banks>
    <bank name="CBRF">
      <region>225</region>
      <currency>RUR</currency>
      <rates>
        <rate from="KZT" to="RUR">0.206533</rate>
        <rate from="USD" to="RUR">31.5892</rate>
        <rate from="RUR" to="RUR">1.0</rate>
      </rates>
    </bank>
    <bank name="NBK">
      <region>159</region>
      <currency>KZT</currency>
      <rates>
        <rate from="KZT" to="KZT">1.0</rate>
        <rate from="USD" to="KZT">152.97</rate>
        <rate from="RUR" to="KZT">4.84</rate>
      </rates>
    </bank>
    <bank name="YNDX">
      <region>-1</region>
      <currency>UE</currency>
      <rates>
        <rate from="KZT" to="UE">0.0076923077</rate>
        <rate from="RUR" to="UE">0.033333335</rate>
        <rate from="UE" to="UE">1.0</rate>
      </rates>
    </bank>
  </banks>
</exchange>
"""

RATES_FOR_PATCH_INPUT = """\
<?xml version='1.0' encoding='utf-8'?>
<exchange>
  <currencies>
    <currency name="BYR"/>
  </currencies>
  <banks>
    <bank name="NBRB">
      <region>149</region>
      <currency>BYR</currency>
      <rates>
        <rate from="RUR" to="BYR">301.07</rate>
        <rate from="USD" to="BYR">19989.0</rate>
        <rate from="EUR" to="BYR">22485.0</rate>
        <rate from="UAH" to="BYR">778.39</rate>
        <rate from="BYR" to="BYR">1.0</rate>
        <rate from="KZT" to="BYR">59.65</rate>
      </rates>
    </bank>
  </banks>
</exchange>
"""

RATES_FOR_PATCH_EXPECTED = """\
<?xml version="1.0" encoding="utf-8"?>
<exchange>
  <currencies>
    <currency name="BYR">
      <alias rate_to_primary="10000.0">BYN</alias>
    </currency>
  </currencies>
  <banks>
    <bank name="NBRB">
      <region>149</region>
      <currency>BYR</currency>
      <rates>
        <rate from="RUR" to="BYR">301.07</rate>
        <rate from="USD" to="BYR">19989.0</rate>
        <rate from="EUR" to="BYR">22485.0</rate>
        <rate from="UAH" to="BYR">778.39</rate>
        <rate from="BYR" to="BYR">1.0</rate>
        <rate from="KZT" to="BYR">59.65</rate>
      </rates>
    </bank>
  </banks>
</exchange>
"""

NEW_RATES_FOR_PATCH_INPUT = """\
<?xml version='1.0' encoding='utf-8'?>
<exchange>
  <currencies>
    <currency name="BYN"/>
  </currencies>
  <banks>
    <bank name="NBRB">
      <region>149</region>
      <currency>BYR</currency>
      <rates>
        <rate from="RUR" to="BYR">301.07</rate>
        <rate from="USD" to="BYR">19989.0</rate>
        <rate from="EUR" to="BYR">22485.0</rate>
        <rate from="UAH" to="BYR">778.39</rate>
        <rate from="BYR" to="BYR">1.0</rate>
        <rate from="KZT" to="BYR">59.65</rate>
      </rates>
    </bank>
  </banks>
</exchange>
"""

NEW_RATES_FOR_PATCH_EXPECTED = """\
<?xml version="1.0" encoding="utf-8"?>
<exchange>
  <currencies>
    <currency name="BYN">
      <alias rate_to_primary="0.0001">BYR</alias>
    </currency>
  </currencies>
  <banks>
    <bank name="NBRB">
      <region>149</region>
      <currency>BYR</currency>
      <rates>
        <rate from="RUR" to="BYR">301.07</rate>
        <rate from="USD" to="BYR">19989.0</rate>
        <rate from="EUR" to="BYR">22485.0</rate>
        <rate from="UAH" to="BYR">778.39</rate>
        <rate from="BYR" to="BYR">1.0</rate>
        <rate from="KZT" to="BYR">59.65</rate>
      </rates>
    </bank>
  </banks>
</exchange>
"""


class Test(unittest.TestCase):
    def test_parse(self):
        crates = currency_rates.parse(StringIO(RATES1))
        clist = crates.currencies
        clist.sort()
        self.assertEqual(clist, ['KZT', 'RUR', 'UE', 'USD'])
        self.assertEqual(crates.currency('RUR'), 'RUR')
        self.assertEqual(crates.currency('RUB'), 'RUR')
        self.assertEqual(crates.currency('bla'), None)

    def test_validate(self):
        crates = currency_rates.parse(StringIO(RATES1))
        self.assertRaises(Exception, currency_rates.validate, crates)


class TestPatch(unittest.TestCase):
    ROOTDIR = os.path.join(os.getcwd(), 'tmp')
    INPUT_PATH = os.path.join(ROOTDIR, 'input')
    ACTUAL_PATH = os.path.join(ROOTDIR, 'actual')
    EXPECTED_PATH = os.path.join(ROOTDIR, 'expected')
    CURR_PATCHES = {
        (NEW_BEL_CURRENCY, OLD_BEL_CURRENCY, 1.0/NEW_OLD_BEL_RATE),
        (OLD_BEL_CURRENCY, NEW_BEL_CURRENCY, NEW_OLD_BEL_RATE)}

    def setUp(self):
        shutil.rmtree(self.ROOTDIR, ignore_errors=True)
        util.makedirs(self.ROOTDIR)

    def tearDown(self):
        shutil.rmtree(self.ROOTDIR, ignore_errors=True)

    def testOldCurrency(self):
        self._write(self.INPUT_PATH, RATES_FOR_PATCH_INPUT)
        self._write(self.EXPECTED_PATH, RATES_FOR_PATCH_EXPECTED)
        self._test()

    def testNewCurrency(self):
        self._write(self.INPUT_PATH, NEW_RATES_FOR_PATCH_INPUT)
        self._write(self.EXPECTED_PATH, NEW_RATES_FOR_PATCH_EXPECTED)
        self._test()

    def _test(self):
        patch(self.INPUT_PATH, self.ACTUAL_PATH, patch_currencies=self.CURR_PATCHES)

        from lxml import etree
        parser = etree.XMLParser(remove_blank_text=True)
        expected_tree = etree.parse(self.EXPECTED_PATH, parser).getroot()
        actual_tree = etree.parse(self.ACTUAL_PATH, parser).getroot()
        self.assert_xml_trees_equal(expected_tree, actual_tree)

    def assert_xml_trees_equal(self, e1, e2):
        # http://stackoverflow.com/a/24349916/552014

        def compare(a, b, attr):
            a_attr = getattr(a, attr)
            b_attr = getattr(b, attr)

            self.assertEquals(
                a_attr,
                b_attr,
                "%s not equal for %s, %s" % (attr, a, b))
            return True

        def length_check():
            yield len(e1) == len(e2)

        attrs = ('tag', 'text', 'tail', 'attrib')
        field_checks = (compare(e1, e2, attr) for attr in attrs)
        child_checks = (self.assert_xml_trees_equal(c1, c2) for c1, c2 in itertools.izip(e1, e2))
        return all(chain(field_checks, length_check(), child_checks))

    @staticmethod
    def _write(filepath, content):
        with open(filepath, 'w') as fobj:
            fobj.write(content)


if __name__ == '__main__':
    unittest.main()
