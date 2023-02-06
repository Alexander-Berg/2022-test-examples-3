# coding: utf-8

import json
import requests


class RatesrvClient:
    def __init__(self, ratesrv_request_prefix):
        self.ratesrv_request_prefix = ratesrv_request_prefix

    def ping(self):
        return requests.get(self.ratesrv_request_prefix + "ping", timeout=0.5)

    def get(self, counters):
        body = json.dumps({"counters": counters})
        return requests.post(self.ratesrv_request_prefix + "counters", data=body, timeout=0.5)

    def increase(self, counters):
        body = json.dumps({"counters": counters})
        return requests.post(self.ratesrv_request_prefix + "counters/increase", data=body, timeout=0.5)
