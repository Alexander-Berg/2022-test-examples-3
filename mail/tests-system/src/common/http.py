import json
import urllib.parse
from urllib.request import Request, urlopen, HTTPError  # noqa: F401


def load_url(url, add_headers={}, post_data=None):
    if type(post_data) is str:
        post_data = post_data.encode()
    request = Request(url, headers=add_headers, data=post_data)
    resp = urlopen(request)
    return resp


def load_json(url, add_headers={}, post_data=None):
    resp = load_url(url, add_headers, post_data).read()
    return json.loads(resp)


def urlencode(args, prefix=""):
    return (prefix + urllib.parse.urlencode(args)) if args else ""
