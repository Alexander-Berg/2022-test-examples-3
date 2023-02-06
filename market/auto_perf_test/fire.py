#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
import glob
import re
import shutil
import sys
import tempfile
import threading
import time
import traceback

from config import *
from constants import *
from lock import create_report_lock
from remote import *
from report import generate_report_from_artifacts
from utils import *


REPORT_VERSION_INFO = 'report_version_info'


def get_report_pids_from_hp(cluster_index, blue_report):
    pids = execute_command_on_hp_in_parallel(
        cluster_index,
        GET_REPORT_PID_COMMAND,
        snippet_command=(GET_REPORT_PID_COMMAND if not blue_report else None),
        get_output=True
    )
    for host_index in pids:
        pids[host_index] = pids[host_index].strip()
    return pids


def prepare_tank_config(config, report_type, revision_descr, is_warmup):
    CONFIG_TEMPLATE = '''
core:
  artifacts_base_dir: yandex-tank-artifacts

uploader:
  job_dsc: {job_dsc}
  job_name: {job_name}
  meta: {{multitag: True}}
  task: {task}

telegraf:
  enabled: {monitoring_enabled}
  config: {monitoring_config}
  kill_old: True

console:
  short_only: True

phantom:
  address: "[{host_name}.market.yandex.net]:{port}"
  ammofile: {ammo_path_prefix}{ammo_index}.log.gz
  ammo_type: phantom
  cache_dir: /var/lib/tankapi/tests/stpd-cache
  instances: 500
  phantom_http_entity: 1024M
  load_profile:
    load_type: rps
    schedule: {rps_schedule}
  timeout: 60s
  writelog: proto_warning
  multi:
'''

    PHANTOM_SECTION_TEMPLATE = '''
    - address: "[{host_name}.market.yandex.net]:{port}"
      ammofile: {ammo_path_prefix}{ammo_index}.log.gz
      ammo_type: phantom
      cache_dir: /var/lib/tankapi/tests/stpd-cache
      instances: 500
      phantom_http_entity: 1024M
      load_profile:
        load_type: rps
        schedule: {rps_schedule}
      timeout: 60s
      writelog: proto_warning
'''

    MONITORING_SECTION_TEMPLATE = '''
    <Host address="{host_name}.market.yandex.net">
        <CPU/>
        <System/>
        <Memory/>
        <Disk devices='["md0","md1","md2","md4","md127"]'/>
        <Net interfaces='["eth0","lo"]'/>
        <Netstat/>
        <Nstat/>
        <Kernel/>
        <KernelVmstat/>
    </Host>
'''
    monitoring_host_list = list()
    for host_index in range(CLUSTER_SIZE):
        monitoring_host_list.append(get_hp_host_name(config.cluster, host_index))
    if not is_blue(report_type):
        monitoring_host_list.append(get_hp_snippet_host_name(config.cluster))
    monitoring_host_list.append(get_tank_host(config.cluster))
    monitoring_host_list = list(sorted(set(monitoring_host_list)))
    monitoring_config = '<Monitoring>\n'
    for host_name in monitoring_host_list:
        monitoring_config += MONITORING_SECTION_TEMPLATE.format(host_name=host_name)
    monitoring_config += '</Monitoring>\n'

    if not re.match(AMMO_DATE_RE, config.ammo):
        ammo_prefix = config.ammo
    else:
        ammo_prefix = os.path.join(DEFAULT_AMMO_DIR[report_type], config.ammo, AMMO_PREFIXES[report_type])

    if is_warmup:
        rps_sched = get_warmup_rps_sched(config, report_type)
    else:
        rps_sched = get_rps_sched(config, report_type)

    config_type = REPORT_NAMES[report_type]
    job_name = config_type
    if is_warmup:
        job_name += ' warmup'
    job_name += ' ' + config.session
    job_dsc = config_type
    job_dsc += ' ' + revision_descr
    job_dsc += ' index:' + config.index_gen
    job_dsc += ' ammo:' + config.ammo
    job_dsc += ' rps:' + rps_sched

    tank_config = CONFIG_TEMPLATE.format(
        job_dsc=job_dsc,
        job_name=job_name,
        task=config.ticket,
        monitoring_enabled=config.tank_monitoring,
        monitoring_config=os.path.join(get_remote_temp_dir(), 'monitoring.xml'),
        host_name=get_hp_host_name(config.cluster, 0),
        port=config.report_port,
        ammo_path_prefix=ammo_prefix,
        ammo_index=1,
        rps_schedule=rps_sched
    )
    for host_index in range(1, CLUSTER_SIZE):
        tank_config += PHANTOM_SECTION_TEMPLATE.format(
            host_name=get_hp_host_name(config.cluster, host_index),
            port=config.report_port,
            ammo_path_prefix=ammo_prefix,
            ammo_index=(host_index + 1 if not is_blue(report_type) else 1),
            rps_schedule=rps_sched
        )

    create_temp_dir_on_host(get_tank_host(config.cluster))
    temp_dir = tempfile.mkdtemp()
    try:
        monitoring_xml_path = os.path.join(temp_dir, 'monitoring.xml')
        tank_conf_path = os.path.join(temp_dir, 'tank.conf')
        with open(monitoring_xml_path, 'w') as f:
            f.write(monitoring_config)
        with open(tank_conf_path, 'w') as f:
            f.write(tank_config)
        copy_files_to_host(
            get_tank_host(config.cluster),
            [monitoring_xml_path, tank_conf_path],
            get_remote_temp_dir())
    finally:
        shutil.rmtree(temp_dir)


