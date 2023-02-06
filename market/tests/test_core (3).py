# coding: utf-8

import os
import time
import pytest

import mock
import yatest.common

from config import config as cfg
from container import Container
from core import Core, TimeoutError


CONTAINER_ROOT = 'mocked_container_root'
COREDUMP = (
    '/coredumps/ISS-AGENT--17050%17050_test_report_market_vla_FY0gcyTvA5D%'
    'iss_hook_start%report.782010.S11.20180420T153848.core'
)
MINI_TANK_CCOREDUMP = 'ISS-AGENT--17050%17050_test%iss%mini_tank.782010.S11.20180420T153848.core'


@pytest.fixture(scope='function')
def mocked_execute(mocker):
    return mocker.patch('util.execute', autospec=True)


@pytest.fixture(scope='function')
def mocked_container(mocker):
    return mocker.patch.object(Container, 'root', new_callable=mock.PropertyMock, return_value=CONTAINER_ROOT)


@pytest.fixture(scope='function')
def mocked_core_actual_size(mocker):
    return mocker.patch.object(Core, 'actual_size', new_callable=mock.PropertyMock, return_value=0)


@pytest.fixture(scope='function')
def mocked_core_expected_size(mocker):
    return mocker.patch.object(Core, 'expected_size', new_callable=mock.PropertyMock, return_value=0)


def test_parse_core_name():
    core = Core(COREDUMP)
    assert core.path == COREDUMP
    assert core.container.name == 'ISS-AGENT--17050/17050_test_report_market_vla_FY0gcyTvA5D/iss_hook_start'
    assert core.bin_name == 'report'
    assert core.signal == '11'
    assert core.timestamp == int(time.mktime(time.strptime('20180420T153848', '%Y%m%dT%H%M%S')))


def test_parse_core_name_of_process_with_dot():
    coredump = (
        '/coredumps/ISS-AGENT--17058%17058_prod_report_market_sas_Uzmv8iXR5rU%'
        'iss_hook_start%python2.7.520229.S6.20210803T181932.core'
    )
    core = Core(coredump)
    assert core.path == coredump
    assert core.container.name == 'ISS-AGENT--17058/17058_prod_report_market_sas_Uzmv8iXR5rU/iss_hook_start'
    assert core.bin_name == 'python2.7'
    assert core.signal == '6'
    assert core.timestamp == int(time.mktime(time.strptime('20210803T181932', '%Y%m%dT%H%M%S')))


def test_bin_path_by_container_root(mocked_execute):
    mocked_execute.return_value = 1, '', ''
    with mock.patch.object(Container, 'root', new_callable=mock.PropertyMock) as mocked_container:
        core = Core(COREDUMP)
        mocked_container.return_value = None
        with pytest.raises(RuntimeError):
            core.bin_path

        mocked_container.return_value = CONTAINER_ROOT
        bin_path = core.bin_path
        assert bin_path == os.path.join(CONTAINER_ROOT, 'bin', core.bin_name)


@pytest.mark.parametrize('core_name, version', [
    ('report_core_version', '2021.3.150.0'),
    ('report_core_w_version', None),
])
def test_report_version(mocked_container, core_name, version):
    core = Core(COREDUMP)
    core.path = yatest.common.source_path(os.path.join('market/tools/corewatcher-rtc/tests/data', core_name))
    assert core.bin_version == version


def test_not_report_version(mocked_container):
    core = Core(MINI_TANK_CCOREDUMP)
    assert core.bin_version is None


@pytest.mark.parametrize('container_volume, output, return_value', [
    (None, 'output_filename', 0),
    (None, 'output_filename', 1),
    ('container_volume', None, 1),
])
def test_run_debugger(
    tmpdir,
    mocked_execute,
    mocked_container,
    container_volume,
    output,
    return_value
):
    expected_result = 'some expected data'
    if output is not None:
        expected_output_file = tmpdir.join(output)
        expected_output_file.write('-')
        output = str(expected_output_file)

    mocked_execute.return_value = return_value, expected_result, ''
    with mock.patch.object(Container, 'volume_path', new_callable=mock.PropertyMock, return_value=container_volume):
        core = Core(COREDUMP)
        result = core._run_debugger('bt', output=output)

        cmd = [cfg.ya_tool_path, 'tool', 'gdb']
        if container_volume:
            cmd += ['-ex', f'set sysroot {container_volume}']
        cmd += ['-ex', 'bt']
        cmd += ['--batch', os.path.join(core.container.root, 'bin', core.bin_name), COREDUMP]
        mocked_execute.assert_called_once_with(cmd, timeout=cfg.core_parsing_timeout)

        if output is not None:
            output_result = expected_output_file.read()
            if return_value == 0:
                assert output_result == result
            else:
                assert output_result == '-'


