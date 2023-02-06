#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
import stat

import collections
import httplib
import json
import shutil
import signal
import tempfile
import traceback

from config import *
from constants import *
from fire import fire_at_hp, save_exception
from get_report import get_report_info_by_revision, get_report_bins
from lock import create_report_lock
from remote import *
from report import generate_report_from_artifacts
from utils import *


def execute_command(command, cwd, get_output=False):
    print '>>> {1}: {0}'.format(' '.join(command), cwd)
    if get_output:
        return subprocess.check_output(command, cwd=cwd)
    else:
        subprocess.check_call(command, cwd=cwd)


def get_module_name():
    script_name = os.path.basename(__file__)
    module_name = os.path.splitext(script_name)[0]
    return module_name


def delete_fs_links(svn_dir):
    output = execute_command(['find', '.', '-type', 'l'], svn_dir, get_output=True)
    for line in output.splitlines():
        path = os.path.join(svn_dir, line)
        print 'Deleting link {0}'.format(path)
        os.remove(path)


def svn_delete_unversioned_files(svn_dir):
    unversioned_matcher = re.compile(r'^[?I]\s+(.*)$')
    output = execute_command(['svn', 'status', '--no-ignore'], svn_dir, get_output=True)
    for line in output.splitlines():
        match = unversioned_matcher.search(line)
        if not match:
            continue
        path = os.path.join(svn_dir, match.group(1))
        print 'Removing unversioned {}'.format(path)
        if os.path.isdir(path) and not os.path.islink(path):
            shutil.rmtree(path)
        else:
            os.remove(path)


def revert_svn_changes(svn_dir):
    status_xml = execute_command(['svn', 'status', '--xml'], svn_dir, get_output=True)
    if ET.fromstring(status_xml).find('target/entry') is not None:
        execute_command(['svn', 'revert', '-R', '-q', '.'], svn_dir)


def switch_svn(svn_dir, svn_branch, svn_revision):
    # '--accept working' is needed because sometimes switch fails with conflicts
    # Example: switch to ^/branches/market/report-2017.1.33/arcadia from ^/trunk/arcadia
    switch_cmd = ['svn', 'switch', '-q', '--accept', 'working']
    if svn_revision is not None:
        switch_cmd.extend(['-r', str(svn_revision)])
    switch_cmd.append(svn_branch)
    execute_command(switch_cmd, svn_dir)
    # Clean up potential conflicts.
    revert_svn_changes(svn_dir)


def clean_rty_configs(cluster_index):
    execute_command_on_hp_in_parallel(
        cluster_index, 'sudo rm -rf {0}'.format(os.path.join(get_remote_temp_dir(), 'rty')))


def copy_rty_configs(cluster_index, svn_dir):
    rty_conf_path_parts = ['market', 'report', 'runtime_cloud', 'rty_configs']
    rty_conf_path = os.path.join(svn_dir, *rty_conf_path_parts)
    if not os.path.isdir(rty_conf_path):
        get_rty_conf_cmd = [
            path_to_ya(svn_dir), 'make', '--checkout', '-r', '-C', os.path.join(*rty_conf_path_parts)]
        try:
            execute_command(get_rty_conf_cmd, svn_dir)
        except:
            return
    if not os.path.isfile(os.path.join(rty_conf_path, 'erf.cfg')):
        return
    copy_files_to_hp_in_parallel(
        cluster_index,
        [rty_conf_path],
        os.path.join(get_remote_temp_dir(), 'rty'))


def generate_config_with_report_ctl(cluster_index, svn_dir, report_type, enable_docfetcher, report_port):
    settings_py_path = os.path.join(svn_dir, 'market', 'report', 'runtime_cloud', 'settings', 'settings.py')
    if not os.path.isfile(settings_py_path):
        return False
    with open(os.path.join(settings_py_path), 'r') as settings_py_file:
        if 'is_perf_testing' not in settings_py_file.read():
            return False
    snippet_host_name = get_hp_snippet_host_name(cluster_index)
    create_temp_dir_on_hp_cluster(cluster_index)
    if not is_blue(report_type):
        create_temp_dir_on_host(snippet_host_name)

    REPORT_CTL_CONF = '''[main]
environment = perf-testing
location = sas
host = {host_name}
cluster_index = {cluster_index}
host_index = {host_index}
nginx_port = {nginx_port}
report_port = {port}
bin_directory =
conf_directory = {temp_directory}
tvmtool_token_path = /var/lib/tvmtool/local.auth
data_directory = {temp_directory}/data
persistent_directory = {temp_directory}/pdata
logs_directory = /var/log/search
pid_directory = /var/run/search
search_directory = /var/lib/search
report_role = {report_role}
report_subrole = {report_subrole}
report_log_prefix =
log_file_name_prefix = {log_file_name_prefix}
backends_config_name = {backends_config_name}
formulas_directory = /usr/share/search
click_daemon_keys_directory = /usr/share/yandex-clickdaemon
enable_docfetcher = {enable_docfetcher}
max_index_rps = 100500
is_rtc = False
'''
    REPORT_SUBROLES = ('market', 'parallel', 'api', 'int', 'blue-market', 'blue-api')
    report_subrole = REPORT_SUBROLES[report_type]
    report_ctl_path = os.path.join(svn_dir, 'market', 'report', 'runtime_cloud', 'report_ctl', 'report_ctl')
    report_color = 'blue-' if is_blue(report_type) else ''
    report_cluster_index = '' if cluster_index == 0 else str(cluster_index + 1)
    backends_config_name = 'backends_{}hp{}'.format(report_color, report_cluster_index)
    temp_dir = tempfile.mkdtemp()
    try:
        report_ctl_src_dst = list()
        for host_index in get_host_range():
            report_ctl_conf_path = os.path.join(temp_dir, 'report_ctl.conf' + str(host_index))
            with open(report_ctl_conf_path, 'w') as report_ctl_conf_file:
                report_ctl_conf_file.write(REPORT_CTL_CONF.format(
                    cluster_index=cluster_index,
                    host_index=host_index,
                    nginx_port=report_port - 1,
                    port=report_port,
                    host_name=get_hp_host_name(cluster_index, host_index),
                    temp_directory=get_remote_temp_dir(),
                    report_role='market-report',
                    report_subrole=report_subrole,
                    log_file_name_prefix='market-parallel-' if report_type == REPORT_PARALLEL else 'market-',
                    backends_config_name=backends_config_name,
                    enable_docfetcher=enable_docfetcher
                ))
            report_ctl_src_dst.append((report_ctl_conf_path, os.path.join(get_remote_temp_dir(), 'report_ctl.conf')))
        copy_files_to_hp_in_parallel(cluster_index, report_ctl_path, get_remote_temp_dir())
        copy_distinct_files_to_hp_in_parallel(cluster_index, report_ctl_src_dst)

        clean_rty_configs(cluster_index)
        copy_rty_configs(cluster_index, svn_dir)

        if not is_blue(report_type):
            report_ctl_conf_path = os.path.join(temp_dir, 'report_ctl.conf')
            with open(report_ctl_conf_path, 'w') as report_ctl_conf_file:
                report_ctl_conf_file.write(REPORT_CTL_CONF.format(
                    cluster_index=cluster_index,
                    host_index=CLUSTER_SIZE,
                    nginx_port=report_port - 1,
                    port=report_port,
                    host_name=snippet_host_name,
                    temp_directory=get_remote_temp_dir(),
                    report_role='market-snippet-report',
                    report_subrole=report_subrole,
                    log_file_name_prefix='market-snippet-',
                    backends_config_name=backends_config_name,
                    enable_docfetcher=enable_docfetcher
                ))
            copy_files_to_host(
                snippet_host_name,
                [report_ctl_path, report_ctl_conf_path],
                get_remote_temp_dir())
    finally:
        shutil.rmtree(temp_dir)

    # sudo нужно т.к. report_ctl пишет в /var/log/search/report_ctl.log
    execute_command_on_hp_in_parallel(
        cluster_index,
        'sudo {0}/report_ctl {0}/report_ctl.conf generate-config'.format(get_remote_temp_dir()))
    execute_command_on_hp_in_parallel(
        cluster_index,
        'sudo sed -i -e "s/17051/{0}/g" {1}/report.conf'.format(report_port, get_remote_temp_dir()))
    execute_command_on_hp_in_parallel(
        cluster_index,
        'sudo cp {0}/report.conf /etc/yandex/market-report-configs/generated/market-report.cfg'.format(get_remote_temp_dir()))
    change_mode(cluster_index, os.path.join(get_remote_temp_dir(), 'rty'))

    if not is_blue(report_type):
        execute_command_on_host(
            snippet_host_name,
            'sudo {0}/report_ctl {0}/report_ctl.conf generate-config'.format(get_remote_temp_dir()))
        execute_command_on_host(
            snippet_host_name,
            'sudo sed -i -e "s/17051/{0}/g" {1}/report.conf'.format(report_port, get_remote_temp_dir()))
        execute_command_on_host(
            snippet_host_name,
            'sudo cp {0}/report.conf /etc/yandex/market-report-configs/generated/market-snippet-report.cfg'.format(get_remote_temp_dir()))

    return True