class PerfRecorderThread(threading.Thread):

    def __init__(self, perf_recorder, delay=0):
        self.perf_recorder = perf_recorder
        self.delay = delay
        self.perf_recorder_exception = None
        threading.Thread.__init__(self)

    def run(self):
        if self.delay:
            time.sleep(self.delay)
        try:
            self.perf_recorder()
        except:
            self.perf_recorder_exception = sys.exc_info()


def run_yandex_tank(tank_host, perf_recorder=None, perf_record_delay=0):
    tank_artifacts_dir_matcher = re.compile(r'.+Artifacts dir: (.+)')
    web_link_matcher = re.compile(r'.+Web link: (.+)')
    tank_command = 'yandex-tank -c {0}'.format(
        os.path.join(get_remote_temp_dir(), 'tank.conf'))
    process = start_command_on_host(tank_host, tank_command, terminal=True)
    perf_recorder_was_started = False
    interrupted = False
    tank_artifacts_dir = None
    web_link = None
    perf_recorder_thread = None
    while True:
        line = process.stdout.readline()
        if not line:
            break
        if tank_artifacts_dir is None:
            tank_artifacts_dir_match = tank_artifacts_dir_matcher.match(line)
            if tank_artifacts_dir_match:
                tank_artifacts_dir = tank_artifacts_dir_match.group(1).strip(CTRL_CHARS)
                if not perf_recorder_was_started:
                    perf_recorder_was_started = True
                    if perf_recorder is not None:
                        perf_recorder_thread = PerfRecorderThread(perf_recorder, perf_record_delay)
                        perf_recorder_thread.start()
        if web_link is None:
            web_link_match = web_link_matcher.match(line)
            if web_link_match:
                web_link = web_link_match.group(1).strip(CTRL_CHARS)
        sys.stdout.write(line)
        if 'Do not press Ctrl+C again, the test will be broken otherwise' in line:
            interrupted = True
    ret_code = process.wait()
    if perf_recorder_thread is not None:
        perf_recorder_thread.join()
    if interrupted:
        raise KeyboardInterrupt
    if ret_code != 0:
        raise Exception('Got non-zero return code from Tank: {0}'.format(ret_code))
    if perf_recorder_thread is not None and perf_recorder_thread.perf_recorder_exception is not None:
        exc = perf_recorder_thread.perf_recorder_exception
        raise exc[0], exc[1], exc[2]
    if perf_recorder is not None and not perf_recorder_was_started:
        raise Exception('Unable to detect start of the benchmark')
    return tank_artifacts_dir, web_link