def test_stack_trace_with_timeout(tmpdir, mocked_container, mocked_core_actual_size, mocked_core_expected_size):
    calls = []
    cfg.update(output_path=str(tmpdir))
    core = Core(COREDUMP)
    for debugger_cmd in ('thread apply all bt', 'thread apply 1 bt'):
        cmd = [cfg.ya_tool_path, 'tool', 'gdb']
        cmd += ['-ex', debugger_cmd]
        cmd += ['--batch', os.path.join(core.container.root, 'bin', core.bin_name), COREDUMP]
        calls.append(mock.call(cmd, timeout=cfg.core_parsing_timeout))

    expected_stack = 'expected_stack'
    with mock.patch.object(Container, 'volume_path', new_callable=mock.PropertyMock, return_value=''):
        with mock.patch('util.execute') as mocked_execute:
            mocked_execute.return_value = cfg.timeout_error_return_code, expected_stack, ''
            stack_trace = core.stack_trace
            assert stack_trace == expected_stack
            mocked_execute.assert_has_calls(calls)


def test_stack_trace(mocked_container, mocked_core_actual_size, mocked_core_expected_size):
    expected_data = 'trace'
    with mock.patch.object(Core, '_run_debugger', return_value=expected_data) as mocked_core:
        core = Core(COREDUMP)
        assert core.stack_trace == expected_data
        mocked_core.assert_called_once_with('thread apply all bt', output=os.path.join(cfg.output_path, 'last.trace'))


@pytest.mark.parametrize('raise_error', [
    (False),
    (True),
])
def test_stack_trace_with_exception(mocked_container, mocked_core_actual_size, mocked_core_expected_size, raise_error):
    expected_data = None if raise_error else 'info'
    with mock.patch.object(Core, '_run_debugger', return_value=expected_data) as mocked_core:
        mocked_core.__name__ = '_run_debugger'  # to avoid conflict mock and @functools.wraps
        if raise_error:
            mocked_core.side_effect = RuntimeError('error')

        core = Core(COREDUMP)
        result = core.stack_trace
        assert result == expected_data

        mocked_core.assert_called_once_with('thread apply all bt', output=os.path.join(cfg.output_path, 'last.trace'))


def test_request_info_with_timeout(mocked_container):
    expected_data = 'some request info'
    with mock.patch.object(Core, '_run_debugger', return_value='req') as mocked_core:
        mocked_core.__name__ = '_run_debugger'  # to avoid conflict mock and @functools.wraps
        mocked_core.side_effect = TimeoutError('timeout', expected_data)

        source_cmd = f'source {cfg.analyzer_path}'
        output_path = os.path.join(cfg.output_path, 'last.request_info')
        mocked_calls = [
            mock.call(f'py target_thread = {t}', source_cmd, output=output_path)
            for t in (0, 1)
        ]
        core = Core(COREDUMP)
        result = core.request_info
        mocked_core.assert_has_calls(mocked_calls)
        assert result == expected_data


@pytest.mark.parametrize('raise_error', [
    (False),
    (True),
])
def test_request_info_with_exception(mocked_container, raise_error):
    expected_data = None if raise_error else 'info'
    with mock.patch.object(Core, '_run_debugger', return_value=expected_data) as mocked_core:
        mocked_core.__name__ = '_run_debugger'  # to avoid conflict mock and @functools.wraps
        if raise_error:
            mocked_core.side_effect = RuntimeError('error')

        core = Core(COREDUMP)
        result = core.request_info
        assert result == expected_data

        mocked_core.assert_called_once_with(
            'py target_thread = 0',
            f'source {cfg.analyzer_path}',
            output=os.path.join(cfg.output_path, 'last.request_info')
        )


def test_not_report_request_info(mocked_container):
    core = Core(MINI_TANK_CCOREDUMP)
    assert core.request_info is None
