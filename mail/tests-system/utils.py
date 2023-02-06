import json
import requests

INSTANCE1 = "http://localhost:4080"
INSTANCE2 = "http://localhost:4081"
COUNTER = 0

def get(key, instance=INSTANCE1, raw=True):
    url = "{}/{}".format(instance, "raw_get" if raw else "get")
    headers ={ 'Content-Type' : 'application/x-www-form-urlencoded' }
    data = "key={}".format(key)
    resp = requests.post(url, data=data, headers=headers)
    ret = { 'status': resp.status_code, 'text': resp.text }
    if resp.status_code == 200:
        resp_json = json.loads(resp.text)
        ret['value'] = resp_json['value']
        ret['ttl'] = resp_json['ttl']
    return ret

def set(key, value, ttl=100500, instance=INSTANCE1, raw=True):
    url = "{}/{}".format(instance, "raw_set" if raw else "set")
    headers = {'Content-Type' : 'application/x-www-form-urlencoded' }
    data = "key={}&value={}&ttl={}".format(key, value, ttl)
    resp = requests.post(url, data=data, headers=headers)
    ret = { 'status': resp.status_code, 'text': resp.text }
    return ret

def gen_key():
    global COUNTER
    COUNTER += 1
    return "key_{}".format(COUNTER)

def gen_value():
    global COUNTER
    COUNTER += 1
    return "value_{}".format(COUNTER)