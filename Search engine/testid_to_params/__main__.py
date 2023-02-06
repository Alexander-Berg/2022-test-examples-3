#!/usr/bin/env python
# coding=utf-8
import json
import sys
import urllib


def getsafe(data, path):
    for p in path:
        if data is None:
            return None
        data = data.get(p)
    return data

if __name__ == '__main__':
    output_cgi = []
    output_names = []
    for testid in sys.argv[1:]:
        params = []
        data = json.loads(urllib.urlopen('https://ab.yandex-team.ru/api/testid/%s' % testid).read())
        data = getsafe(json.loads(data['params'])[0], ['CONTEXT', 'MAIN', 'source', "MAPS_GEOSEARCH"])
        if data is None:
            data = []
        for k in data:
            for v in data[k]:
                params.append('search_%s=%s' % (k, v))
        output_cgi.append('&'.join(params))
        output_names.append(testid)

    output = {'beta_cgi': output_cgi, 'beta_name': output_names}
    print json.dumps(output)
