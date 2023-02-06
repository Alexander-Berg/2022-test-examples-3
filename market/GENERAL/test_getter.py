#!/usr/bin/env python
# -*- coding: utf-8 -*-

import json
import __classic_import  # noqa
import market.dyno.service.mt.env as env

default_response = [{'column_1': 0}, {'column_1': 1}, {'column_1': 2}, {'column_1': 3}, {'column_1': 4}]


class T(env.DynoSuite):
    def test_json(self):
        response = self.dyno.request_json('get?path=//home/market/yamarec/models')
        self.assertFragmentIn(response, default_response)

    def test_with_columns(self):
        response = self.dyno.request_json('get?path=//home/market/yamarec/models&columns=column_1')
        self.assertFragmentIn(response, default_response)

    def test_with_columns_star(self):
        response = self.dyno.request_json('get?path=//home/market/yamarec/models&columns=*')
        self.assertFragmentIn(response, default_response)

    def test_with_table(self):
        response = self.dyno.request_json('get?path=//home/market/yamarec/models&tableName=models')
        self.assertFragmentIn(response, default_response)

    def test_with_cluster(self):
        response = self.dyno.request_json('get?path=//home/market/yamarec/models&cluster=seneca')
        self.assertFragmentIn(response, default_response)

    def test_with_query(self):
        response = self.dyno.request_json(
            'get?path=//home/market/yamarec/models&query=* from [//home/market/yamarec/models] limit 1')
        self.assertFragmentIn(response, default_response)

    def test_full(self):
        response = self.dyno.request_json(
            'get?path=//home/market/yamarec/models&limit=1&columns=column_1&tableName=models&cluster=seneca')
        self.assertFragmentIn(response, default_response)

    def test_with_json_columns(self):
        response = self.dyno.request_json(
            'get?path=//home/market/yamarec/models&limit=1&columns=' + json.dumps(['column_1']))
        self.assertFragmentIn(response, default_response)

    def test_with_simple_filter(self):
        response = self.dyno.request_json(
            'get?path=//home/market/yamarec/models&limit=1&filter=hash:1;partition:2;'
        )
        self.assertFragmentIn(response, default_response)

    def test_with_not_filter(self):
        response = self.dyno.request_json(
            'get?path=//home/market/yamarec/models&limit=1&filter=hash:null;'
        )
        self.assertFragmentIn(response, default_response)

    def test_with_full_filter(self):
        response = self.dyno.request_json(
            'get?path=//home/market/yamarec/models&limit=1&filter=prop1:null;prop2:1;prop3:true;prop4:"my_str";prop5:not 1;'
        )
        self.assertFragmentIn(response, default_response)

    def test_with_full_filter_json(self):
        response = self.dyno.request_json(
            'get?path=//home/market/yamarec/models&limit=1&filter={"prop1":null,"prop2":1,"prop3":true,"prop4":0.1,"prop5":-10,"prop6":"my_string","prop7":"NOT \'my_string\'"}'
        )
        self.assertFragmentIn(response, default_response)


if __name__ == '__main__':
    env.main()
