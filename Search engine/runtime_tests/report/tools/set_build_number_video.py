__author__ = 'fangorn'

from runtime.simcity.messages import build_number
import requests

import sys

if __name__ == "__main__":
    host = sys.argv[1]
    tag = False
    if len(sys.argv) == 3:
        tag = True
    url = 'http://' + host + '/video/search/vl?json=1'
    print url
    r = requests.get(url, verify=False)
    print r.json()
    if tag:
        res = r.json()['report']['version']
        res = '/'.join(res.split("/")[:4])
    else:
        res = r.json()['report']['revision']
    build_number('{}'.format(res))
