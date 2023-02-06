import os
import yaml
import tempfile
from contextlib import closing, contextmanager

import pytest

from ora2pg.app.transfer_app import TransferApp, parse_arguments
from ora2pg.app.arguments import Flag, Argument
from ora2pg.app import config_file


def mk_app(arguments, sys_argv):
    args = parse_arguments('', arguments, sys_argv)
    return TransferApp(args)


def get_config():
    return config_file.load_config_file(
        config_file.env_to_config_file('devpack')
    )


@contextmanager
def make_config_file(config):
    new_config_file = None
    with closing(tempfile.NamedTemporaryFile(mode='w', delete=False)) as fd:
        new_config_file = fd.name
        yaml.dump(config, fd)
    try:
        yield new_config_file
    finally:
        os.remove(new_config_file)


@pytest.mark.parametrize(
    ('sys_argv', 'foo_value'),
    [(['-e', 'devpack'], False),
     (['-e', 'devpack', '--foo'], True)]
)
def test_add_flag(sys_argv, foo_value):
    args = mk_app([Flag('--foo', '')], sys_argv).args
    assert hasattr(args, 'foo')
    assert bool(args.foo) == foo_value


@pytest.mark.parametrize(
    ('sys_argv_add', 'omg_value'),
    [([], 'omg'),
     (['--omg', 'WTF'], 'WTF')]
)
def test_ovveride(sys_argv_add, omg_value):
    config = get_config()
    config['omg'] = 'omg'

    with make_config_file(config) as new_config_file:
        OmgArg = Argument('--omg', str, '?!')
        args = mk_app(
            [OmgArg],
            ['--config', new_config_file] + sys_argv_add
        ).args
        assert args.omg == omg_value
