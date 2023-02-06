#!/usr/bin/env python
import json
import os
import socket
import time
import urllib2

prj = 'disk-testing'


class HTTPJSONCacher(object):
    def __init__(self, cache_file, ttl=300):
        self.cache_file = cache_file
        self.ttl = ttl

    def read(self):
        try:
            with open(self.cache_file, "r") as cache_fd:
                result = cache_fd.read()
        except Exception:
            result = None
        return result

    def update(self, data):
        try:
            cache_dir = os.path.dirname(self.cache_file)
            if not os.path.exists(cache_dir):
                os.mkdir(cache_dir)

            with open(self.cache_file, "w") as cache_fd:
                cache_fd.write(data)
        except Exception:
            return None

    def http_get(self, url, retries, timeout=10):
        for i in range(retries):
            try:
                http_req = urllib2.urlopen(url, timeout=timeout)
                if http_req.getcode() == 200:
                    return http_req.read()
            except Exception:
                pass
        return None

    def http_get_json(self, url, retries):
        json_data = dict()
        bdata = self.http_get(url, retries)
        json_data = json.loads(bdata)
        return json_data

    def get_json(self, url, retries=3):
        cache_needs_update = True
        if os.path.exists(self.cache_file):
            ts_now = time.time()
            ts_cache = os.path.getmtime(self.cache_file)
            if (ts_now - ts_cache) < self.ttl:
                cache_needs_update = False

        if cache_needs_update:
            json_data = self.http_get_json(url, retries)
            if json_data:
                self.update(json.dumps(json_data))

        cache_data = self.read()
        json_data = json.loads(cache_data)
        return json_data


class ConductorInfo(HTTPJSONCacher):
    def __init__(self):
        super(ConductorInfo, self).__init__('/tmp/conductor_cache.json')
        self.url = 'https://c.yandex-team.ru/api-cached/hosts/{0}/?format=json'

    def info(self, host):
        hostinfo = self.get_json(self.url.format(host))
        if not hostinfo:
            print('Host is not configured in conductor.')
            return
        return hostinfo[0]


class Tags(ConductorInfo):
    def get(self, itype):
        host = socket.getfqdn()
        info = self.info(host)
        ctype = info.get('group', '').replace('_', '-')
        datacenter = info.get('short_dc', info.get('root_datacenter', 'nodc'))
        res = [
            host + ':11003@' + itype,
            'a_itype_' + itype,
            'a_prj_' + prj,
            'a_ctype_' + ctype,
            'a_geo_' + datacenter,
        ]
        return ' '.join(res)


if __name__ == '__main__':
    tags = Tags()
    print(tags.get('disk'))