def save_report_version_info(cluster_index, save_dir, report_port):
    def _save_version_info(cluster_index, url_query, save_dir, save_file_name):
        version_info_dict = execute_command_on_hp_in_parallel(
            cluster_index,
            'curl -s http://localhost:{0}/yandsearch?{1}'.format(report_port, url_query),
            get_output=True)
        for host_index, version_info in version_info_dict.iteritems():
            file_dir = os.path.join(save_dir, get_hp_host_name(cluster_index, host_index))
            create_directory(file_dir)
            file_name = os.path.join(file_dir, save_file_name)
            with open(file_name, 'w') as f:
                f.write(version_info)

    _save_version_info(cluster_index, 'admin_action=versions', save_dir, REPORT_VERSION_INFO)
    _save_version_info(cluster_index, 'info=getversion', save_dir, 'report_version_info2')


def reset_logs_on_hp(cluster_index, report_type, port):
    log_paths = ' '.join(
        [os.path.join(LOG_DIR, log_name) for log_name in REPORT_LOG_FILE_NAMES[report_type]])
    execute_command_on_hp_in_parallel(cluster_index, 'sudo rm -f {0}'.format(log_paths))
    execute_command_on_hp_in_parallel(
        cluster_index,
        'curl -s "http://localhost:{0}/yandsearch?admin_action=flushlogs"'.format(port))


def fetch_logs_from_hp(cluster_index, report_type, save_dir):
    log_paths = [os.path.join(LOG_DIR, log_name) for log_name in REPORT_LOG_FILE_NAMES[report_type]]
    copy_files_from_hp_in_parallel(cluster_index, save_dir, log_paths)


def save_phantom_logs(tank_host, tank_artifacts_dir, save_dir):
    PHANTOM_LOGS = (
        ('phout_*.log', PHOUT_LOG),
        ('answ_*.log', 'answ.log')
    )
    for phantom_log_glob, phanton_log_name in PHANTOM_LOGS:
        copy_files_from_host(tank_host,
                             os.path.join(tank_artifacts_dir, phantom_log_glob),
                             save_dir,
                             append_host_to_dst=False)
        os.rename(glob.glob(os.path.join(save_dir, phantom_log_glob))[0],
                  os.path.join(save_dir, phanton_log_name))


def save_web_link(save_path, web_link):
    if save_path is None:
        return
    error_path = os.path.join(save_path, WEB_LINK_FILE)
    with open(error_path, 'w') as f:
        f.write(web_link)


def fetch_process_stats(cluster_index, pids, save_dir, report_type):
    try:
        PID_STAT_FILES = {'/proc/{pid}/status', '/proc/{pid}/stat', '/proc/{pid}/io', '/proc/{pid}/net/snmp'}

        copy_command_list = dict()
        for host_index, pid in pids.iteritems():
            copy_cmd = 'sudo cp --no-preserve mode'
            for pid_stat_file in PID_STAT_FILES:
                copy_cmd += ' ' + pid_stat_file.format(pid=pid)
            copy_cmd += ' ' + get_remote_temp_dir()
            copy_command_list[host_index] = copy_cmd
        execute_command_on_hp_in_parallel(cluster_index, copy_command_list)

        proc_stat_list = list()
        for pid_stat_file in PID_STAT_FILES:
            proc_stat_list.append(os.path.join(get_remote_temp_dir(), os.path.basename(pid_stat_file)))
        copy_files_from_hp_in_parallel(
            cluster_index,
            save_dir,
            proc_stat_list + ['/run/shm/report_global_stats'],
            snippet_file_list=None if is_blue(report_type) else proc_stat_list
        )
    except:
        print traceback.format_exc()


def save_report_config(cluster_index, save_path):
    copy_files_from_hp_in_parallel(
        cluster_index,
        save_path,
        '/etc/yandex/market-report-configs/generated/market-report.cfg',
        snippet_file_list='/etc/yandex/market-report-configs/generated/market-snippet-report.cfg'
    )


def get_report_pid(host_name):
    output = execute_command_on_host(host_name, GET_REPORT_PID_COMMAND, get_output=True)
    return int(output.strip())


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


