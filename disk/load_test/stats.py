import numpy as np


class StatEntity(object):
    def __init__(self, name=None, value=None):
        self._entities = {}
        self._name = name
        self._value = value

    def __getitem__(self, item):
        if item not in self._entities:
            self._entities[item] = StatEntity(name=item)
        return self._entities[item]

    def __getattribute__(self, item):
        try:
            return object.__getattribute__(self, item)
        except AttributeError:
            if item.startswith('_'):
                raise

            return self[item]

    def rows(self):
        return self._entities.values()

    @property
    def name(self):
        return self._name

    def append(self, value):
        if self._value is None:
            self._value = list()

        if type(self._value) is not list:
            raise

        self._value.append(value)

    def increment(self, delta=1):
        if self._value is None:
            self._value = 0

        self._value += delta

    def percentiles(self, *percentages):
        a = self._np_array()
        if a is None:
            return [[p, 0] for p in percentages]
        else:
            return [[p, np.percentile(a, p)] for p in percentages]

    def percentiles_only(self, *percentages):
        a = self._np_array()
        if a is None:
            return [0.0 for _ in percentages]
        else:
            return [np.percentile(a, p) for p in percentages]

    def percentile(self, p):
        a = self._np_array()
        if a is None:
            return 0.0
        else:
            return np.percentile(a, p)

    def _np_array(self):
        return np.array(self._value) if self._value else None

    def median(self):
        a = np.array(self._value) if self._value else None
        if a is None:
            return float('nan')
        return float(np.median(a))

    def rps(self, duration):
        return 1.0 * self.count() / duration

    def count(self):
        if self._value:
            if isinstance(self._value, (int, long)):
                return self._value
            else:
                return len(self._value)
        elif self._entities:
            return len(self._entities)
        else:
            return 0

    def __str__(self):
        return str(self._value)

    def __int__(self):
        return int(self._value) if self._value else 0

    def __len__(self):
        return len(self._value) if self._value else 0

    def get_values(self, *getters, **kwargs):
        if len(self._entities) == 0:
            return []

        sort_by = kwargs.get('sort_by')
        if sort_by:
            rows = sorted(self.rows(), key=sort_by, reverse=False)
        else:
            rows = self.rows()

        return [[getter(row) for getter in getters] for row in rows]
