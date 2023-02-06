# -*- coding: utf-8 -*-

import ConfigParser
import uuid
import os

from constants import *
from utils import *


# В этом классе не должно быть методов, т.к. это ломает экспорт в файл
class LoadTestConfig(object):

    def __init__(self):
        # these are set automatically
        self.session = str(uuid.uuid4())
        self.index = 0
        # these typically stay constant for every test
        self.cluster = 0
        self.artifacts_dir = DEFAULT_ARTIFACTS_DIR
        self.svn_dir = None
        self.git_dir = None
        self.dist_build = True
        self.extra_build_options = None
        self.ticket = None
        self.ammo = None
        self.index_gen = None
        self.main_save_logs = False
        self.main_warmup = True
        self.main_test_count = DEFAULT_TEST_COUNT[REPORT_MAIN]
        self.main_warmup_rps_sched = DEFAULT_WARMUP_RPS_SCHED[REPORT_MAIN]
        self.main_rps_sched = DEFAULT_RPS_SCHED[REPORT_MAIN]
        self.parallel_save_logs = False
        self.parallel_warmup = True
        self.parallel_test_count = DEFAULT_TEST_COUNT[REPORT_PARALLEL]
        self.parallel_warmup_rps_sched = DEFAULT_WARMUP_RPS_SCHED[REPORT_PARALLEL]
        self.parallel_rps_sched = DEFAULT_RPS_SCHED[REPORT_PARALLEL]
        self.api_save_logs = False
        self.api_warmup = True
        self.api_test_count = DEFAULT_TEST_COUNT[REPORT_API]
        self.api_warmup_rps_sched = DEFAULT_WARMUP_RPS_SCHED[REPORT_API]
        self.api_rps_sched = DEFAULT_RPS_SCHED[REPORT_API]
        self.int_save_logs = False
        self.int_warmup = True
        self.int_test_count = DEFAULT_TEST_COUNT[REPORT_INT]
        self.int_warmup_rps_sched = DEFAULT_WARMUP_RPS_SCHED[REPORT_INT]
        self.int_rps_sched = DEFAULT_RPS_SCHED[REPORT_INT]
        self.blue_main_save_logs = False
        self.blue_main_warmup = True
        self.blue_main_test_count = DEFAULT_TEST_COUNT[REPORT_BLUE_MAIN]
        self.blue_main_warmup_rps_sched = DEFAULT_WARMUP_RPS_SCHED[REPORT_BLUE_MAIN]
        self.blue_main_rps_sched = DEFAULT_RPS_SCHED[REPORT_BLUE_MAIN]
        self.save_logs = False
        self.record_perf = False
        self.perf_stat = False
        self.perf_executable_path = DEFAULT_PERF_EXECUTABLE_PATH
        self.flame_graph_path = DEFAULT_FLAME_GRAPH_PATH
        self.perf_host = DEFAULT_PERF_HOST_INDEX
        self.perf_record_time = DEFAULT_PERF_RECORD_TIME
        self.perf_record_delay = DEFAULT_PERF_RECORD_DELAY
        self.perf_events = None
        self.perf_flags = None
        self.execution_stats = False
        self.tank_monitoring = True
        self.enable_docfetcher = False
        self.report_port = CUSTOM_PORT
        self.use_model_collection_service = False
        self.use_dmock = False
        # these identify source code revision for testing
        self.use_git = False
        self.git_branch = None
        self.svn_revision = None
        self.svn_branch = None
        self.svn_patch = None


def get_report_type_attr(config, report_type, attr_name):
    return getattr(config, '{report_name}_{attr_name}'.format(report_name=REPORT_NAMES[report_type], attr_name=attr_name))


def set_report_type_attr(config, report_type, attr_name, attr_value):
    setattr(config, '{report_name}_{attr_name}'.format(report_name=REPORT_NAMES[report_type], attr_name=attr_name), attr_value)


def get_test_count(config, report_type):
    return get_report_type_attr(config, report_type, 'test_count')


def set_test_count(config, report_type, value):
    set_report_type_attr(config, report_type, 'test_count', value)


def get_save_logs(config, report_type):
    return get_report_type_attr(config, report_type, 'save_logs')


def set_save_logs(config, report_type, value):
    set_report_type_attr(config, report_type, 'save_logs', value)


def get_warmup(config, report_type):
    return get_report_type_attr(config, report_type, 'warmup')


def set_warmup(config, report_type, value):
    set_report_type_attr(config, report_type, 'warmup', value)


def get_warmup_rps_sched(config, report_type):
    return get_report_type_attr(config, report_type, 'warmup_rps_sched')


def get_rps_sched(config, report_type):
    return get_report_type_attr(config, report_type, 'rps_sched')


def set_rps_sched(config, report_type, value):
    set_report_type_attr(config, report_type, 'rps_sched', value)


def get_revision_descr2(git_branch, svn_branch, svn_revision, svn_patch):
    revision = ''
    if git_branch is not None:
        revision = git_branch
    else:
        if svn_branch is not None:
            revision = svn_branch
        if svn_revision is not None:
            if revision:
                revision += ', '
            revision += 'r' + str(svn_revision)
        if svn_patch is not None:
            revision += ' patch:' + os.path.basename(svn_patch)
    return revision


def get_revision_descr(config):
    return get_revision_descr2(config.git_branch, config.svn_branch, config.svn_revision, config.svn_patch)


def import_config_from_ini_file(dir_path):
    config = LoadTestConfig()
    config_parser = ConfigParser.ConfigParser()
    config_parser.read(os.path.join(dir_path, CONFIG_FILE_NAME))
    for name, value in config_parser.items('config'):
        if hasattr(config, name):
            setattr(config, name, value)
    return config


def export_config_to_ini_file(config, save_dir):
    text = '[config]\n'
    for attr_name in dir(config):
        if not attr_name.startswith('__'):
            value = getattr(config, attr_name)
            if value is not None:
                text += '{0}={1}\n'.format(attr_name, value)
    with open(os.path.join(save_dir, CONFIG_FILE_NAME), 'w') as f:
        f.write(text)


def get_first_test_index(config):
    root_path = os.path.join(config.artifacts_dir, config.session)
    if not os.path.isdir(root_path):
        return 0
    test_ids = [int(name) for name in os.listdir(root_path) if is_int(name)]
    if not test_ids:
        return 0
    test_ids.sort()
    return test_ids[-1] + 1


def is_blue(report_type):
    return report_type == REPORT_BLUE_MAIN


def is_blue_test(config):
    return get_test_count(config, REPORT_BLUE_MAIN)