class PerfRecorder(object):

    def __init__(self, report_host, perf_path, flame_graph_path, record_time, perf_events, perf_flags, save_path):
        self.report_host = report_host
        self.perf_path = perf_path
        self.flame_graph_path = flame_graph_path
        self.record_time = record_time
        self.save_path = save_path
        self.perf_events = perf_events
        self.perf_flags = perf_flags
        self.report_pid = get_report_pid(report_host)

    def __call__(self):
        perf_data_path = os.path.join(get_remote_temp_dir(), 'perf.data')
        perf_command = 'sudo ionice -c1 nice -n -20 {perf_path} record -p {pid} -g'.format(
            perf_path=self.perf_path, pid=self.report_pid)
        if self.perf_events is not None:
            perf_command += ' -e {events}'.format(events=self.perf_events)
        if self.perf_flags is not None:
            perf_command += ' ' + self.perf_flags
        perf_command += ' -o {output_path} -B -- sleep {record_time}'.format(
            output_path=perf_data_path, record_time=self.record_time)
        execute_command_on_host(self.report_host, perf_command)
        perf_folded_stacks_path = os.path.join(get_remote_temp_dir(), PERF_FOLDED_STACKS)
        stack_collapse_command = 'sudo {perf_path} script -i {input_path} | {flame_graph_script} > {output_path}'.format(
            perf_path=self.perf_path, input_path=perf_data_path,
            flame_graph_script=os.path.join(self.flame_graph_path, 'stackcollapse-perf.pl'),
            output_path=perf_folded_stacks_path)
        execute_command_on_host(self.report_host, stack_collapse_command)
        perf_svg_path = os.path.join(get_remote_temp_dir(), 'perf.svg')
        generate_perf_svg_comand = 'sudo cat {input_path} | {flame_graph_script} > {output_path}'.format(
            input_path=perf_folded_stacks_path,
            flame_graph_script=os.path.join(self.flame_graph_path, 'flamegraph.pl'),
            output_path=perf_svg_path)
        execute_command_on_host(self.report_host, generate_perf_svg_comand)
        copy_files_from_host(self.report_host, [perf_folded_stacks_path, perf_svg_path], self.save_path)


class PerfStatRecorder(object):

    def __init__(self, report_host, perf_path, record_time, perf_events, perf_flags, save_path):
        self.report_host = report_host
        self.perf_path = perf_path
        self.record_time = record_time
        self.perf_events = perf_events
        self.perf_flags = perf_flags
        self.save_path = save_path
        self.report_pid = get_report_pid(report_host)

    def __call__(self):
        perf_data_path = os.path.join(get_remote_temp_dir(), 'perf_stat')
        perf_command = 'sudo ionice -c1 nice -n -20 {perf_path} stat -p {pid}'.format(
            perf_path=self.perf_path, pid=self.report_pid)
        if self.perf_events is not None:
            perf_command += ' -e {events}'.format(events=self.perf_events)
        if self.perf_flags is not None:
            perf_command += ' ' + self.perf_flags
        perf_command += ' -o {output_path} -- sleep {record_time}'.format(
            output_path=perf_data_path, record_time=self.record_time)
        execute_command_on_host(self.report_host, perf_command)
        copy_files_from_host(self.report_host, [perf_data_path], self.save_path)


def extract_revision_info(config, test_save_dir):
    revision_info_path = os.path.join(test_save_dir, get_hp_host_name(config.cluster, 0), REPORT_VERSION_INFO)
    with open(revision_info_path, 'r') as f:
        revision_info_xml = f.read()
    revision_elem = ET.fromstring(revision_info_xml).find('report')
    if revision_elem is not None:
        return revision_elem.text
    return None


