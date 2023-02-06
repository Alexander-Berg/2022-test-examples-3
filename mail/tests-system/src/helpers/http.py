import json
from urllib.request import Request, urlopen
from urllib.parse import urlencode as urllibencode
from urllib.error import HTTPError  # noqa: F401


def load_url(url, add_headers={}, post_data=None):
    request = Request(url, headers=add_headers, data=post_data.encode() if post_data else None)
    resp = urlopen(request)
    return resp


def load_json(url, add_headers={}, post_data=None):
    resp = load_url(url, add_headers, post_data).read()
    return json.loads(resp)


def urlencode(args, prefix=""):
    return (prefix + urllibencode(args)) if args else ""