def generate_config(cluster_index, svn_dir, report_type, enable_docfetcher, report_port):
    if generate_config_with_report_ctl(cluster_index, svn_dir, report_type, enable_docfetcher, report_port):
        return
    snippet_host_name = get_hp_snippet_host_name(cluster_index)
    is_parallel_report = report_type == REPORT_PARALLEL
    servant_name = 'market-parallel-report' if is_parallel_report else 'market-report'
    port_number = 17053 if is_parallel_report else 17051
    config_generator_path_parts = ['market', 'report', 'configs', 'generator']
    config_generator_path = os.path.join(svn_dir, *config_generator_path_parts)
    if not os.path.isdir(config_generator_path):
        get_config_generator_cmd = [
            path_to_ya(svn_dir), 'make', '--checkout', '-r', '-C', os.path.join(*config_generator_path_parts)]
        execute_command(get_config_generator_cmd, svn_dir)
    with open(os.path.join(config_generator_path, 'generate.sh'), 'r') as generator_file:
        has_config_generator_support = 'TEMPLATE_DIR' in generator_file.read()
    if has_config_generator_support:
        create_temp_dir_on_hp_cluster(cluster_index)
        copy_files_to_hp_in_parallel(cluster_index, config_generator_path, get_remote_temp_dir())
        execute_command_on_hp_in_parallel(
            cluster_index,
            'sudo SERVANTS_LIST={1} DESTINATION_DIRECTORY={0} TEMPLATE_DIR={0}/generator/etc {0}/generator/generate.sh'.format(get_remote_temp_dir(), servant_name))
        execute_command_on_hp_in_parallel(
            cluster_index,
            'sudo sed -i -e "s/{0}/{1}/g" {2}/{3}.cfg'.format(port_number, report_port, get_remote_temp_dir(), servant_name))
        execute_command_on_hp_in_parallel(
            cluster_index,
            'sudo cp {0}/{1}.cfg /etc/yandex/market-report-configs/generated/market-report.cfg'.format(get_remote_temp_dir(), servant_name))

        if not is_blue(report_type):
            create_temp_dir_on_host(snippet_host_name)
            copy_files_to_host(snippet_host_name, config_generator_path, get_remote_temp_dir())
            execute_command_on_host(
                snippet_host_name,
                'sudo SERVANTS_LIST=market-snippet-report DESTINATION_DIRECTORY={0} TEMPLATE_DIR={0}/generator/etc {0}/generator/generate.sh'.format(get_remote_temp_dir()))
            execute_command_on_host(
                snippet_host_name,
                'sudo cp {0}/market-snippet-report.cfg /etc/yandex/market-report-configs/generated'.format(get_remote_temp_dir()))
    else:
        execute_command_on_hp_in_parallel(
            cluster_index,
            'sudo SERVANTS_LIST={0} /usr/lib/yandex/market-report-configs/generate.sh'.format(servant_name))
        if is_parallel_report:
            execute_command_on_hp_in_parallel(
                cluster_index,
                'sudo mv /etc/yandex/market-report-configs/generated/{0}.cfg /etc/yandex/market-report-configs/generated/market-report.cfg'.format(servant_name))
        execute_command_on_hp_in_parallel(
            cluster_index,
            'sudo sed -i -e "s/{0}/{1}/g" /etc/yandex/market-report-configs/generated/market-report.cfg'.format(port_number, report_port))


def regenerate_config(cluster_index):
    execute_command_on_hp_in_parallel(
        cluster_index,
        'sudo /usr/lib/yandex/market-report-configs/generate.sh')
    execute_command_on_host(
        get_hp_snippet_host_name(cluster_index),
        'sudo /usr/lib/yandex/market-report-configs/generate.sh')


def stop_services_on_hp(cluster_index):
    COMMAND = '[[ $(sudo service cron status) == "cron stop/waiting" ]] || sudo service cron stop'
    execute_command_on_hp_in_parallel(cluster_index, COMMAND)
    execute_command_on_host(get_hp_snippet_host_name(cluster_index), COMMAND)
    YASM_STOP_CMD = 'sudo service yasmagent stop'
    execute_command_on_hp_in_parallel(cluster_index, YASM_STOP_CMD)
    execute_command_on_host(get_hp_snippet_host_name(cluster_index), YASM_STOP_CMD)


def start_services_on_hp(cluster_index):
    COMMAND = '[[ $(sudo service cron status) != "cron stop/waiting" ]] || sudo service cron start'
    execute_command_on_hp_in_parallel(cluster_index, COMMAND)
    execute_command_on_host(get_hp_snippet_host_name(cluster_index), COMMAND)


