from abc import ABCMeta, abstractproperty

import json


class DataFile(object):
    __metaclass__ = ABCMeta

    def __init__(self, service):
        self.service = service

    @abstractproperty
    def data(self):
        pass

    def _calc_key(self, method, params):
        ret = "/" + self.service
        if method:
            ret += "/" + method
        if params:
            ret += "?"
            ret += '&'.join(["%s=%s" % p for p in sorted(params)])
        return ret

    def save(self, filename):
        with open(filename, "w") as f:
            json.dump(self.data, f, indent=2, separators=(',', ': '), sort_keys=True)


class Stub(DataFile):
    def __init__(self):
        super(Stub, self).__init__('stub')

    @property
    def data(self):
        return {}
