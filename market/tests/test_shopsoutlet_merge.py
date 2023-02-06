#!/usr/bin/env python

import unittest
import StringIO
from lxml import etree as ET

from getter.service.mbi import merge_shopsoutlet


def make_expected_str(xml_str):
    expected = ET.fromstring(xml_str)
    return ET.tostring(expected, xml_declaration=True, pretty_print=True, encoding="UTF-8")


class TestMergeShopsOutlet(unittest.TestCase):
    def test_simple_merge(self):
        orig = StringIO.StringIO('<OutletInfo><delivery-services></delivery-services></OutletInfo>')
        extra = StringIO.StringIO('<OutletInfo><delivery-services><delivery-service id="123"/></delivery-services></OutletInfo>')
        expected = make_expected_str('<OutletInfo><delivery-services><delivery-service id="123"/></delivery-services></OutletInfo>')
        result = StringIO.StringIO()
        merge_shopsoutlet(result, orig, extra)

        self.assertEqual(result.getvalue(), expected)

    def test_replace_tag_with_attr(self):
        orig = StringIO.StringIO('<OutletInfo><delivery-services><delivery-service id="123" name="DS 123"/></delivery-services></OutletInfo>')
        extra = StringIO.StringIO('<OutletInfo><delivery-services><delivery-service id="123" name="New 123"/></delivery-services></OutletInfo>')
        expected = make_expected_str(
            '<OutletInfo><delivery-services><delivery-service id="123" name="New 123"/></delivery-services></OutletInfo>')
        result = StringIO.StringIO()
        merge_shopsoutlet(result, orig, extra)

        self.assertEqual(result.getvalue(), expected)

    def test_replace_tag_with_subtags(self):
        orig = StringIO.StringIO('''
            <OutletInfo>
                <delivery-services>
                    <delivery-service id="123">
                        <subtag1><subtag2>123</subtag2></subtag1>
                    </delivery-service>
                </delivery-services>
            </OutletInfo>
        ''')
        extra = StringIO.StringIO('''
            <OutletInfo>
                <delivery-services>
                    <delivery-service id="123">
                        <subtag3><subtag4>321</subtag4></subtag3>
                    </delivery-service>
                </delivery-services>
            </OutletInfo>
        ''')
        expected = make_expected_str('''
            <OutletInfo>
                <delivery-services>
                    <delivery-service id="123">
                        <subtag3><subtag4>321</subtag4></subtag3>
                    </delivery-service>
                </delivery-services>
            </OutletInfo>
        ''')

        result = StringIO.StringIO()
        merge_shopsoutlet(result, orig, extra)

        self.assertEqual(result.getvalue(), expected)

    def test_append_only(self):
        orig = StringIO.StringIO('''
            <OutletInfo>
                <delivery-services>
                    <delivery-service id="123">
                        <subtag1><subtag2>123</subtag2></subtag1>
                    </delivery-service>
                </delivery-services>
            </OutletInfo>
        ''')
        extra = StringIO.StringIO('''
            <OutletInfo>
                <delivery-services>
                    <delivery-service id="100500">
                        <subtag3><subtag4>321</subtag4></subtag3>
                    </delivery-service>
                </delivery-services>
            </OutletInfo>
        ''')
        expected = make_expected_str('''
            <OutletInfo>
                <delivery-services>
                    <delivery-service id="123">
                        <subtag1><subtag2>123</subtag2></subtag1>
                    </delivery-service>
                <delivery-service id="100500">
                        <subtag3><subtag4>321</subtag4></subtag3>
                    </delivery-service>
                </delivery-services>
            </OutletInfo>
        ''')

        result = StringIO.StringIO()
        merge_shopsoutlet(result, orig, extra)

        self.assertEqual(result.getvalue(), expected)

    def test_no_tag_in_orig(self):
        orig = StringIO.StringIO('''
            <OutletInfo>
            </OutletInfo>
        ''')
        extra = StringIO.StringIO('''
            <OutletInfo>
                <delivery-services>
                    <delivery-service id="123">
                        <subtag3><subtag4>321</subtag4></subtag3>
                    </delivery-service>
                </delivery-services>
            </OutletInfo>
        ''')

        with self.assertRaises(Exception):
            result = StringIO.StringIO()
            merge_shopsoutlet(result, orig, extra)

    def test_no_tag_in_extra(self):
        orig = StringIO.StringIO('''
            <OutletInfo>
                <delivery-services>
                    <delivery-service id="123">
                        <subtag1><subtag2>123</subtag2></subtag1>
                    </delivery-service>
                </delivery-services>
            </OutletInfo>
        ''')
        extra = StringIO.StringIO('''
            <OutletInfo>
            </OutletInfo>
        ''')
        expected = make_expected_str('''
            <OutletInfo>
                <delivery-services>
                    <delivery-service id="123">
                        <subtag1><subtag2>123</subtag2></subtag1>
                    </delivery-service>
                </delivery-services>
            </OutletInfo>
        ''')

        result = StringIO.StringIO()
        merge_shopsoutlet(result, orig, extra)

        self.assertEqual(result.getvalue(), expected)

    def test_replace_no_subtags_in_orig(self):
        orig = StringIO.StringIO('''
            <OutletInfo>
                <delivery-services>
                </delivery-services>
            </OutletInfo>
        ''')
        extra = StringIO.StringIO('''
            <OutletInfo>
                <delivery-services>
                    <delivery-service id="123">
                        <subtag3><subtag4>321</subtag4></subtag3>
                    </delivery-service>
                </delivery-services>
            </OutletInfo>
        ''')
        expected = make_expected_str('''
            <OutletInfo>
                <delivery-services>
                <delivery-service id="123">
                        <subtag3><subtag4>321</subtag4></subtag3>
                    </delivery-service>
                </delivery-services>
            </OutletInfo>
        ''')

        result = StringIO.StringIO()
        merge_shopsoutlet(result, orig, extra)

        self.assertEqual(result.getvalue(), expected)

    def test_replace_no_subtags_in_extra(self):
        orig = StringIO.StringIO('''
            <OutletInfo>
                <delivery-services>
                    <delivery-service id="123">
                        <subtag1><subtag2>123</subtag2></subtag1>
                    </delivery-service>
                </delivery-services>
            </OutletInfo>
        ''')
        extra = StringIO.StringIO('''
            <OutletInfo>
                <delivery-services>
                </delivery-services>
            </OutletInfo>
        ''')
        expected = make_expected_str('''
            <OutletInfo>
                <delivery-services>
                    <delivery-service id="123">
                        <subtag1><subtag2>123</subtag2></subtag1>
                    </delivery-service>
                </delivery-services>
            </OutletInfo>
        ''')

        result = StringIO.StringIO()
        merge_shopsoutlet(result, orig, extra)

        self.assertEqual(result.getvalue(), expected)

    def test_multiply_tags(self):
        orig = StringIO.StringIO('''
            <OutletInfo>
                <delivery-services>
                    <delivery-service id="123">
                        <subtag1><subtag2>123</subtag2></subtag1>
                    </delivery-service>
                </delivery-services>
                <shops>
                    <shop id="1"><subshop>123</subshop></shop>
                    <shop id="2"><subshop>234</subshop></shop>
                    <shop id="3"><subshop>456</subshop></shop>
                </shops>
            </OutletInfo>
        ''')
        extra = StringIO.StringIO('''
            <OutletInfo>
                <delivery-services>
                    <delivery-service id="123">
                        <subtag3><subtag4>321</subtag4></subtag3>
                    </delivery-service>
                </delivery-services>
                <shops>
                    <shop id="1"><subshop1>321</subshop1></shop>
                    <shop id="2"><subshop2>432</subshop2></shop>
                    <shop id="4"><subshop>654</subshop></shop>
                </shops>

            </OutletInfo>
        ''')
        expected = make_expected_str('''
            <OutletInfo>
                <delivery-services>
                    <delivery-service id="123">
                        <subtag3><subtag4>321</subtag4></subtag3>
                    </delivery-service>
                </delivery-services>
                <shops>
                    <shop id="3"><subshop>456</subshop></shop>
                <shop id="1"><subshop1>321</subshop1></shop>
                    <shop id="2"><subshop2>432</subshop2></shop>
                    <shop id="4"><subshop>654</subshop></shop>
                </shops>
            </OutletInfo>        ''')

        result = StringIO.StringIO()
        merge_shopsoutlet(result, orig, extra)
        self.assertEqual(result.getvalue(), expected)


if __name__ == "__main__":
    unittest.main()
