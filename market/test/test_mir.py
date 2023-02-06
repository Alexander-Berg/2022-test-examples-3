# coding: utf-8

import os
import sys

import yatest


CUSTOM_YT_PROXY_VAR = 'CUSTOM_YT_PROXY_VAR'
CUSTOM_YT_TOKEN_PATH_VAR = 'CUSTOM_YT_TOKEN_PATH_VAR'
CUSTOM_YT_HOME_VAR = 'CUSTOM_YT_HOME_VAR'


def debug(s):
    sys.stderr.write('{}\n'.format(s))


def _run_mir(mir_command_name):
    source_root = os.path.normpath(yatest.common.source_path('.'))
    binary_root = os.path.dirname(yatest.common.binary_path('market'))
    mir_program = yatest.common.binary_path('market/idx/mir/mir')
    root_dir = yatest.common.work_path('root')

    debug('cwd: {}'.format(os.getcwd()))
    debug('source_root: {}'.format(source_root))
    debug('binary_root: {}'.format(binary_root))
    debug('mir_program: {}'.format(mir_program))
    debug('root_dir:    {}'.format(root_dir))

    os.environ['LOCAL_MYSQL'] = '1'
    os.environ['SOURCE_ROOT'] = source_root
    os.environ['BINARY_ROOT'] = binary_root

    if CUSTOM_YT_PROXY_VAR in yatest.common.context.flags:
        custom_yt_proxy = yatest.common.context.flags.get(CUSTOM_YT_PROXY_VAR)
        debug('custom yt proxy: {}'.format(custom_yt_proxy))
        os.environ['YT_PROXY'] = custom_yt_proxy

    if CUSTOM_YT_TOKEN_PATH_VAR in yatest.common.context.flags:
        custom_yt_token_path = yatest.common.context.flags.get(CUSTOM_YT_TOKEN_PATH_VAR)
        debug('custom yt token path: {}'.format(custom_yt_token_path))
        os.environ['YT_TOKEN_PATH'] = custom_yt_token_path
        with open(custom_yt_token_path) as f:
            os.environ['YT_TOKEN'] = f.read()

    if CUSTOM_YT_HOME_VAR in yatest.common.context.flags:
        custom_yt_home = yatest.common.context.flags.get(CUSTOM_YT_HOME_VAR)
        debug('custom yt home: {}'.format(custom_yt_home))
        os.environ['YT_HOME'] = custom_yt_home

    res = yatest.common.execute(
        [mir_program, mir_command_name],
        env=os.environ,
        check_exit_code=False,
        wait=True,
        stdout=sys.stdout,
        stderr=sys.stderr,
    )

    if res.exit_code == 0:
        return

    raise Exception("MIR {0} test failed".format(mir_command_name))


def test_mir_white():
    _run_mir('white')


def test_mir_goods():
    _run_mir('goods')
