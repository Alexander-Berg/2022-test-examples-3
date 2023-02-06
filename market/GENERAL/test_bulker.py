#!/usr/bin/env python
# -*- coding: utf-8 -*-

import json  # noqa
import __classic_import  # noqa
import market.dyno.service.mt.env as env

full_response = [{
    "business.business_id": 0,
    "business.business_value": 0,
    "models.model_id": 0,
    "models.model_value": 0
    }, {
    "business.business_id": 1,
    "business.business_value": 1,
    "models.model_id": 1,
    "models.model_value": 1
    }, {
    "business.business_id": 2,
    "business.business_value": 2,
    "models.model_id": 2,
    "models.model_value": 2
    }, {
    "business.business_id": 3,
    "business.business_value": 3,
    "models.model_id": 3,
    "models.model_value": 3
    }, {
    "business.business_id": 4,
    "business.business_value": 4,
    "models.model_id": 4,
    "models.model_value": 4
}]

short_response = [{
    "business.business_value": 0,
    "models.model_value": 0
    }, {
    "business.business_value": 1,
    "models.model_value": 1
    }, {
    "business.business_value": 2,
    "models.model_value": 2
    }, {
    "business.business_value": 3,
    "models.model_value": 3
    }, {
    "business.business_value": 4,
    "models.model_value": 4
}]

lim_response = [{
    "business.business_id": 0,
    "business.business_value": 0,
    "models.model_id": 0,
    "models.model_value": 0
}]


class T(env.DynoSuite):
    def test_json(self):
        response = self.dyno.request_json(
        "bulk?keys_to_merge=model_id,business_id&requests=[{'path':'//home/market/yamarec/models','columns':'model_value'},{'path':'//home/market/yamarec/business','columns':'business_value'}]")
        self.assertFragmentIn(response, short_response)

    def test_full(self):
        response = self.dyno.request_json(
        "bulk?keys_to_merge=model_id,business_id&requests=[{'path':'//home/market/yamarec/models','columns':'*'},{'path':'//home/market/yamarec/business','columns':'*'}]")
        self.assertFragmentIn(response, full_response)

    def test_limit(self):
        response = self.dyno.request_json(
        "bulk?keys_to_merge=model_id,business_id&requests=[{'path':'//home/market/yamarec/models','columns':'*'},{'path':'//home/market/yamarec/business','columns':'*'}]&limit=1")
        self.assertFragmentIn(response, lim_response)

    def test_multiple_columns_left(self):
        response = self.dyno.request_json(
        "bulk?keys_to_merge=model_id,business_id&requests=[{'path':'//home/market/yamarec/models','columns':'model_id,model_value'},{'path':'//home/market/yamarec/business','columns':'*'}]")
        self.assertFragmentIn(response, full_response)

    def test_multiple_columns_right(self):
        response = self.dyno.request_json(
        "bulk?keys_to_merge=model_id,business_id&requests=[{'path':'//home/market/yamarec/models'},{'path':'//home/market/yamarec/business','columns':'business_id,business_value'}]")
        self.assertFragmentIn(response, full_response)

if __name__ == '__main__':
    env.main()