def start_ordinary_report_on_hp(cluster_index):
    # clean_rty_data_on_hp_cluster(cluster_index)
    execute_command_on_hp_in_parallel(cluster_index, 'sudo service httpsearch start')


def start_snippet_report_on_hp(cluster_index):
    execute_command_on_host(get_hp_snippet_host_name(cluster_index), 'sudo service httpsearch start')


def start_report_on_hp(cluster_index):
    start_ordinary_report_on_hp(cluster_index)
    start_snippet_report_on_hp(cluster_index)


def stop_report_on_hp(cluster_index):
    COMMAND = 'sudo pkill -KILL -u httpsearch; exit 0'
    execute_command_on_hp_in_parallel(cluster_index, COMMAND, snippet_command=COMMAND)


def restart_report_on_hp(cluster_index):
    stop_report_on_hp(cluster_index)
    start_report_on_hp(cluster_index)
    start_snippet_report_on_hp(cluster_index)


def kill_stale_agents(cluster_index):
    try:
        COMMAND = r'sudo pkill -KILL -f "/agent\.py.+/agent\.cfg"; sudo pkill -KILL -f "\/tmp\/telegraf"; exit 0'
        execute_command_on_hp_in_parallel(cluster_index, COMMAND)
        execute_command_on_host(get_hp_snippet_host_name(cluster_index), COMMAND)
        # execute_command_on_host('tank01ht', COMMAND)
    except KeyboardInterrupt:
        raise
    except:
        print traceback.format_exc()


def wait_for_report_to_start_on_hp(cluster_index, build_type, report_port):
    timeout = 500 if build_type == BUILD_DEBUG else 300
    execute_script_on_hp_cluster(
        cluster_index,
        'remote_wait_for_report_to_start.py {0} {1}'.format(timeout, report_port))


def install_market_report_formulas_on_hp(cluster_index, svn_dir):
    checkout_cmd = [path_to_ya(svn_dir),
                    'make', '--checkout', '-j0',
                    '-C', os.path.join('market', 'report', 'data', 'formulas')]
    try:
        execute_command(checkout_cmd, svn_dir)
    except:
        return
    formulas_path = os.path.join(svn_dir, 'market', 'report', 'data', 'formulas')
    build_command = [path_to_ya(svn_dir),
                     'package',
                     '--custom-version=hp',
                     'yandex-market-report-formulas.json']
    execute_command(build_command, formulas_path)
    PACKAGE_NAME = 'yandex-market-report-formulas.hp.tar.gz'
    package_path = os.path.join(formulas_path, PACKAGE_NAME)
    copy_files_to_hp_in_parallel(cluster_index, package_path, get_remote_temp_dir())
    execute_command_on_hp_in_parallel(cluster_index, 'sudo rm -rf /usr/share/search/formulas')
    execute_command_on_hp_in_parallel(
        cluster_index,
        'sudo tar xf {0} -C /'.format(os.path.join(get_remote_temp_dir(), PACKAGE_NAME)))


def install_custom_report_on_hp(cluster_index, svn_dir, build_type, report_type, report_port):
    stop_report_on_hp(cluster_index)

    create_temp_dir_on_hp_cluster(cluster_index)

    install_market_report_formulas_on_hp(cluster_index, svn_dir)

    report_bin_path = os.path.join(svn_dir, 'market', 'report', 'report_bin', 'report_bin')
    basesearch_path = os.path.join(svn_dir, 'market', 'report', 'basesearch', 'basesearch')

    temp_dir = tempfile.mkdtemp()
    compressed_file_path = os.path.join(temp_dir, "report_bin.gz")
    try:
        with open(compressed_file_path, "w") as compressed_file:
            try:
                print '>>> Compressing {0}'.format(report_bin_path)
                subprocess.check_call(
                    ['pigz', '-c', '--fast', report_bin_path], stdout=compressed_file)
            except subprocess.CalledProcessError:
                compressed_file_path = report_bin_path
        copy_files_to_hp_in_parallel(
            cluster_index, compressed_file_path, get_remote_temp_dir())
    finally:
        shutil.rmtree(temp_dir)

    remote_report_bin_path = '{0}/report_bin'.format(get_remote_temp_dir())
    if compressed_file_path.endswith('.gz'):
        execute_command_on_hp_in_parallel(
            cluster_index, 'pigz -d -f {0}.gz'.format(remote_report_bin_path))
        execute_command_on_hp_in_parallel(
            cluster_index, 'chmod +x {0}'.format(remote_report_bin_path))

    execute_command_on_hp_in_parallel(
        cluster_index, 'sudo rm -f /usr/bin/market-report.httpsearch')
    execute_command_on_hp_in_parallel(
        cluster_index, 'sudo ln -s {0} /usr/bin/market-report.httpsearch'.format(remote_report_bin_path))

    start_ordinary_report_on_hp(cluster_index)

    if not is_blue(report_type):
        snippet_host_name = get_hp_snippet_host_name(cluster_index)
        create_temp_dir_on_host(snippet_host_name)
        remote_basesearch_path = '{0}/basesearch'.format(get_remote_temp_dir())
        copy_files_to_host(snippet_host_name, basesearch_path, remote_basesearch_path)
        execute_command_on_host(
            snippet_host_name, 'sudo rm -f /usr/bin/market-snippet-report.httpsearch')
        execute_command_on_host(
            snippet_host_name,
            'sudo ln -s {0} /usr/bin/market-snippet-report.httpsearch'.format(remote_basesearch_path))
        execute_command_on_host(
            snippet_host_name, 'chmod +x {0}'.format(remote_basesearch_path))
        start_snippet_report_on_hp(cluster_index)

    wait_for_report_to_start_on_hp(cluster_index, build_type, report_port)


def uninstall_custom_report_on_hp(cluster_index):
    stop_report_on_hp(cluster_index)
    regenerate_config(cluster_index)
    execute_command_on_hp_in_parallel(
        cluster_index, 'sudo rm -rf {0}'.format(get_remote_temp_dir()))
    execute_command_on_hp_in_parallel(
        cluster_index, 'sudo rm -f /usr/bin/market-report.httpsearch')
    execute_command_on_hp_in_parallel(
        cluster_index,
        'sudo ln -s ../lib/yandex/report/report_bin /usr/bin/market-report.httpsearch')
    snippet_host_name = get_hp_snippet_host_name(cluster_index)
    execute_command_on_host(
        snippet_host_name, 'sudo rm -rf {0}'.format(get_remote_temp_dir()))
    execute_command_on_host(
        snippet_host_name,
        'sudo rm -f /usr/bin/market-snippet-report.httpsearch')
    execute_command_on_host(
        snippet_host_name,
        'sudo ln -s ../lib/yandex/basesearch /usr/bin/market-snippet-report.httpsearch')
    start_report_on_hp(cluster_index)


