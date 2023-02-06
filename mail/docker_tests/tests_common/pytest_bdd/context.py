import logging

from pytest_bdd.exceptions import StepError
from .steps_executer import StepsExecuter

log = logging.getLogger(__name__)


class Context(object):
    def __init__(self):
        self.args = self.text = self.table = None
        self.request = None
        self.request_info = None
        self.outline_param_names = []

    def set_example_params(self, scenario):
        self.outline_param_names = scenario.get_example_params()
        log.debug('Outline param names are now %s', self.outline_param_names)

    def set_args(self, args):
        self.args = args

    def set_extra(self, text):
        if text is None:
            self.text = self.table = None
            return
        text = text.strip()
        if text.startswith('"""') and text.endswith('"""'):
            self.table = None
            self.text = Text(text).text
            return
        if text.startswith('|') and text.endswith('|'):
            self.text = None
            self.table = Table(text)
            return
        raise StepError(
            'Step parse failed, extra block should be between ["""] for text or [|] for table, but got: %s' % text
        )

    def clear_step(self):
        self.args = self.text = self.table = None

    def clear_scenario(self):
        self.request_info = None

    def execute_steps(self, steps):
        StepsExecuter(self.request, steps).execute()

    def __getitem__(self, item):
        return getattr(self, item, None)

    def __iter__(self):
        for item in self.__dict__:
            yield item


class Text(object):
    def __init__(self, text):
        self.text = text.strip('"""').rstrip().lstrip('\n')


class Table(object):
    def __init__(self, text):
        self._rows = []
        lines = text.strip().split('\n')
        self.headings = self._split_line(lines[0])
        if len(lines) > 1:
            for line in lines[1:]:
                values = self._split_line(line)
                if len(self.headings) != len(values):
                    raise StepError('invalid table format, unmatched number of headers and values')
                self.rows.append(Row(dict(zip(self.headings, values))))

    def __iter__(self):
        for r in self.rows:
            yield r

    def __getitem__(self, idx):
        return self.rows[idx]

    def __len__(self):
        return len(self.rows)

    def __repr__(self):
        return repr(self.rows)

    def to_dicts(self):
        return [dict(r) for r in self.rows]

    @property
    def rows(self):
        return self._rows

    @staticmethod
    def _split_line(line):
        line = line.strip()
        if not line.startswith('|'):
            raise StepError('invalid table format, row does not start with |')
        if not line.endswith('|'):
            raise StepError('invalid table format, row does not end with |')
        return [col.strip() for col in line.strip('|').split('|')]


class Row(dict):
    def __getitem__(self, item):
        if isinstance(item, int):
            return list(self.items())[item][1]
        return super(Row, self).__getitem__(item)

    @property
    def headings(self):
        return list(self.keys())


context = Context()
