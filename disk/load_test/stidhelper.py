import ssl
import urllib2
import re


def retry_on_http_error(function_to_decorate):
    def wrapper(*args):
        exception = None
        for _ in xrange(3):
            try:
                return function_to_decorate(*args)
            except urllib2.HTTPError as e:
                exception = e
                if e.code in (500, 504):
                    continue

                raise

        raise exception

    return wrapper


def _create_none_ssl_context():
    ctx = ssl.create_default_context()
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE
    return ctx


@retry_on_http_error
def get_url_by_stid(host, stid, qs_params):
    helper_url = host + '/stidhelper/?stid=' + stid
    try:
        resp = urllib2.urlopen(helper_url, context=_create_none_ssl_context())
        body = resp.read()
    except urllib2.HTTPError:
        print 'Error getting stid = ' + stid
        print helper_url
        raise

    match = re.search("<a target='_blank' href='([^']+?/playlist.m3u8)[^']*'>direct-with-cache", body)
    if match is None:
        raise Exception('Body doesn\'t contain url - stid=' + stid + '; body:\n' + body)

    return match.group(1) + '?stid=' + stid + qs_params
