from http import HTTPStatus
import urllib

import requests
from requests.adapters import HTTPAdapter
from requests.packages.urllib3.util.retry import Retry


def _replace_last_path_part(src, dst):
    # ('', '/foo') -> '/foo'
    # ('/foo', '/bar') -> '/bar'
    # ('/foo/bar', '/baz') -> '/foo/baz'
    return src[: src.rfind('/')] + dst


def make_url(metasearch, params, path='/yandsearch'):
    pr = urllib.parse.urlsplit(metasearch)
    if not pr.netloc:
        # urlsplit recognizes a netloc only if it is properly introduced by '//'
        pr = urllib.parse.urlsplit('http://' + metasearch)

    pr = pr._replace(path=_replace_last_path_part(pr.path, path))

    qs_parts = []
    if pr.query:
        qs_parts.append(pr.query)
    if params:
        qs_parts.append(urllib.parse.urlencode(params, doseq=True, quote_via=urllib.parse.quote))
        # quote_via is used to encode a space as %20 instead of +
    pr = pr._replace(query='&'.join(qs_parts))

    return urllib.parse.urlunsplit(pr)


def set_up_requests_session():
    session = requests.Session()
    retry = Retry(
        total=3,
        backoff_factor=0.3,
        status_forcelist=(
            HTTPStatus.REQUEST_TIMEOUT,
            HTTPStatus.INTERNAL_SERVER_ERROR,
            HTTPStatus.SERVICE_UNAVAILABLE,
            HTTPStatus.GATEWAY_TIMEOUT,
        ),
    )
    adapter = HTTPAdapter(max_retries=retry)
    session.mount('http://', adapter)
    return session
