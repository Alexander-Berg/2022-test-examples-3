# coding: utf-8

import json
import os


class State(object):
    """
    >>> s = State()
    >>> s.opened
    True

    >>> s.close_balancer()
    >>> s.opened
    False
    >>> s.ping()
    'closed'

    >>> s.open_balancer()
    >>> s.opened
    True
    >>> s.ping()
    '0;ok'
    """
    _keys = [
        'opened',
    ]

    def __init__(self, filepath=None):
        self.opened = True

        self._filepath = filepath
        self.load()

    def close_balancer(self):
        self.opened = False
        self.dump()

    def open_balancer(self):
        self.opened = True
        self.dump()

    def ping(self):
        return '0;ok' if self.opened else 'closed'

    def from_dict(self, data):
        for key in self._keys:
            if key in data:
                setattr(self, key, data[key])

    def to_dict(self):
        data = {}
        for key in self._keys:
            val = getattr(self, key)
            if val is not None:
                data[key] = val
        return data

    def load(self):
        if self._filepath is None:
            return

        data = {}
        try:
            with open(self._filepath) as fobj:
                data = json.loads(fobj.read())
        except IOError:
            pass
        self.from_dict(data)

    def dump(self):
        if self._filepath is None:
            return

        tmppath = '{}.{}.tmp'.format(self._filepath, os.getpid())
        with open(tmppath, 'w') as fobj:
            fobj.write(json.dumps(self.to_dict(), indent=2, sort_keys=True))
        os.rename(tmppath, self._filepath)