def fire_at_hp(report_type, save_dir, config):
    kill_stale_agents(config.cluster)

    repeat = get_test_count(config, report_type)
    perform_warmup = get_warmup(config, report_type)
    test_save_dir = os.path.join(save_dir, REPORT_NAMES[report_type])

    pids_at_start = get_report_pids_from_hp(config.cluster, is_blue(report_type))
    save_report_version_info(config.cluster, test_save_dir, config.report_port)
    revision_descr = get_revision_descr(config)
    if not revision_descr:
        revision_descr = extract_revision_info(config, test_save_dir)
    tank_host = get_tank_host(config.cluster)
    if perform_warmup:
        prepare_tank_config(config, report_type, revision_descr, is_warmup=True)
        run_yandex_tank(tank_host)
    if config.execution_stats:
        execute_command_on_hp_in_parallel(
            config.cluster,
            'curl -s "http://localhost:{0}/yandsearch?admin_action=execstats&enable=1"'.format(config.report_port))
    prepare_tank_config(config, report_type, revision_descr, is_warmup=False)
    for index in xrange(repeat):
        sub_test_save_dir = os.path.join(test_save_dir, str(index))
        create_directory(sub_test_save_dir)
        fetch_process_stats(config.cluster, pids_at_start, os.path.join(sub_test_save_dir, 'stats_before'), report_type)
        if config.save_logs or get_save_logs(config, report_type):
            reset_logs_on_hp(config.cluster, report_type, config.report_port)
        perf_recorder = None
        if config.perf_host is not None:
            if config.perf_host == CLUSTER_SIZE and not is_blue(report_type):
                perf_host = get_hp_snippet_host_name(config.cluster)
            else:
                perf_host = get_hp_host_name(config.cluster, config.perf_host)
        else:
            perf_host = get_hp_host_name(config.cluster, DEFAULT_PERF_HOST_INDEX)
        if config.record_perf or config.perf_stat:
            create_temp_dir_on_host(perf_host)
        if config.record_perf:
            perf_recorder = PerfRecorder(
                perf_host,
                config.perf_executable_path,
                config.flame_graph_path,
                config.perf_record_time,
                config.perf_events,
                config.perf_flags,
                sub_test_save_dir)
        if config.perf_stat:
            perf_recorder = PerfStatRecorder(
                perf_host,
                config.perf_executable_path,
                config.perf_record_time,
                config.perf_events,
                config.perf_flags,
                sub_test_save_dir)
        tank_artifacts_dir, web_link = run_yandex_tank(tank_host, perf_recorder, config.perf_record_delay)
        pids = get_report_pids_from_hp(config.cluster, is_blue(report_type))
        if pids != pids_at_start:
            raise Exception('Report PID changed during test\n{0}\n{1}'.format(pids_at_start, pids))
        if config.save_logs or get_save_logs(config, report_type):
            fetch_logs_from_hp(config.cluster, report_type, sub_test_save_dir)
        if tank_artifacts_dir:
            save_phantom_logs(tank_host, tank_artifacts_dir, sub_test_save_dir)
        if web_link:
            save_web_link(sub_test_save_dir, web_link)
        fetch_process_stats(config.cluster, pids, os.path.join(sub_test_save_dir, 'stats_after'), report_type)
    save_report_config(config.cluster, test_save_dir)


def save_exception(save_path):
    if save_path is None:
        return
    error_path = os.path.join(save_path, ERROR_FILE)
    with open(error_path, 'w') as f:
        f.write(traceback.format_exc())


