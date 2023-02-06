import requests
from nose.tools import *
from time import sleep
from utils import *

def test_should_respond_to_ping():
    resp = requests.get("{}/ping".format(INSTANCE1))
    eq_(resp.status_code, 200)
    eq_(resp.text, "pong")

def test_should_store_value():
    key = gen_key()
    val = gen_value()

    resp = set(key, val)
    eq_(resp["status"], 200, resp)

    resp = get(key)
    eq_(resp["status"], 200, resp)
    eq_(resp["value"], val)

def test_should_return_204_on_unexisting_value():
    resp = get(gen_key())
    eq_(resp["status"], 204, resp)

def test_should_overwrite_value():
    key = gen_key()
    val1 = gen_value()
    val2 = gen_value()

    set(key, val1)
    resp = get(key)
    eq_(resp["status"], 200, resp)
    eq_(resp["value"], val1, resp)

    set(key, val2)
    resp = get(key)
    eq_(resp["status"], 200, resp)
    eq_(resp["value"], val2)

def test_should_reset_value_after_ttl():
    key = gen_key()
    val = gen_value()

    set(key, val, ttl=2)
    resp = get(key)
    eq_(resp["value"], val, resp)

    sleep(2)
    resp = get(key)
    eq_(resp["status"], 204, resp)

def test_should_propagate_values():
    key = gen_key()
    val = gen_value()

    set(key, val, raw=False)
    resp = get(key)
    eq_(resp["value"], val, resp)
    resp = get(key, instance=INSTANCE2)
    eq_(resp["value"], val, resp)

def test_should_get_values_from_other_nodes():
    key = gen_key()
    val = gen_value()

    set(key, val)
    resp = get(key, instance=INSTANCE2, raw=False)
    eq_(resp["value"], val, resp)

def test_should_not_log_keys_and_values():
    key = gen_key()
    val = gen_value()

    set(key, val)
    get(key)

    log = open("var/log/rcache/rcache.log", "r").read()
    eq_(log.find(key), -1, key)
    eq_(log.find(val), -1, val)