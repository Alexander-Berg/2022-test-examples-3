# -*- coding: utf-8 -*-
import imghdr
import collections
import runtime_tests.util.proto.iface.message as m
import runtime_tests.util.proto.iface.data as d

# connection asserts


def is_closed(conn, timeout=None):
    assert conn.is_closed(timeout), 'Connection is not closed'


def is_not_closed(conn, timeout=None):
    assert not conn.is_closed(timeout), 'Connection is closed'

# HTTP message asserts


def header(msg, name):
    assert name in msg.headers, 'Header "%s" not found' % name


def single_header(msg, name):
    assert msg.headers.get_one(name), 'Header "%s" not found' % name


def header_value(msg, name, value, case_sensitive=True):
    name = name.lower()
    header(msg, name)
    msg_values = msg.headers.get_all(name)

    if not case_sensitive:
        value = value.lower()
        msg_values = [v.lower() for v in msg_values]

    assert value in msg_values, 'Header "%s" with value "%s" not found' % (name, value)


def header_values(msg, name, values, case_sensitive=True):
    name = name.lower()
    header(msg, name)
    msg_values = msg.headers.get_all(name)

    if not case_sensitive:
        values = collections.Counter([v.lower() for v in values])
        msg_values = collections.Counter([v.lower() for v in msg_values])
    else:
        values = collections.Counter(values)
        msg_values = collections.Counter(msg_values)

    store_values = collections.Counter(msg_values)
    msg_values.subtract(values)
    assert min(msg_values.values()) >= 0, \
        'Not all values found for header "%s": got %s, expected %s' % (name, store_values, values)


def no_header(msg, name):
    assert name not in msg.headers, 'Found header "%s"' % name


def no_headers(msg, names):
    for name in names:
        no_header(msg, name)


def no_header_value(msg, name, value, case_sensitive=True):
    name = name.lower()
    msg_values = list()
    if name in msg.headers:
        msg_values = msg.headers.get_all(name)

    if not case_sensitive:
        value = value.lower()
        msg_values = [v.lower() for v in msg_values]

    assert value not in msg_values, 'Found header "%s" with value "%s"' % (name, value)


def headers(msg, names):
    for name in names:
        header(msg, name)


def headers_values(msg, hdrs, case_sensitive=True):
    if isinstance(hdrs, dict):
        hdrs = hdrs.items()
    for name, value in hdrs:
        header_value(msg, name, value, case_sensitive)


def content(msg, data):
    msg_content = msg.data.content
    assert msg_content == data, 'Invalid message content: got %s, expected %s' % (repr(msg_content), repr(data))


def empty_content(msg):
    content(msg, '')


def raw_data(msg, data):
    assert msg.data.raw == data


def version(msg, ver):
    if isinstance(msg, m.Request):
        msg_version = msg.request_line.version
    else:
        msg_version = msg.status_line.version
    assert msg_version == ver, 'Invalid protocol: got "%s", expected "%s"' % (msg_version, ver)


def is_content_length(msg):
    if isinstance(msg, m.Request):
        msg_version = msg.request_line.version
    else:
        msg_version = msg.status_line.version
    header(msg, 'content-length')
    if msg_version == 'HTTP/1.1':
        no_header(msg, 'transfer-encoding')


def is_chunked(msg):
    chunked = isinstance(msg.data, d.ChunkedData)
    assert chunked, 'Not chunked message'


def image(msg, img_type='gif'):
    msg_content = msg.data.content
    assert imghdr.what(None, msg_content) == img_type, 'No %s image in response' % img_type

# HTTP request asserts


def method(req, val):
    assert req.request_line.method == val


def path(req, val):
    req_path = req.request_line.path
    assert req_path == val, 'Invalid path: got "%s", expected "%s"' % (req_path, val)


def scheme(req, val):
    assert req.request_line.scheme == val


def authority(req, val):
    assert req.request_line.authority == val


def cgi(req, name):
    assert name in req.request_line.cgi, 'CGI parameter "%s" not found' % name


def cgi_value(req, name, value):
    cgi(req, name)
    assert value in req.request_line.cgi[name], 'CGI parameter "%s" with value "%s" not found' % (name, value)


def no_cgi_value(req, name, value):
    if name in req.request_line.cgi:
        assert value not in req.request_line.cgi[name], 'Found CGI parameter "%s" with value "%s"' % (name, value)

# HTTP response asserts


def status(resp, expected_status):
    resp_status = resp.status_line.status
    assert resp_status == expected_status, 'Invalid status: got %d, expected %d' % (resp_status, expected_status)


def reason_phrase(resp, expected_reason):
    resp_reason = resp.status_line.reason_phrase
    assert resp_reason == expected_reason, 'Invalid reason phrase: got %s, expected %s' % (resp_reason, expected_reason)


# Process asserts


def alive(process):
    assert process.is_alive(), 'Process %s is not alive' % process.name


def not_alive(process, return_code=0):
    assert not process.is_alive(), 'Process %s is alive' % process.name
    assert process.return_code == return_code, 'Process %s exited with return code %d, expected %d' %\
        (process.name, process.return_code, return_code)
    process.set_finished()
