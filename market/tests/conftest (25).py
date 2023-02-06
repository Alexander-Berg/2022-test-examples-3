import pytest
import yatest
import os

from pathlib import Path
from ruamel.yaml import YAML
from schema.schema import Schema, Optional, And, Or, Use, Regex


yaml = YAML(typ='safe')

NAME_REGEX = r'^[\w\d_\-+\.\/]+$'
TAG_REGEX = r'^[\w\d\-\.]+$'
PROJECT_PATH = 'market/sre/conf/market-alerts-configs'
INCLUDE_KEY = '#!include'


def get_conf_dir():
    return yatest.common.source_path(PROJECT_PATH + '/configs')


@pytest.fixture(scope='session')
def conf_dir():
    return get_conf_dir()


def get_configs():
    for root, _, files in os.walk(get_conf_dir()):
        for entry in files:
            if (entry.endswith('.yml') or entry.endswith('.yaml')) and entry != 'a.yaml':
                yield os.path.relpath(os.path.join(root, entry), get_conf_dir())


def get_config_content(file):
    globals_path = yatest.common.source_path(PROJECT_PATH + '/globals')
    includes = []
    with open(file) as fd:
        for line in fd.readlines():
            if line.startswith(INCLUDE_KEY):
                includes.append(line.lstrip(INCLUDE_KEY).strip())

        global_vars_list = []
        for include in includes:
            with open(Path(globals_path, include)) as fd_include:
                global_vars_list = fd_include.readlines()
        global_vars = "\n".join(global_vars_list)

        fd.seek(0)
        data = global_vars + fd.read()

    return yaml.load(data)


class Args():
    def __init__(self):
        self.juggler_api = 'http://juggler-api.search.yandex.net'
        self.oauth_token = None
        self.cleanup_tag = '_market_cleanup_tag_'
        self.mandatory_tags = ['market', '_market_']
        self.verbose = False
        self.dry_run = True


@pytest.fixture(scope='module')
def args():
    return Args()


@pytest.fixture(scope='session')
def config_schema():
    config = Schema(
        {
            Optional('global_vars'): list,
            Optional('vars'): list,
            'juggler': {
                'default': {
                    'host': str,
                    str: object
                },
                'checks': [
                    {
                        Optional(And(str, lambda x: x != 'host')): object
                    }
                ]
            }
        }
    )
    return config


@pytest.fixture(scope='session')
def check_schema():
    check = Schema(
        [
            {
                'host': And(Use(str), lambda x: x.islower(), lambda x: len(x) < 192, Regex(NAME_REGEX)),
                'namespace': And(Use(str), lambda x: x.startswith('market')),
                'service': And(Use(str), lambda x: len(x) < 128, Regex(NAME_REGEX)),
                Optional('aggregator'): lambda x: x in (
                    'logic_or', 'logic_and', 'more_than_limit_is_crit', 'more_than_limit_is_problem',
                    'timed_more_than_limit_is_problem', 'timed', 'description_mismatch', 'more_than_limit_in_group',
                    'only_child'),
                Optional('children'): Or(
                    [
                        {
                            'host': str,
                            'service': str,
                            Optional('type'): str,
                            Optional('instance'): str
                        },
                    ],
                    And(
                        [
                            Regex(r'^(\w+%)?(.+?)(:\w+){0,2}$')
                        ],
                        lambda lst: len(lst) == len(set(lst))
                    )
                ),
                Optional('aggregator_kwargs'): {
                    Optional('nodata_mode'): lambda x: x in ('force_ok', 'force_crit', 'force_warn', 'skip'),
                    Optional('hold_crit'): Use(int),
                    Optional('downtimes_mode'): lambda x: x in ('force_ok', 'skip', 'ignore'),
                    Optional('unreach_checks'): [str],
                    Optional('unreach_mode'): lambda x: x in ('skip', 'force_ok'),
                    Optional('unreach_service'): [
                        {
                            'check': str,
                            Optional('hold'): Use(int)
                        }
                    ],
                    Optional('limits'): [
                        {
                            'day_start': Use(int),
                            'day_end': Use(int),
                            'time_start': Use(int),
                            'time_end': Use(int),
                            'warn': Use(str),
                            'crit': Use(str)
                        }
                    ],
                    Optional('crit_limit'): Use(int),  # For more_than_limit_is_problem
                    Optional('warn_limit'): Use(int),  # For more_than_limit_is_problem
                    Optional('mode'): lambda x: x in ('normal', 'percent'),  # For more_than_limit_is_problem
                    Optional('show_ok'): str,  # For more_than_limit_is_problem
                    Optional('ok_desc'): str,
                    Optional('warn_desc'): str,
                    Optional('crit_desc'): str,
                    Optional('nodata_desc'): str,
                    Optional('limit'): Use(int),
                    Optional('percent'): Use(int)
                },
                Optional('active'): str,
                Optional('active_kwargs'): dict,
                Optional('check_options'): dict,
                Optional('refresh_time'): Use(int),
                Optional('ttl'): Use(int),
                Optional('pronounce'): str,
                Optional('tags'): [
                    And(Use(str), Regex(TAG_REGEX)),
                ],
                Optional('meta'): dict,
                Optional('methods'): list,
                Optional('flaps'): Or(
                    'default',
                    {
                        Optional('stable_time'): Use(int),
                        Optional('critical_time'): Use(int),
                        Optional('boost_time'): Use(int)
                    }
                ),
                Optional('notifications'): [
                    {
                        'template_name': lambda x: x in ('on_status_change', 'on_desc_change', 'push',
                                                         'phone_escalation', 'startrek', 'solomon'),
                        'template_kwargs': dict,
                        Optional('description'): str
                    }
                ]
            }
        ]
    )
    return check


@pytest.fixture(scope='session')
def juggler_namespaces():
    """
    Receive namespaces from juggler api
    """
    # args = Args()
    # juggler_api = juggler_sdk.api.JugglerApi(args.juggler_api, dry_run=args.dry_run)
    # reply = juggler_sdk.common.fetch_json(juggler_api._context, '/v2/namespaces/get_namespaces', body=json.dumps({
    #     'limit': 0,
    #     'name': 'market*'}))
    # if reply['limit'] < reply['total']:
    #     # Currently juggler api doesn't support paging while listing namespaces, and there isn't a simple way
    #     # to get more namespaces by prefix
    #     raise RuntimeError('There are total {} namespaces while api limit is only {}. Check CSADMIN-31412.'.format(
    #         reply['total'],
    #         reply['limit']
    #     ))
    namespaces = []
    for item in []:
        namespaces.append(str(item['name']))

    return namespaces
