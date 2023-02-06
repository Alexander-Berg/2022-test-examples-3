#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.response import JsonResponse, XmlResponse
import unittest

from core.matcher import Absent, Less, Contains, ElementCount

import six

if six.PY3:
    import http.client as httplib
else:
    import httplib


class ResponseMock(object):
    def __init__(self, text):
        self.text = text
        self.status = httplib.OK

    def read(self):
        return self.text


class ResponseTests(unittest.TestCase):
    def test_xml_response_allow_different_len(self):
        response = XmlResponse(ResponseMock('<parent><child1/><child2/><child3/></parent>'), True, '')
        success = True, []
        self.assertEqual(
            success, response.contains('<parent><child1/><child2/><child3/></parent>', allow_different_len=False)
        )

        self.assertEqual(
            success, response.contains('<parent><child1/><child2/><child3/></parent>', allow_different_len=True)
        )

        self.assertNotEqual(
            success,
            response.contains('<parent><child1/><child2/><child3/><child3/></parent>', allow_different_len=False),
        )

        self.assertNotEqual(
            success,
            response.contains('<parent><child1/><child2/><child3/><child3/></parent>', allow_different_len=True),
        )

        self.assertNotEqual(
            success, response.contains('<parent><child1/><child3/></parent>', allow_different_len=False)
        )

        self.assertEqual(success, response.contains('<parent><child1/><child3/></parent>', allow_different_len=True))

    def test_xml_use_regex(self):
        response = XmlResponse(
            ResponseMock('<parent><a>123</a><b>type type </b><c>abcdefg_123</c><d></d></parent>'), True, ''
        )
        self.assertTrue(response.contains('<a>123</a>', use_regex=True)[0])
        self.assertTrue(response.contains(r'<a>1\d{2}</a>', use_regex=True)[0])
        self.assertFalse(response.contains('<a>1</a>', use_regex=True)[0])
        self.assertFalse(response.contains('<a>1</a>', use_regex=False)[0])

        self.assertTrue(response.contains('<b>.* type </b>', use_regex=True)[0])
        self.assertFalse(response.contains('<b>.* type </b>', use_regex=False)[0])
        self.assertFalse(response.contains('<b>.*type</b>', use_regex=True)[0])
        self.assertTrue(response.contains('<b>type.*</b>', use_regex=True)[0])

        self.assertTrue(response.contains(r'<c>\w+</c>', use_regex=True)[0])

        self.assertTrue(response.contains('<d></d>', use_regex=True)[0])
        self.assertTrue(response.contains('<d></d>', use_regex=False)[0])
        self.assertTrue(response.contains('<d>.*</d>', use_regex=True)[0])
        self.assertTrue(response.contains('<d>$^</d>', use_regex=True)[0])

    def test_json_response_allow_partial_match_with_order(self):
        response = JsonResponse(ResponseMock('[1,2,3,4]'), True, '')
        self.assertTrue(response.contains([1, 2, 3], preserve_order=True)[0])
        self.assertTrue(response.contains([2, 3, 4], preserve_order=True)[0])
        self.assertTrue(response.contains([1, 3], preserve_order=True)[0])
        self.assertFalse(response.contains([2, 1], preserve_order=True)[0])
        self.assertTrue(response.contains([2, 1])[0])


