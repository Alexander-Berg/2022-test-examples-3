import httplib

import gevent.queue

from crypta.lib.python.yt.http_mapper.mapper import ConnectionPool


def test_connection_pool(monkeypatch):
    class MockQueue(object):
        def __init__(self):
            self.items = []

        def get_nowait(self):
            if not self.items:
                raise gevent.queue.Empty
            return self.items.pop()

        def put(self, item):
            self.items.append(item)

    class MockResponse(object):
        def __init__(self, data):
            self.data = data

        def read(self):
            return self.data

    class MockHTTPConnection(object):
        _ID = 0

        def __init__(self, host, timeout):
            self.host = host
            self.ID = MockHTTPConnection._ID
            MockHTTPConnection._ID += 1
            self.timeout = timeout
            self.args = []

        def request(self, *args):
            if args[0] == "exception":
                raise Exception()
            self.args = args

        def getresponse(self):
            return MockResponse(" ".join(str(x) for x in [self.ID, self.host, self.timeout] + list(self.args)))

    hosts = ["host1", "host2"]
    src = [
        ("host1", ["1", "2", "3"]),
        ("host1", ["4", "5"]),
        ("host2", ["6"]),
        ("host2", ["exception"])
    ]
    refs = [
        {"data": "0 host1 10 1 2 3"},
        {"data": "0 host1 10 4 5"},
        {"data": "1 host2 10 6"},
        {"exception": Exception()}
    ]

    monkeypatch.setattr(gevent.queue, "Queue", MockQueue)
    monkeypatch.setattr(httplib, "HTTPConnection", MockHTTPConnection)
    pool = ConnectionPool(10, hosts)

    results = [pool.request(x[0], *x[1]) for x in src]
    for ref, result in zip(refs, results):
        for key in ref:
            assert key in result
            if key != "exception":
                assert result[key] == ref[key]