def main():
    arg_parser = argparse.ArgumentParser(description='Run Yandex Tank with selected options', formatter_class=argparse.RawTextHelpFormatter)
    arg_parser.add_argument('--session', help='Existing session GUID to use, can be used to append data to existing report')
    arg_parser.add_argument('--cluster', type=int, choices=ALLOWED_HP_CLUSTERS, default=ALLOWED_HP_CLUSTERS[0], help='Zero-based index of HP cluster')
    arg_parser.add_argument('--artifacts-dir', help='Path to store generated files (logs, flame graphs)')
    arg_parser.add_argument('--config-type', choices=REPORT_NAMES, required=True, help='Type of report')
    arg_parser.add_argument('--ticket', help='Lunapark ticket name like MARKETOUT-11539')
    arg_parser.add_argument('--ammo',
                            help='Ammo from tank0[12]ht (e.g. 20170502). See /home/lunapark/mainreport/ammo/\n'
                                 'This can also be full path with prefix like /home/lunapark/mainreport/ammo/20170514/main')
    arg_parser.add_argument(
        '--rps-sched',
        help='RPS schedule, see https://yandextank.readthedocs.io/en/latest/tutorial.html\n'
        'Examples:\n'
        '\tstep(5, 25, 5, 60) - stepped load from 5 to 25 rps, with 5 rps steps, step duration 60s\n'
        '\tline(1, 10, 10m) - linear load from 1 to 10 rps, duration - 10 minutes\n'
        '\tconst(10,10m) - constant load for 10 rps for 10 minutes\n'
        'You can set fractional load like this: line(1.1, 2.5, 10) - from 1.1rps to 2.5 for 10 seconds.\n'
        'You can specify complex load schemes using those primitives: line(1, 10, 10m) const(10,10m)\n')
    arg_parser.add_argument('--test-count', type=int, default=1, help='Number of times to run test, default: 1')
    arg_parser.add_argument('--save-logs', action='store_true', help='Save Report logs for each test')
    arg_parser.add_argument('--record-perf', action='store_true', help='Run perf profiler along with test')
    arg_parser.add_argument('--perf-stat', action='store_true', help='Run perf stat along with test')
    arg_parser.add_argument('--perf-host', type=int, help='Zero based index of cluster host to run perf on ({0} for snippet machine), default: {1}'.format(CLUSTER_SIZE, DEFAULT_PERF_HOST_INDEX))
    arg_parser.add_argument('--perf-record-time', type=int, help='Perf recording duration in seconds, default: {0}'.format(DEFAULT_PERF_RECORD_TIME))
    arg_parser.add_argument('--perf-record-delay', type=int, help='Perf recording delay in seconds, default: {0}.'.format(DEFAULT_PERF_RECORD_DELAY))
    arg_parser.add_argument(
        '--perf-events',
        help='Comma separated list of perf events to collect. Example: task-clock,context-switches,cpu-migrations,page-faults,cpu-cycles,'
        'instructions,branch-instructions,branch-misses,cache-misses,cache-references,dTLB-load-misses,dTLB-loads,dTLB-store-misses,dTLB-stores,iTLB-load-misses,'
        'iTLB-loads,node-load-misses,node-loads,node-store-misses,node-stores')
    arg_parser.add_argument('--perf-flags', help='Additional perf flags.')
    arg_parser.add_argument('--execution-stats', action='store_true', help='Enable collection of execution stats in Report')
    arg_parser.add_argument('--no-tank-monitoring', action='store_false', dest='tank_monitoring', help='Disable Tank monitoring')
    arg_parser.add_argument('--report-port', type=int, default=CUSTOM_PORT, help='TCP port number for Report')
    args = arg_parser.parse_args()

    config = LoadTestConfig()
    config.session = str(uuid.uuid4()) if args.session is None else args.session
    config.cluster = args.cluster
    config.artifacts_dir = expand_path(DEFAULT_ARTIFACTS_DIR if args.artifacts_dir is None else args.artifacts_dir)
    config.ticket = DEFAULT_TICKETS[config.cluster] if args.ticket is None else args.ticket
    config.ammo = get_recent_ammo(args.cluster) if args.ammo is None else args.ammo
    if config.ammo is not None and not re.match(AMMO_DATE_RE, config.ammo):
        config.ammo = expand_path(config.ammo)
    for report_type in REPORT_TYPES:
        set_test_count(config, report_type, 0)
    report_type = REPORT_NAMES.index(args.config_type)
    set_save_logs(config, report_type, False)
    set_warmup(config, report_type, False)
    set_test_count(config, report_type, args.test_count)
    if args.rps_sched is not None:
        set_rps_sched(config, report_type, args.rps_sched)
    config.save_logs = args.save_logs
    config.record_perf = args.record_perf
    config.perf_stat = args.perf_stat
    if args.perf_host is not None:
        config.perf_host = args.perf_host
    if args.perf_record_time is not None:
        config.perf_record_time = args.perf_record_time
    if args.perf_record_delay is not None:
        config.perf_record_delay = args.perf_record_delay
    config.perf_events = args.perf_events
    config.perf_flags = args.perf_flags
    config.execution_stats = args.execution_stats
    config.tank_monitoring = args.tank_monitoring
    config.report_port = args.report_port

    config.index_gen = get_current_index_gen(config.cluster, is_blue(report_type))
    config.index = get_first_test_index(config)

    print '>>> Making sure report lock for HP is acquired'
    create_report_lock(args.cluster).ensure_acquired()

    save_dir = os.path.join(config.artifacts_dir, config.session, str(config.index))
    create_directory(save_dir)
    try:
        export_config_to_ini_file(config, save_dir)
        fire_at_hp(report_type=report_type, save_dir=save_dir, config=config)
    except Exception:
        save_exception(save_dir)

    print generate_report_from_artifacts(config.artifacts_dir, config.session)

if __name__ == "__main__":
    main()
