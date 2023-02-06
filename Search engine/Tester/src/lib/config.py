from datetime import timedelta
import yaml


class VerticalConfig(object):
    def __init__(self, support_line='marty', ticket_age=timedelta(minutes=40)):
        if isinstance(ticket_age, timedelta):
            self.ticket_age = ticket_age
        elif isinstance(ticket_age, dict):
            self.ticket_age = timedelta(**ticket_age)
        elif isinstance(ticket_age, str):
            ticket_age = int(ticket_age)
            self.ticket_age = timedelta(minutes=ticket_age)
        elif isinstance(ticket_age, int):
            self.ticket_age = timedelta(minutes=ticket_age)
        else:
            self.ticket_age = timedelta(minutes=40)
        self.support_line = support_line

    @property
    def dict(self):
        return {
            'support_line': self.support_line,
            'ticket_age': self.ticket_age
        }


class Config(object):
    def __init__(self):
        self._general = {}
        self._verticals = {}
        self._default_support_line = 'marty'
        self._default_ticket_age = timedelta(minutes=40)

    @classmethod
    def from_file(cls, fileobj):
        result = cls()
        config = yaml.load(fileobj.read(), Loader=yaml.Loader)
        result._general = config.get('general')
        verticals = config.get('verticals')
        if verticals is not None and isinstance(verticals, dict):
            for vertical, vert_conf in verticals.items():
                result._verticals[vertical] = VerticalConfig(**vert_conf)
        return result

    def age(self, vertical):
        if vertical in self._verticals:
            return self._verticals[vertical].ticket_age
        return self._default_ticket_age

    def support_line(self, vertical):
        if vertical in self._verticals:
            return self._verticals[vertical].support_line
        return self._default_support_line

    def __getitem__(self, item):
        return self._verticals[item]

    def get(self, item):
        return self._verticals.get(item, VerticalConfig())

    @property
    def dict(self):
        result = {
            'general': self._general,
            'verticals': {k: v.dict for k, v in self._verticals.items()}
        }
        result['verticals']['_default'] = VerticalConfig().dict
        return result