def path_to_ya(svn_dir):
    ya_path = os.path.abspath(os.path.join(svn_dir, 'ya'))
    if not os.path.isfile(ya_path):
        # Try obsolete path
        ya_path = os.path.abspath(os.path.join(svn_dir, 'devtools', 'ya', 'ya'))
    return ya_path


def create_svn_dir_if_absent(svn_dir):
    if os.path.isdir(svn_dir):
        return
    temp_dir = tempfile.mkdtemp()
    try:
        execute_command([
            'svn', 'checkout', 'svn+ssh://arcadia.yandex.ru/arc/trunk/arcadia',
            '--depth', 'files', temp_dir
        ], None)
        execute_command([os.path.join(temp_dir, 'ya'), 'clone', '--no-junk', svn_dir], None)
    finally:
        shutil.rmtree(temp_dir)


def build_report_ctl(svn_dir):
    report_ctl_path = os.path.join(svn_dir, 'market', 'report', 'runtime_cloud', 'report_ctl', 'report_ctl')
    try:
        os.remove(report_ctl_path)
    except:
        pass
    if os.path.isfile(report_ctl_path):
        raise Exception('Unable to delete old version of report_ctl')
    build_report_ctl_cmd = [
        path_to_ya(svn_dir), 'make', '--checkout', '-r',
        '-C', os.path.join('market', 'report', 'runtime_cloud', 'report_ctl')]
    try:
        execute_command(build_report_ctl_cmd, svn_dir)
    except:
        pass


BUILD_RELEASE = 0
BUILD_DEBUG = 1
BUILD_PROFILE = 2


def get_build_type(config):
    return BUILD_PROFILE if config.record_perf else BUILD_RELEASE


def build_report(svn_dir, build_type, dist_build, extra_build_options):
    build_command = [path_to_ya(svn_dir), 'make', '--checkout']
    if build_type == BUILD_RELEASE:
        build_command.append('-r')
    elif build_type == BUILD_DEBUG:
        build_command.append('-d')
    elif build_type == BUILD_PROFILE:
        build_command.append('--build=profile')
    if dist_build:
        build_command.extend(['--dist', '-E'])
    if extra_build_options:
        build_command.extend(shlex.split(extra_build_options))
    build_command.extend(['-C', os.path.join('market', 'report', 'report_bin')])
    build_command.extend(['-C', os.path.join('market', 'report', 'basesearch')])
    execute_command(build_command, svn_dir)
    build_report_ctl(svn_dir)


def patch_debian_version(svn_dir, description):
    changelog_path = os.path.join(svn_dir, 'market', 'report', 'debian', 'changelog')
    if not os.path.isfile(changelog_path):
        checkout_cmd = [
            path_to_ya(svn_dir), 'make', '--checkout', '-j0',
            '-C', os.path.join('market', 'report', 'debian')
        ]
        execute_command(checkout_cmd, svn_dir)
    with open(changelog_path, 'r') as f:
        text = f.read()
    text = text.replace('0.0.1', description)
    with open(changelog_path, 'w') as f:
        f.write(text)


def build_report_with_fallback(svn_dir, build_type, dist_build, extra_build_options, description):
    patch_debian_version(svn_dir, description)
    if dist_build:
        try:
            build_report(svn_dir, build_type, dist_build, extra_build_options)
            return
        except KeyboardInterrupt:
            raise
        except:
            print traceback.format_exc()
    build_report(svn_dir, build_type, False, extra_build_options)


def get_svn_branch(svn_branch):
    return svn_branch if svn_branch is not None else '^/trunk/arcadia'


def prepare_svn_revision(svn_dir, svn_revision, svn_branch):
    execute_command(['svn', 'revert', '--recursive', '-q', '.'], svn_dir)
    svn_delete_unversioned_files(svn_dir)
    delete_fs_links(svn_dir)
    switch_svn(svn_dir, get_svn_branch(svn_branch), svn_revision)


def build_custom_report(svn_dir, svn_revision, svn_branch, svn_patch, build_type, dist_build, extra_build_options, description):
    prepare_svn_revision(svn_dir, svn_revision, svn_branch)
    if svn_patch is not None:
        apply_svn_patch(svn_dir, svn_patch)
    build_report_with_fallback(svn_dir, build_type, dist_build, extra_build_options, description)


def build_report_from_git_branch(git_dir, svn_dir, git_branch, build_type, dist_build, extra_build_options, description):
    sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
    from prepare_svn_commit_from_git_branch import prepare_svn_commit_from_git_branch
    prepare_svn_commit_from_git_branch(git_dir, svn_dir, git_branch)
    build_report_with_fallback(svn_dir, build_type, dist_build, extra_build_options, description)


def clean_current_index(cluster_index):
    execute_command_on_hp_in_parallel(cluster_index, 'sudo rm -rf /var/lib/search/index/')


def unpack_index(cluster_index, index_gen, blue_index):
    stop_report_on_hp(cluster_index)
    execute_command_on_hp_in_parallel(cluster_index,
                                      'echo -n | sudo tee /var/lib/search/marketsearch/current.generation')
    if blue_index:
        clean_current_index(cluster_index)

    blue_command = '--blue' if blue_index else ''
    execute_script_on_hp_cluster(
        cluster_index,
        'remote_unpack_index.py {0} {1}'.format(blue_command, index_gen))

    if not blue_index:
        snippet_host_name = get_hp_snippet_host_name(cluster_index)
        execute_command_on_host(snippet_host_name,
                                'echo -n | sudo tee /var/lib/search/snippet_index/download/current.generation')
        execute_script_on_host(
            snippet_host_name,
            'remote_unpack_index.py {0} --snippet-report'.format(index_gen))
        current_index_gen = get_current_index_gen(cluster_index, blue_index)
        if current_index_gen is None or current_index_gen != index_gen:
            raise Exception('Unexpected index generation: real {0}, expected {1}'.format(current_index_gen, index_gen))


def extract_original_revision_from_patch(patch_file_name):
    matcher = re.compile(r'^--- [^\t]+\t\(revision (\d+)\)$')
    with open(patch_file_name, 'r') as f:
        for line in f:
            match = matcher.match(line)
            if match is not None:
                return int(match.group(1))
    return None


def apply_svn_patch(svn_dir, patch_file_name):
    svn_patch_cmd = ['svn', 'patch', os.path.abspath(patch_file_name)]
    execute_command(svn_patch_cmd, svn_dir)


def set_executable_flag(file_name):
    file_stat = os.stat(file_name)
    os.chmod(file_name, file_stat.st_mode | stat.S_IEXEC)


