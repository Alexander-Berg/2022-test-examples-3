
class ConfDict:
    def __init__(self, d: dict):
        self.d = d

    def __getattr__(self, k):
        v = self.__getitem__(k)
        if isinstance(v, dict):
            return ConfDict(v)
        return v

    def __setattr__(self, k, v):
        if "d" not in self.__dict__:
            super(ConfDict, self).__setattr__(k, v)
        else:
            self.__setitem__(k, v)

    def __setitem__(self, k, v):
        self.d[k] = v

    def __getitem__(self, k):
        return self.d[k]

    def __contains__(self, k):
        return k in self.d

    def __iter__(self):
        return iter(self.d)

    def __bool__(self):
        return bool(self.d)

    def items(self):
        for k, v in self.d.items():
            if isinstance(v, dict):
                yield k, ConfDict(v)
            else:
                yield k, v
