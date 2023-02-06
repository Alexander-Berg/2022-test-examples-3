# -*- coding: utf-8 -*-

import datetime

import freezegun
import mock
import pytest

import common
import core_dumps
import test_common


@pytest.fixture(scope='function')
def get_instance_name(mocker):
    mocker.patch('core_dumps.get_instance_name', return_value='iva1-0466')


@pytest.mark.parametrize('file_name, process_name', [
    ('report.623657.6', None),
    ('ISS-AGENT--17050%17050_test_report%iss_hook_start%report.565379.S6.20200730T160657', None),
    ('ISS-AGENT--17050%17050_test_report%iss_hook_start%report.565379.S6.20200730T160657.core', 'report'),
])
def test_extract_process_name(file_name, process_name):
    assert core_dumps.extract_process_name_from_core_file_name(file_name) == process_name


@pytest.mark.parametrize('file_name, base_port', [
    ('report.623657.6', None),
    ('17050%17050_test_report%iss_hook_start%report.565379.S6.20200730T160657.core', None),
    ('ISS-AGENT--port%17050_test_report%iss_hook_start%report.565379.S6.20200730T160657.core', None),
    ('ISS-AGENT--17050%17050_test_report%iss_hook_start%report.565379.S6.20200730T160657.core', '17050'),
])
def test_extract_base_port(file_name, base_port):
    assert core_dumps.extract_base_port_from_core_file_name(file_name) == base_port


def make_core_file_name(process_name, current_dt, diff=datetime.timedelta(seconds=0)):
    core_tile_templ = '{file},ISS-AGENT--17050%17050_test_report%iss_hook_start%{process}.565379.S6.{core}.core'
    dt = current_dt - diff
    return core_tile_templ.format(
        process=process_name,
        file=dt.strftime('%Y/%m/%d-%H:%M:%S'),
        core=dt.strftime('%Y%m%dT%H%M%S'))


@freezegun.freeze_time('2020-07-30 17:00:00')
@pytest.mark.usefixtures('get_instance_name')
def test_core_dump_without_monitoring_dir():
    current_date_time = datetime.datetime.now()
    process_name = 'report'
    service_name = 'fresh-report-core-dumps'
    core_files = [make_core_file_name(process_name, current_date_time)]
    with mock.patch('subprocess.check_output', return_value='\n'.join(core_files)):
        with mock.patch('common.config', side_effect=IOError):
            with test_common.OutputCapture() as capture:
                core_dumps.main()
            assert capture.get_stderr() == ''
            for line in capture.get_stdout().split('\n'):
                line = line.strip()
                if line and line.split(';')[0].endswith(service_name):
                    code = common.STATUS_FAILURE
                    message = (
                        'Fresh {} core dumps found (parse delay 5m): https://coredumps.n.yandex-team.ru/index?host_list=iva1-0466:17050'
                    ).format(process_name)
                    assert line == 'PASSIVE-CHECK:{};{};{}'.format(service_name, code, message)


@freezegun.freeze_time('2020-07-30 17:00:00')
@pytest.mark.usefixtures('generate_config', 'get_instance_name')
@pytest.mark.parametrize('process_name, service_name, date_time_diff, alarm', [
    ('report', 'fresh-report-core-dumps', core_dumps.DEFAULT_TTL, False),
    ('report', 'fresh-report-core-dumps', core_dumps.DEFAULT_TTL - datetime.timedelta(seconds=1), True),
    ('report', 'fresh-report-core-dumps-silent', datetime.timedelta(seconds=90), False),
    ('report', 'fresh-report-core-dumps-silent', datetime.timedelta(seconds=89), True),
    ('reanimator', 'market-reanimator-core-dumps', core_dumps.DEFAULT_TTL, False),
    ('reanimator', 'market-reanimator-core-dumps', core_dumps.DEFAULT_TTL - datetime.timedelta(seconds=1), True),
])
def test_core_dump_ttl(process_name, service_name, date_time_diff, alarm):
    current_date_time = datetime.datetime.now()
    core_files = [make_core_file_name(process_name, current_date_time, date_time_diff)]
    with mock.patch('subprocess.check_output', return_value='\n'.join(core_files)):
        with test_common.OutputCapture() as capture:
            core_dumps.main()
        assert capture.get_stderr() == ''
        for line in capture.get_stdout().split('\n'):
            line = line.strip()
            if line and line.split(';')[0].endswith(service_name):
                code = common.STATUS_FAILURE if alarm else common.STATUS_OK
                message = (
                    'Fresh {} core dumps found (parse delay 5m): https://coredumps.n.yandex-team.ru/index?host_list=iva1-0466:17050'
                ).format(process_name) if alarm else common.OK
                assert line == 'PASSIVE-CHECK:{};{};{}'.format(service_name, code, message)