def copy_bin_file_from_resource_dir_to_build_dir(svn_dir, resource_filename, build_path_suffix, build_filename, resource_dir):
    build_dir_path = os.path.join(svn_dir, 'market', 'report', build_path_suffix)
    if not os.path.exists(build_dir_path):
        os.makedirs(build_dir_path)
    build_file_path = os.path.join(build_dir_path, build_filename)
    resource_file_path = os.path.join(resource_dir, 'bin', resource_filename)
    if os.path.exists(build_file_path):
        os.remove(build_file_path)
    shutil.copyfile(resource_file_path, build_file_path)
    set_executable_flag(build_file_path)


def try_get_report_from_sandbox_resource(config):
    temp_dir = tempfile.mkdtemp()
    try:
        report_info = get_report_info_by_revision(config.svn_revision)
        report_skynet_id, file_name = report_info
        if report_info is None:
            print ">>> No such built report with svn revision {} in sandbox resources".format(config.svn_revision)
            return False
        print '>>> Built resource has been successfully found by svn revision {}'.format(config.svn_revision)
        get_report_bins(report_skynet_id, temp_dir, file_name)
        copy_bin_file_from_resource_dir_to_build_dir(
            svn_dir=config.svn_dir,
            resource_filename='report',
            build_path_suffix='report_bin',
            build_filename='report_bin',
            resource_dir=temp_dir
        )
        copy_bin_file_from_resource_dir_to_build_dir(
            svn_dir=config.svn_dir,
            resource_filename='snippet_report',
            build_path_suffix='basesearch',
            build_filename='basesearch',
            resource_dir=temp_dir
        )
        copy_bin_file_from_resource_dir_to_build_dir(
            svn_dir=config.svn_dir,
            resource_filename='report_ctl',
            build_path_suffix=os.path.join('runtime_cloud', 'report_ctl'),
            build_filename='report_ctl',
            resource_dir=temp_dir
        )
    except Exception:
        print ">>> Cann't get resource info by svn revision because of exception:"
        traceback.format_exc()
        return False
    finally:
        shutil.rmtree(temp_dir)
    return True


def perform_load_test(config, initial_index_gen):
    save_dir = None
    if config.artifacts_dir is not None:
        save_dir = os.path.join(config.artifacts_dir, config.session, str(config.index))
        create_directory(save_dir)

    try:
        export_config_to_ini_file(config, save_dir)
        if (initial_index_gen is not None and config.index_gen is not None and
                get_current_index_gen(config.cluster, is_blue_test(config)) != config.index_gen):
            unpack_index(config.cluster, config.index_gen, is_blue_test(config))
        if config.svn_patch is not None:
            shutil.copyfile(config.svn_patch, os.path.join(save_dir, os.path.basename(config.svn_patch)))

        build_type = get_build_type(config)
        revision_descr = get_revision_descr(config)
        if config.use_git:
            build_report_from_git_branch(
                config.git_dir,
                config.svn_dir,
                config.git_branch,
                build_type,
                config.dist_build,
                config.extra_build_options,
                revision_descr)
        else:
            if True or config.force_build or build_type != BUILD_RELEASE or not try_get_report_from_sandbox_resource(config):
                if config.force_build:
                    print ">>> Start building report because of --force-build"
                elif build_type != BUILD_RELEASE:
                    print ">>> Start building report because build type is not release"
                build_custom_report(
                    config.svn_dir,
                    config.svn_revision,
                    config.svn_branch,
                    config.svn_patch,
                    build_type,
                    config.dist_build,
                    config.extra_build_options,
                    revision_descr
                )
            else:
                print ">>> Report bin files have been successfully received from sandbox resource"

        first_report_type = True
        for report_type in REPORT_TYPES:
            if get_test_count(config, report_type):
                generate_config(config.cluster, config.svn_dir, report_type, config.enable_docfetcher, config.report_port)
                if first_report_type:
                    first_report_type = False
                    install_custom_report_on_hp(config.cluster, config.svn_dir, build_type, report_type, config.report_port)
                else:
                    restart_report_on_hp(config.cluster)
                    wait_for_report_to_start_on_hp(config.cluster, build_type, config.report_port)
                fire_at_hp(report_type=report_type, save_dir=save_dir, config=config)
    except:
        save_exception(save_dir)
        raise


def get_startrek_token():
    tokens_path = os.path.expanduser('~/.robot-market-st.tokens')
    if not os.path.exists(tokens_path):
        raise Exception('Can\'t post report to StarTrek without token file: {0}'.format(tokens_path))
    with open(tokens_path) as tokens_file:
        tokens = json.loads(tokens_file.read())
        return tokens['startrek']


def post_report_to_startrek(report, ticket, auth_token):
    print '>>> Sending report to StarTrek ticket {0}'.format(ticket)
    body = {'text': '%%{0}%%'.format(report)}
    headers = {'Authorization': 'OAuth {0}'.format(auth_token),
               'Content-Type': 'application/json'}
    conn = httplib.HTTPSConnection('st-api.yandex-team.ru')
    conn.request('POST', '/v2/issues/{0}/comments'.format(ticket), json.dumps(body), headers)
    response = conn.getresponse()
    success = response.status // 100 == 2
    if not success:
        print '>>> StarTrek replied with error: {0} {1}'.format(response.status, response.reason)
    data = response.read()
    if not success:
        print data
    conn.close()


def send_completion_notification(svn_dir):
    try:
        execute_command([path_to_ya(svn_dir), 'notify'], svn_dir)
    except KeyboardInterrupt:
        raise
    except:
        pass


def generate_report(config, startrek_token):
    report = generate_report_from_artifacts(config.artifacts_dir, config.session)
    text_report = strip_esc_codes(report)
    try:
        post_report_to_startrek(text_report, config.ticket, startrek_token)
    finally:
        print
        if sys.stdout.isatty():
            print report
        else:
            print text_report


def perform_series_of_tests(config, config_generator, initial_index_gen):
    def signal_handler(_signal, _frame):
        signal.signal(signal.SIGINT, signal.SIG_IGN)
        if sys.stdout.isatty():
            sys.stdout.write(create_warning('Execution interrupted! Wait for cleanup to complete.'))
        raise KeyboardInterrupt

    startrek_token = get_startrek_token()
    load_test_config = None
    interrupted = False
    signal.signal(signal.SIGINT, signal_handler)
    try:
        print '>>> Acquiring report lock for HP'
        report_lock = create_report_lock(config.cluster)
        report_lock.acquire()
        try:
            test_index = get_first_test_index(config)
            stop_services_on_hp(config.cluster)
            try:
                kill_stale_agents(config.cluster)
                while True:
                    try:
                        load_test_config = config_generator.next()
                        load_test_config.index = test_index
                        test_index += 1
                        perform_load_test(load_test_config, initial_index_gen)
                    except StopIteration:
                        break
                    except KeyboardInterrupt:
                        interrupted = True
                        break
                    except:
                        print traceback.format_exc()
                if not interrupted:
                    send_completion_notification(load_test_config.svn_dir)
                try:
                    current_index_gen = get_current_index_gen(config.cluster, is_blue_test(config))
                except Exception:
                    print traceback.format_exc()
                    current_index_gen = None
                if initial_index_gen is not None and initial_index_gen != current_index_gen:
                    unpack_index(config.cluster, initial_index_gen, is_blue_test(config))
                # uninstall_custom_report_on_hp(config.cluster)
            finally:
                start_services_on_hp(config.cluster)
        finally:
            print '>>> Releasing report lock for HP'
            report_lock.release()
    finally:
        signal.signal(signal.SIGINT, signal.SIG_DFL)
    if not interrupted:
        generate_report(load_test_config, startrek_token)


