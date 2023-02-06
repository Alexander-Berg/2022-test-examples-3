# -*- coding: utf-8 -*-

import urllib


def strip_begin(s, prefix):
    if s.startswith(prefix):
        return s[len(prefix) :]
    return s


def decode_idna(s):
    try:
        return s.decode('idna').encode('utf-8')
    except:
        return s


def urldecode(s):
    return urllib.parse.unquote(s)


def normalize_url(url):
    if '//' not in url:
        url = 'http://' + url
    parsed = urllib.parse.urlparse(url.strip())
    res = urldecode(decode_idna(strip_begin(strip_begin(parsed.netloc.strip(), 'www.'), 'm.')))
    if parsed.path:
        res += parsed.path
    if parsed.query:
        res += '?' + parsed.query
    return res.rstrip('/')


def get_host(url):
    if '//' not in url:
        url = 'http://' + url
    parsed = urllib.parse.urlparse(url.strip())
    return urldecode(decode_idna(strip_begin(strip_begin(parsed.netloc.strip(), 'www.'), 'm.')))
