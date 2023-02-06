# coding: utf-8


class Settings(object):
    def __init__(self, args=None, **kwds):
        self.args = args

        self._data = {}
        if args:
            self._data.update(args.__dict__)
        if kwds:
            self._data.update(kwds)

    @property
    def args_as_dict(self):
        if self.args:
            return self.args.__dict__

    def __getattr__(self, name):
        return self._data.get(name)