# Return use_git, git_branch, svn_revision, svn_branch
def parse_revision(revision, git_dir):
    revision_match = re.match(r'^(\d+)$', revision)
    if revision_match:
        # SVN revision number
        return False, None, int(revision), None
    elif '/' not in revision and git_dir is not None:
        # GIT branch name
        return True, revision, None, None
    elif ',' in revision:
        # SVN branch name and revision
        branch, rev = revision.split(',')
        return False, None, int(rev), branch
    else:
        # SVN branch name
        return False, None, None, revision


def check_i_am_not_run_from(dir_path):
    if os.path.realpath(__file__).startswith(os.path.realpath(dir_path)):
        raise Exception('Do not run this script from SVN working copy specified in --svn-dir. It may produce problems when switching to different source code revision.')


def _sanity_check(config, revisions, patch_list, index_gen_list, current_index_gen, ammo_list, config_file_path):
    print '>>> Performing sanity check'

    if current_index_gen is None:
        raise Exception('Current index generation cannot be determined')

    if config.svn_dir is None or not os.path.isdir(os.path.join(config.svn_dir, '.svn')):
        raise Exception('--svn-dir parameter is required and must point to the root of SVN working copy. Use "ya clone --no-junk <svn_dir>" to create one.')

    if not os.path.isfile(os.path.join(config.svn_dir, 'ya')):
        raise Exception('ya utility is not found inside of SVN directory. Use "ya clone --no-junk <svn_dir>" to create directory with SVN working copy.')

    check_i_am_not_run_from(config.svn_dir)

    if config_file_path is not None and os.path.realpath(config_file_path).startswith(os.path.realpath(config.svn_dir)):
        raise Exception('Do not use configuration files from SVN working copy specified in --svn-dir. It may produce problems when switching to different source code revision.')

    if not revisions and not patch_list:
        raise Exception('No source code revision specified (use at least one revision or --patch option)')

    index_type_count = collections.defaultdict(int)
    for report_type in REPORT_TYPES:
        index_type = "blue" if report_type in (REPORT_BLUE_MAIN, REPORT_BLUE_API) else "white"
        if get_test_count(config, report_type):
            index_type_count[index_type] += 1
    if index_type_count["blue"] and index_type_count["white"]:
        raise Exception('It is impossible to fire at different color Report at the same time.')

    report_type_count = sum(index_type_count.itervalues())
    if report_type_count == 0:
        raise Exception('At least one test count option must be non-zero.')

    for ammo in ammo_list:
        if re.search(r'[0-9]{8}', ammo) is None:
            raise Exception('Ammo path path must contain date in YYYYMMDD format.')
        ls_cmd = 'ls'
        if re.match(AMMO_DATE_RE, ammo):
            for n in xrange(CLUSTER_SIZE):
                for report_type in REPORT_TYPES:
                    if get_test_count(config, report_type):
                        if is_blue_test(config) and n > 0:
                            continue
                        ls_cmd += ' ' + os.path.join(DEFAULT_AMMO_DIR[report_type], ammo, '{0}{1}.log.gz'.format(AMMO_PREFIXES[report_type], n + 1))
        else:
            if report_type_count > 1:
                raise Exception('Custom ammo path specified but several report types are tested.')
            for n in xrange(CLUSTER_SIZE):
                ls_cmd += ' {0}{1}.log.gz'.format(ammo, n + 1)
        try:
            execute_command_on_host(get_tank_host(config.cluster), ls_cmd, get_output=True)
        except:
            raise Exception('Specified ammo path is not available: {0}'.format(ammo))

    ls_cmd = 'ls'
    for index_gen in index_gen_list:
        ls_cmd += ' /var/lib/search/marketsearch/{0}'.format(index_gen)
    try:
        execute_command_on_host(get_hp_host_name(config.cluster, 0), ls_cmd, get_output=True)
    except:
        raise Exception('Specified index generation is not available on HP cluster.')
    ls_cmd = 'ls /var/lib/search/marketsearch/{0}'.format(current_index_gen)
    try:
        execute_command_on_host(get_hp_host_name(config.cluster, 0), ls_cmd, get_output=True)
    except:
        raise Exception('Current index generation {0} is not present in /var/lib/search/marketsearch on HP cluster. It won\'t be possible to restore it.'.format(current_index_gen))

    try:
        for revision in revisions:
            use_git, _, svn_revision, svn_branch = parse_revision(revision, config.git_dir)
            if not use_git:
                # '--depth empty' указано для быстрой проверки возможности переключения на указанную ветку
                test_cmd = ['svn', 'switch', '--depth', 'empty']
                if svn_revision is not None:
                    test_cmd.extend(['-r', str(svn_revision)])
                test_cmd.append(get_svn_branch(svn_branch))
                execute_command(test_cmd, config.svn_dir, get_output=True)
    except:
        raise Exception('{0} does not appear to be a valid SVN revision'.format(revision))

    for patch in patch_list:
        svn_revision = extract_original_revision_from_patch(patch)
        if svn_revision is None:
            raise Exception('Failed to extract revision number from {0}. Was it created with "svn diff"?'.format(patch))
        try:
            switch_svn(config.svn_dir, get_svn_branch(None), svn_revision)
        except:
            raise Exception('{0} does not appear to be valid SVN revision'.format(svn_revision))
        try:
            apply_svn_patch(config.svn_dir, patch)
        except:
            raise Exception('Patch {0} failed to apply'.format(patch))

    # Проверка доступа - заходим на Танк и запускаем с него команду на HP
    try:
        hostname_cmd = turn_command_into_string(create_remote_command(get_hp_host_name(config.cluster, 0), 'hostname'))
        execute_command_on_host(get_tank_host(config.cluster), hostname_cmd)
    except:
        traceback.print_exc()
        raise Exception(
            'Problem with running remote commands. Make sure to use agent forwarding when connecting to remote server (ssh -A). '
            'Copy .ssh keys to remote machines (Dev and Tank) if you use Tmux or Screen.')


