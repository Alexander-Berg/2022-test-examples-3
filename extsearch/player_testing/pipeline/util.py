#!/usr/bin/env python
from urlparse import urlparse


def process_samples(hosts, func, **kwargs):
    for hostrec in hosts:
        if 'Status' in hostrec or 'Samples' not in hostrec:
            continue
        for item in hostrec['Samples']:
            if 'Status' in item or 'Sample' not in item:
                continue
            modified = False
            if 'Host' not in item and 'Host' in hostrec:
                item['Host'] = hostrec['Host']
                modified = True
            func(item, **kwargs)
            if modified:
                del item['Host']


def get_host(s):
    up = urlparse(s)
    if up.scheme and up.netloc:
        return up.netloc
    elif up.path:
        return up.path.split('/')[0]
    else:
        raise Exception('invalid host rec "{}"'.format(s))