class MatcherTests(unittest.TestCase):
    def test_absent(self):
        mock = ResponseMock(
            '''{"null": null, "zero": 0,
                                "empty_str": "", "one": 1}'''
        )
        response = JsonResponse(mock, True, '')
        res, msg = response.contains({"null": Absent()})
        self.assertFalse(res)
        self.assertEqual(
            msg,
            [
                '"{...}/null": "None" did not match pattern <ABSENT> because of [unwanted key with value "None" present in dict]'
            ],
        )

        res, msg = response.contains({"zero": Absent()})
        self.assertFalse(res)
        self.assertEqual(
            msg,
            [
                '"{...}/zero": "0" did not match pattern <ABSENT> because of [unwanted key with value "0" present in dict]'
            ],
        )

        res, msg = response.contains({"empty_str": Absent()}, preserve_order=False)
        self.assertFalse(res)
        self.assertEqual(
            msg,
            [
                '"{...}/empty_str": "" did not match pattern <ABSENT> because of [unwanted key with value "" present in dict]'
            ],
        )

        res, msg = response.contains({"one": Absent()})
        self.assertFalse(res)
        self.assertEqual(
            msg,
            [
                '"{...}/one": "1" did not match pattern <ABSENT> because of [unwanted key with value "1" present in dict]'
            ],
        )

        res, msg = response.contains({"absent": Absent()})
        self.assertTrue(res)
        self.assertEqual(msg, [])

        res, msg = response.contains({"absent": Absent(), "absent2": Absent(), "one": 1}, preserve_order=False)
        self.assertTrue(res)
        self.assertEqual(msg, [])

        res, msg = response.contains({"one": Less(10), "zero": Absent()}, preserve_order=False)
        self.assertFalse(res)
        self.assertEqual(
            msg,
            [
                '"{...}/zero": "0" did not match pattern <ABSENT> because of [unwanted key with value "0" present in dict]'
            ],
        )

        mock = ResponseMock(
            '''{"root1": {"child1": 1, "child2": 2},
                                "root2": {"child1": 1, "child2": 2}}'''
        )
        response = JsonResponse(mock, True, '')

        res, msg = response.contains({'root1': {'child2': 2}, 'root3': Absent()}, preserve_order=False)
        self.assertTrue(res)
        self.assertEqual(msg, [])

        res, msg = response.contains({'root1': {'child3': Absent()}, 'root3': Absent()}, preserve_order=False)
        self.assertTrue(res)
        self.assertEqual(msg, [])

        res, msg = response.contains({'root1': {'child2': Absent()}, 'root3': Absent()}, preserve_order=False)
        self.assertFalse(res)
        self.assertEqual(
            msg,
            [
                '"{...}/root1/child2": "2" did not match pattern <ABSENT> because of [unwanted key with value "2" present in dict]',
                '"{...}/root1" has type "<class \'market.pylibrary.lite.matcher.NoKey\'>", but expected "<type \'dict\'>"',
                '"{...}/root1" has type "<class \'market.pylibrary.lite.matcher.NoKey\'>", but expected "<type \'dict\'>"',
            ],
        )

    def test_wildcards(self):
        mock = ResponseMock('<root a="1" b="2"/>')
        response = XmlResponse(mock, True, '')
        self.assertTrue(response.contains('<root/>')[0])
        self.assertTrue(response.contains('<root a="1"/>')[0])
        self.assertTrue(response.contains('<root a="*"/>')[0])
        self.assertTrue(response.contains('<root a="+"/>')[0])
        self.assertTrue(response.contains('<root c="-"/>')[0])

        res, msg = response.contains('<root a="-"/>')
        self.assertFalse(res)
        self.assertEqual(msg, ['<root> has unexpected attribute "a"'])

    def test_element_count(self):
        mock = ResponseMock('{ "key" : [1, 2, 3] }')
        response = JsonResponse(mock, True, '')
        self.assertTrue(response.contains({"key": ElementCount(3)})[0])
        self.assertFalse(response.contains({"key": ElementCount(4)})[0])
        self.assertFalse(response.contains({"key": ElementCount(2)})[0])
        self.assertTrue(response.contains({"key": ElementCount(Less(5))})[0])


class MatcherRusSupportTest(unittest.TestCase):
    def test_json(self):
        mock = ResponseMock('''{"name": "русский текст"}''')
        response = JsonResponse(mock, True, '')

        res, msg = response.contains({"name": "русский текст"})
        self.assertTrue(res)
        self.assertEqual(msg, [])

        res, msg = response.contains({"name": "левый текст"})
        self.assertFalse(res)
        self.assertEqual(msg, ['"{...}/name" value is "русский текст", but expected "левый текст"'])

        res, msg = response.contains({"name": Contains("текст")})
        self.assertTrue(res)
        self.assertEqual(msg, [])

        res, msg = response.contains({"name": Contains("левый")})
        self.assertFalse(res)
        self.assertEqual(
            msg,
            [
                '"{...}/name": "русский текст" did not match pattern <CONTAINS "левый"> because of [Assertion failed: "левый" not in "русский текст"]'
            ],
        )


if __name__ == '__main__':
    unittest.main()