def _auto_perf_test():
    arg_parser = argparse.ArgumentParser(description='Automated load testing for Market Report. https://wiki.yandex-team.ru/Market/Development/Report/loadtesting/',
                                         formatter_class=argparse.RawTextHelpFormatter)
    # При объявлении аргументов нельзя использовать default и action=store_true,
    # это ломает механизм загрузки значений опций из файла конфигурации.
    arg_parser.add_argument('--config', help='Path to configuration file')
    arg_parser.add_argument('--session', help='Existing session GUID to use, can be used to append data to existing report')
    arg_parser.add_argument('--cluster', type=int, choices=ALLOWED_HP_CLUSTERS, default=ALLOWED_HP_CLUSTERS[0], help='Zero-based index of HP cluster')
    arg_parser.add_argument('--artifacts-dir', help='Path to store generated files (logs, flame graphs)')
    arg_parser.add_argument('--svn-dir', help='Path to SVN working copy. It will be used to checkout source code and build Report binary.')
    arg_parser.add_argument('--git-dir', help='Path to GIT repository, required to apply GIT branches')
    arg_parser.add_argument(
        '--no-dist-build', action='store_const', const=False, dest='dist_build',
        help='Distributed build is generally faster. You may want to disable it if you have everything already cached.')
    arg_parser.add_argument('--extra-build-options', help='Additional options for ya make')
    arg_parser.add_argument('--ticket', help='Lunapark ticket name like MARKETOUT-11539')
    arg_parser.add_argument('--ammo', action='append',
                            help='Ammo from tank0[12]ht (e.g. 20170502). See /home/lunapark/mainreport/ammo/\n'
                                 'This can also be full path with prefix like /home/lunapark/mainreport/ammo/20170514/main')
    arg_parser.add_argument('--index-gen', action='append',
                            help='Index to install before running tests (e.g. 20170922_0209).\n'
                                 'Look for available indices in /var/lib/search/marketsearch on any machine from HP cluster.')
    for report_type in REPORT_TYPES:
        report_name = REPORT_NAMES[report_type].replace('_', '-')
        arg_parser.add_argument(
            '--save-{report_name}-logs'.format(report_name=report_name),
            action='store_const', const=True,
            dest='{report_name}_save_logs'.format(report_name=report_name),
            help='Save {report_name} Report logs for each test'.format(report_name=report_name))
        arg_parser.add_argument(
            '--no-{report_name}-warmup'.format(report_name=report_name),
            action='store_const', const=False,
            dest='{report_name}_warmup'.format(report_name=report_name),
            help='Do not perform warm-up stage for {report_name}'.format(report_name=report_name))
        arg_parser.add_argument(
            '--{report_name}-test-count'.format(report_name=report_name),
            type=int,
            help='Number of times to test {report_name}, default: {0}'.format(DEFAULT_TEST_COUNT[report_type], report_name=report_name))
        arg_parser.add_argument(
            '--{report_name}-warmup-rps-sched'.format(report_name=report_name),
            help='Warm up RPS schedule for {report_name}, default: {0}'.format(DEFAULT_WARMUP_RPS_SCHED[report_type], report_name=report_name))
        if report_type == REPORT_MAIN:
            extra_help = (
                '\nSee https://yandextank.readthedocs.io/en/latest/tutorial.html\n'
                'Examples:\n'
                '\tstep(5, 25, 5, 60) - stepped load from 5 to 25 rps, with 5 rps steps, step duration 60s\n'
                '\tline(1, 10, 10m) - linear load from 1 to 10 rps, duration - 10 minutes\n'
                '\tconst(10,10m) - constant load for 10 rps for 10 minutes\n'
                'You can set fractional load like this: line(1.1, 2.5, 10) - from 1.1rps to 2.5 for 10 seconds.\n'
                'You can specify complex load schemes using those primitives: line(1, 10, 10m) const(10,10m)\n'
            )
        else:
            extra_help = ''
        arg_parser.add_argument(
            '--{report_name}-rps-sched'.format(report_name=report_name),
            help='RPS schedule for {report_name}, default: {0}{extra_help}'.format(DEFAULT_RPS_SCHED[report_type], report_name=report_name, extra_help=extra_help))
    arg_parser.add_argument('--save-logs', action='store_const', const=True, help='Save Report logs for each test')
    arg_parser.add_argument('--record-perf', action='store_const', const=True, help='Run perf profiler along with test')
    arg_parser.add_argument('--perf-stat', action='store_const', const=True, help='Run perf stat along with test')
    arg_parser.add_argument('--perf-executable-path', help='Path to perf executable on target host, default: {0}'.format(DEFAULT_PERF_EXECUTABLE_PATH))
    arg_parser.add_argument('--flame-graph-path', help='Path to Flame Graph script directory of target host, default: {0}'.format(DEFAULT_FLAME_GRAPH_PATH))
    arg_parser.add_argument('--perf-host', type=int, help='Zero based index of cluster host to run perf on ({0} for snippet machine), default: {1}'.format(CLUSTER_SIZE, DEFAULT_PERF_HOST_INDEX))
    arg_parser.add_argument('--perf-record-time', type=int, help='Perf recording duration in seconds, default: {0}'.format(DEFAULT_PERF_RECORD_TIME))
    arg_parser.add_argument('--perf-record-delay', type=int, help='Perf recording delay in seconds.')
    arg_parser.add_argument(
        '--perf-events',
        help='Comma separated list of perf events to collect. Example: task-clock,context-switches,cpu-migrations,page-faults,cpu-cycles,'
        'instructions,branch-instructions,branch-misses,cache-misses,cache-references,dTLB-load-misses,dTLB-loads,dTLB-store-misses,dTLB-stores,iTLB-load-misses,'
        'iTLB-loads,node-load-misses,node-loads,node-store-misses,node-stores')
    arg_parser.add_argument('--perf-flags', help='Additional perf flags.')
    arg_parser.add_argument('--execution-stats', action='store_const', const=True, help='Enable collection of execution stats in Report')
    arg_parser.add_argument('--patch', action='append', help='This will apply patch created by "svn diff" and run tests for both original and patched versions of source code.')
    arg_parser.add_argument(
        'revisions', nargs='*', default=[],
        help='List of source code revisions to test. Can be:\n'
             'SVN revision: 2793588\n'
             'SVN branch: ^/branches/market/report-2017.1.72/arcadia\n'
             'SVN branch and revision: ^/branches/market/report-2017.1.72/arcadia,2793588\n'
             'GIT branch: my_feature_branch\n')
    arg_parser.add_argument('--force-build', action='store_const', const=True, help='Build report in spite of having related sandbox resource', default=False)
    arg_parser.epilog = 'Examples:\n' \
        '* Test a list of SVN revisions:\n' \
        '\t{0}.py --svn-dir=~/svn --ticket=MARKETOUT-11539 --ammo=20170502 2793588 2793871 2794547\n' \
        '* Test two SVN branches:\n' \
        '\t{0}.py --svn-dir=~/svn --ticket=MARKETOUT-11539 --ammo=20170502 ^/branches/market/report-2017.1.71/arcadia ^/branches/market/report-2017.1.72/arcadia\n' \
        '* Record perf profile:\n' \
        '\t{0}.py --config=~/perf.cfg --ticket=MARKETOUT-11539 ^/branches/junk/user/feature\n' \
        'perf.cfg contents:\n' \
        '\t[config]\n' \
        '\tartifacts_dir = ~/auto_perf_test\n' \
        '\tsvn_dir = ~/svn\n' \
        '\tammo = 20170502\n' \
        '\tmain_test_count = 1\n' \
        '\tparallel_test_count = 1\n' \
        '\tmain_rps_sched = const(10,2m)\n' \
        '\tparallel_rps_sched = const(50,2m)\n' \
        '\tmain_warmup_rps_sched = %(main_rps_sched)s\n' \
        '\tparallel_warmup_rps_sched = %(parallel_rps_sched)s\n' \
        '\trecord_perf = True\n' \
        '\tperf_executable_path = ~/perf\n' \
        '\tflame_graph_path = ~/FlameGraph\n' \
        '\tperf_host = 0\n' \
        '\tperf_record_time = 60'.format(get_module_name())
    args = arg_parser.parse_args()

    config = LoadTestConfig()
    config_file_path = None
    if args.config is not None:
        config_file_path = expand_path(args.config)
        if not os.path.isfile(config_file_path):
            config_file_path = os.path.join(os.path.dirname(__file__), config_file_path)
        if not os.path.isfile(config_file_path):
            raise Exception('Unable to find ' + args.config)
        config_parser = ConfigParser.ConfigParser()
        if not config_parser.read(config_file_path):
            raise Exception('Unable to parse ' + args.config)

        CONFIG_SECTION = 'config'

        def get_config_option(name, getter=''):
            if config_parser.has_option(CONFIG_SECTION, name):
                setattr(config, name, getattr(config_parser, 'get' + getter)(CONFIG_SECTION, name))

        get_config_option('session')
        get_config_option('cluster', 'int')
        get_config_option('artifacts_dir')
        get_config_option('svn_dir')
        get_config_option('git_dir')
        get_config_option('dist_build', 'boolean')
        get_config_option('extra_build_options')
        get_config_option('ticket')
        for report_name in REPORT_NAMES:
            get_config_option('{report_name}_save_logs'.format(report_name=report_name), 'boolean')
            get_config_option('{report_name}_warmup'.format(report_name=report_name), 'boolean')
            get_config_option('{report_name}_test_count'.format(report_name=report_name), 'int')
            get_config_option('{report_name}_warmup_rps_sched'.format(report_name=report_name))
            get_config_option('{report_name}_rps_sched'.format(report_name=report_name))
        get_config_option('save_logs', 'boolean')
        get_config_option('record_perf', 'boolean')
        get_config_option('perf_stat', 'boolean')
        get_config_option('perf_executable_path')
        get_config_option('flame_graph_path')
        get_config_option('perf_host', 'int')
        get_config_option('perf_record_time', 'int')
        get_config_option('perf_record_delay', 'int')
        get_config_option('perf_events')
        get_config_option('perf_flags')
        get_config_option('execution_stats', 'boolean')
        get_config_option('force_build', 'boolean')

    def get_argument(name):
        if not hasattr(args, name):
            return
        value = getattr(args, name)
        if value is not None:
            setattr(config, name, value)

    get_argument('session')
    get_argument('cluster')
    get_argument('artifacts_dir')
    get_argument('svn_dir')
    get_argument('git_dir')
    get_argument('dist_build')
    get_argument('extra_build_options')
    get_argument('ticket')
    for report_name in REPORT_NAMES:
        get_argument('{report_name}_save_logs'.format(report_name=report_name))
        get_argument('{report_name}_warmup'.format(report_name=report_name))
        get_argument('{report_name}_test_count'.format(report_name=report_name))
        get_argument('{report_name}_warmup_rps_sched'.format(report_name=report_name))
        get_argument('{report_name}_rps_sched'.format(report_name=report_name))
    get_argument('save_logs')
    get_argument('record_perf')
    get_argument('perf_executable_path')
    get_argument('flame_graph_path')
    get_argument('perf_host')
    get_argument('perf_record_time')
    get_argument('perf_record_delay')
    get_argument('perf_events')
    get_argument('perf_flags')
    get_argument('execution_stats')
    get_argument('force_build')

    config.artifacts_dir = expand_path(config.artifacts_dir)
    config.svn_dir = expand_path(config.svn_dir)
    config.git_dir = expand_path(config.git_dir)
    if config.ammo is not None and not re.match(AMMO_DATE_RE, config.ammo):
        config.ammo = expand_path(config.ammo)
    if config.ticket is None:
        config.ticket = DEFAULT_TICKETS[config.cluster]
    if config.perf_executable_path is None:
        config.perf_executable_path = expand_path(DEFAULT_PERF_EXECUTABLE_PATH)
    if config.flame_graph_path is None:
        config.flame_graph_path = expand_path(DEFAULT_FLAME_GRAPH_PATH)

    def get_list_arg(name):
        if hasattr(args, name):
            value = getattr(args, name)
            if value is not None:
                return value
        if args.config is not None and config_parser.has_option(CONFIG_SECTION, name):
            return config_parser.get(CONFIG_SECTION, name).split(',')
        return list()

    index_gen_list = get_list_arg('index_gen')
    current_index_gen = get_current_index_gen(config.cluster, is_blue_test(config))

    if not index_gen_list:
        initial_index_gen = None
        index_gen_list.append(current_index_gen)
    else:
        initial_index_gen = current_index_gen

    ammo_list = get_list_arg('ammo')
    if not ammo_list:
        ammo_list.append(get_recent_ammo(config.cluster))

    patch_list = [expand_path(patch) for patch in get_list_arg('patch')]

    def config_generator():
        for index_gen in index_gen_list:
            config.index_gen = index_gen
            for ammo in ammo_list:
                config.ammo = ammo
                config.svn_patch = None
                for revision in args.revisions:
                    config.use_git, config.git_branch, config.svn_revision, config.svn_branch = parse_revision(revision, config.git_dir)
                    yield config
                config.use_git, config.git_branch, config.svn_branch = False, None, None
                for patch in patch_list:
                    config.svn_revision = extract_original_revision_from_patch(patch)
                    config.svn_patch = None
                    yield config
                    config.svn_patch = patch
                    yield config

    _sanity_check(config, args.revisions, patch_list, index_gen_list, current_index_gen, ammo_list, config_file_path)
    perform_series_of_tests(config, config_generator(), initial_index_gen)


if __name__ == "__main__":
    _auto_perf_test()
