import time
import yaml
import logging
import yatest.common
import yatest.common.network
from collections import defaultdict


_log = logging.getLogger('scenario')


ACTIONS = defaultdict(dict)


class Context(dict):
    def __init__(self, *args, **kwargs):
        super(Context, self).__init__(*args, **kwargs)
        self.__dict__ = self


class Scenario:
    def __init__(self, conf=None, path=None, name=None):
        if name is not None:
            path = yatest.common.test_source_path(f'scenarios/{name}.yaml')
        if path is not None:
            with open(path, 'r') as f:
                conf = yaml.load(f, yaml.FullLoader)
        self._actions = conf['scenario']
        self._functions = {}
        self._idx = 0
        self._ctx = Context()

    @property
    def context(self):
        return self._ctx

    def bind(self, key, value):
        self._ctx[key] = value

    def _normalize_name(self, action):
        return action.replace('-', '_')

    def _expand_vars(self, payload):
        if isinstance(payload, dict):
            for key, value in payload.items():
                payload[key] = self._expand_vars(value)
        elif isinstance(payload, list):
            for i in range(len(payload)):
                payload[i] = self._expand_vars(payload[i])
        elif isinstance(payload, str):
            if payload.startswith('$'):
                tokens = payload[1:].split('.')
                ctx_view = self.context
                for token in tokens:
                    if token:
                        assert token in ctx_view, f'Unbound variable {payload}'
                        ctx_view = ctx_view[token]
                payload = ctx_view
        return payload

    def register_action(self, section, func):
        self._functions[f'{section}/{func.__name__}'] = func

    def register_actions(self, actions):
        for section, funcs in actions.items():
            for func in funcs:
                self.register_action(section, func)

    def step(self):
        if self._idx >= len(self._actions):
            return False
        action = self._actions[self._idx]
        name = self._normalize_name(action['action'])
        section, name = name.split('/', maxsplit=2)
        try:
            func = ACTIONS[section][name]
        except:
            _log.exception('Unkown scenario %s', name)
            raise
        _log.info('Running action %s', name)
        try:
            payload = self._expand_vars(action)
            func(self._ctx, payload)
        except:
            _log.exception('Action %s failed', name)
            raise
        self._idx += 1
        return True

    def run(self):
        while self.step():
            pass


def action(section=''):
    def wrapper(func):
        ACTIONS[section][func.__name__] = func
        return func
    return wrapper


@action('common')
def update_context(ctx, payload):
    for key, value in payload['context'].items():
        ctx[key] = value


@action('common')
def log(ctx, payload):
    _log.info(payload['message'], *payload['args'])


@action('common')
def sleep(ctx, payload):
    time.sleep(payload['seconds'])
