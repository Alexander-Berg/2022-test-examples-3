#!/usr/bin/env python
# -*- coding: utf-8 -*-

import sys
sys.path.append('../util')
from make_geosearch_queries import make_cgi_query
from download import obtain_meta_response

class TestResult:
    def __init__(self, search_object, test_case, meta_response):
        self.isOk, object_report_str = search_object.check(meta_response, test_case)
        self.fields = []
        self.fields.append('SUCCESS' if self.isOk else 'FAIL')
        self.fields.append(object_report_str)
        self.fields.append(test_case.signature)
        self.fields.append(test_case.query_text_builder.getQueryText(search_object))
        window = test_case.window_builder.getWindow(search_object)
        self.fields.append('@@'.join((window[0], window[1])))
        self.fields.append(str(test_case.window_builder.getRegion(search_object)))
        what = test_case.query_text_builder.getWhatQueryPart(search_object)
        where = test_case.query_text_builder.getWhereQueryPart(search_object)
        self.fields.append('@@'.join((what, where)))
        self.signature = test_case.signature
        self.as_str = '\t'.join(self.fields)

    def __str__(self):
        return self.as_str

class TestCase(object):
    def __init__(self, query_text_builder, window_builder, search_object_type):
        self.query_text_builder = query_text_builder
        self.window_builder = window_builder
        builders = [str(self.query_text_builder), \
                    str(self.window_builder), \
                    search_object_type.__name__]
        self.signature = ','.join(builders)


    def build_cgi(self, search_object):
        query = self.query_text_builder.getQueryText(search_object)
        region = self.window_builder.getRegion(search_object)
        window = self.window_builder.getWindow(search_object)
        return make_cgi_query(query, \
                              region, \
                              window[0], \
                              window[1], \
                              report = 'xml', \
                              address = 'http://addrs-testing.search.yandex.net/upper/stable/', \
                              additional_cgi='business_show_closed=1')

    def test(self, search_object):
        cgi = self.build_cgi(search_object)
        response = obtain_meta_response(cgi)
        return TestResult(search_object, self, response)
