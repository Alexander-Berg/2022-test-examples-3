from pytest_bdd.feature import (
    parse_line,
    get_step_type,
)
from pytest_bdd.steps import get_args
from pytest_bdd.scenario import find_argumented_step_fixture_name
from pytest_bdd.exceptions import (
    StepDefinitionNotFoundError,
    StepError,
)


class StepsExecuter(object):
    def __init__(self, request, steps):
        self._request = request
        self._steps = self.split_steps(steps)

    def execute(self):
        req = self._request
        if not req:
            raise StepError('should pass request to context')
        for typ, name in self._steps:
            fixture = find_argumented_step_fixture_name(name, typ, req._fixturemanager, req)
            if not fixture:
                raise StepDefinitionNotFoundError('cannot find step definition for: {t} {s}'
                                                  .format(t=typ, s=name))
            func = req.getfixturevalue(fixture)
            kwargs = dict((arg, req.getfixturevalue(arg)) for arg in get_args(func))
            func(**kwargs)

    @staticmethod
    def split_steps(text):
        steps = []
        prev_typ = None
        for l in text.split('\n'):
            line = l.strip()
            if not line:
                continue
            prefix, name = parse_line(line)
            if not prefix:
                if len(steps) == 0:
                    raise StepError('got step without type: {}'.format(line))
                steps[-1][1] += ('\n' + l)
                continue
            typ = get_step_type(line)
            if typ:
                steps.append([typ, name])
                prev_typ = typ
            else:
                if not prev_typ:
                    raise StepError('using continuation prefix for unknown step type: {}'.format(line))
                steps.append([prev_typ, name])
        return steps