@freezegun.freeze_time('2020-07-30 17:00:00')
@pytest.mark.usefixtures('generate_config', 'get_instance_name')
@pytest.mark.parametrize('process_names, service_name', [
    (['report'], 'fresh-report-core-dumps'),
    (['snippet_report'], 'fresh-report-core-dumps'),
    (['report', 'snippet_report'], 'fresh-report-core-dumps'),
])
def test_process_group_core_dump(process_names, service_name):
    current_date_time = datetime.datetime.now()
    core_files = [
        make_core_file_name(process_name, current_date_time)
        for process_name in process_names
    ]
    with mock.patch('subprocess.check_output', return_value='\n'.join(core_files)):
        with test_common.OutputCapture() as capture:
            core_dumps.main()
        assert capture.get_stderr() == ''
        for line in capture.get_stdout().split('\n'):
            line = line.strip()
            if line and line.split(';')[0].endswith(service_name):
                process_name = ','.join(process_names)
                message = (
                    'Fresh {} core dumps found (parse delay 5m): https://coredumps.n.yandex-team.ru/index?host_list=iva1-0466:17050'
                ).format(process_name)
                assert line == 'PASSIVE-CHECK:{};{};{}'.format(service_name, common.STATUS_FAILURE, message)


@freezegun.freeze_time('2020-07-30 17:00:00')
@pytest.mark.usefixtures('generate_config', 'get_instance_name')
@pytest.mark.parametrize('process_names, alarms', [
    (['report'], [True, True, False]),
    (['reanimator'], [False, False, True]),
    (['report', 'reanimator'], [True, True, True]),
])
def test_all_core_dump_services(process_names, alarms):
    PROCESS_NAMES = [
        'report',
        'report',
        'reanimator',
    ]
    SERVICE_NAMES = [
        'fresh-report-core-dumps-silent',
        'fresh-report-core-dumps',
        'market-reanimator-core-dumps',
    ]
    current_date_time = datetime.datetime.now()
    core_files = [
        make_core_file_name(process_name, current_date_time)
        for process_name in process_names
    ]
    with mock.patch('subprocess.check_output', return_value='\n'.join(core_files)):
        with test_common.OutputCapture() as capture:
            core_dumps.main()
        assert capture.get_stderr() == ''
        lines = iter(sorted(
            line.strip()
            for line in capture.get_stdout().split('\n')
            if line.strip()
        ))
        for process_name, service_name, alarm in zip(PROCESS_NAMES, SERVICE_NAMES, alarms):
            line = next(lines)
            code = common.STATUS_FAILURE if alarm else common.STATUS_OK
            message = (
                'Fresh {} core dumps found (parse delay 5m): https://coredumps.n.yandex-team.ru/index?host_list=iva1-0466:17050'
            ).format(process_name) if alarm else common.OK
            assert line == 'PASSIVE-CHECK:{};{};{}'.format(service_name, code, message)
        with pytest.raises(StopIteration):
            next(lines)


def assertOK(capture):
    assert capture.get_stderr() == ''
    lines = iter(sorted(
        line.strip()
        for line in capture.get_stdout().split('\n')
        if line.strip()
    ))
    assert next(lines) == 'PASSIVE-CHECK:fresh-report-core-dumps-silent;0;Ok'
    assert next(lines) == 'PASSIVE-CHECK:fresh-report-core-dumps;0;Ok'
    assert next(lines) == 'PASSIVE-CHECK:market-reanimator-core-dumps;0;Ok'


@pytest.mark.usefixtures('generate_config', 'get_instance_name')
def test_no_core_dumps():
    with mock.patch('subprocess.check_output', return_value=''):
        with test_common.OutputCapture() as capture:
            core_dumps.main()
        assertOK(capture)


@pytest.mark.usefixtures('generate_config', 'get_instance_name', 'indigo_cluster')
def test_ignore_indigo_clusters():
    current_date_time = datetime.datetime.now()
    core_files = [
        make_core_file_name(process_name, current_date_time)
        for process_name in ['report', 'reanimator']
    ]
    with mock.patch('subprocess.check_output', return_value='\n'.join(core_files)):
        with test_common.OutputCapture() as capture:
            core_dumps.main()
        assertOK(capture)


@pytest.mark.usefixtures('generate_config', 'get_instance_name')
def test_ignore_old_cores_after_removing_indigo():
    current_date_time = datetime.datetime.now()
    core_files = [
        make_core_file_name(process_name, current_date_time)
        for process_name in ['report', 'reanimator']
    ]
    with mock.patch('subprocess.check_output', return_value='\n'.join(core_files)):
        with test_common.colorize_cluster_in_indigo():
            with test_common.OutputCapture() as capture:
                core_dumps.main()
            assertOK(capture)

        with test_common.OutputCapture() as capture:
            core_dumps.main()
        assertOK(capture)
