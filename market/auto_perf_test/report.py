#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
import collections
import os
import re
import socket
import traceback

from config import *
from constants import *
from utils import *


def parse_query(url):
    query_params = collections.defaultdict(list)
    query_index = url.find('?')
    if query_index != -1:
        for params in url[query_index + 1:].split('&'):
            if params:
                query_parts = params.split('=', 1)
                key = query_parts[0].strip()
                value = '' if len(query_parts) == 1 else query_parts[1].strip()
                query_params[key].append(value)
    return query_params


class AccessTskvLogRecord(object):
    def __init__(self, line):
        self.__params = dict()
        # skip first field - 'tskv'
        for params in line.split('\t')[1:]:
            key, value = params.split('=', 1)
            self.__params[key.strip()] = value.strip()
        self.__query_params = parse_query(self.__params.get('url', ''))

    def is_service_request(self):
        place = self.__query_params.get('place', [''])
        # Служебные плейсы
        if "consistency_check" in place or "report_status" in place:
            return True
        # Подзарпосы
        if self.__query_params.get('rep-outgoing', ['']) != ['']:
            return True
        if self.__query_params.get('subrequest', ['']) != ['']:
            return True
        # Запросы от прогревалки
        if self.__query_params.get('local-sources-only', ['']) != ['']:
            return True
        if self.__query_params.get('mini-tank', ['']) != ['']:
            return True
        return False

    def get_wizards(self):
        return self.__params.get('wizards', '').split(',')


def get_host_dirs(stats_dir):
    host_matcher = re.compile(r'^msh\d\dhp$')
    return [name for name in os.listdir(stats_dir) if host_matcher.match(name)]


def get_memory_counters(file_path):
    PROC_STATUS_KEY_NAMES = ('RssAnon')
    with open(file_path) as f:
        proc_status = f.read()
    counters = dict()
    for line in proc_status.splitlines():
        name, value = line.split(':')
        if name in PROC_STATUS_KEY_NAMES:
            value = int(value.strip().split(' ')[0]) * 1024
            counters[name] = value
    return counters


def gather_process_stats(sub_test_dir):
    stats_dir = os.path.join(sub_test_dir, 'stats_after')
    host_dirs = get_host_dirs(stats_dir)
    snippet_host_matcher = re.compile(r'^msh-off\d\dhp$')
    snippet_host_dirs = [name for name in os.listdir(stats_dir) if snippet_host_matcher.match(name)]
    process_stats = dict()
    for host_dir in host_dirs:
        process_stats = merge_dicts(
            process_stats,
            get_memory_counters(os.path.join(stats_dir, host_dir, 'status')),
            max)
    snippet_process_stats = get_memory_counters(
        os.path.join(stats_dir, snippet_host_dirs[0], 'status')) if snippet_host_dirs else None
    return process_stats, snippet_process_stats


def get_wizard_counters(access_log_file):
    wizard_counters = collections.defaultdict(int)
    if os.path.exists(access_log_file):
        with open(access_log_file, 'rt') as log_file:
            for line in log_file:
                log_record = AccessTskvLogRecord(line)
                if not log_record.is_service_request():
                    for wizard in log_record.get_wizards():
                        # Считаем только определенные колдунщики
                        if wizard in ("market_model", "market_model_right_incut", "market_ext_category",
                                      "market_implicit_model", "market_implicit_model_center_incut", "market_implicit_model_adg_wizard", "market_implicit_model_without_incut",
                                      "market_offers_wizard", "market_offers_wizard_right_incut", "market_offers_wizard_center_incut", "market_offers_adg_wizard"):
                            wizard_counters[wizard] += 1
    return wizard_counters


def gather_wizard_stats(sub_test_dir):
    host_dirs = get_host_dirs(sub_test_dir)
    wizard_stats = dict()
    for host_dir in host_dirs:
        access_log_file = os.path.join(sub_test_dir, host_dir, PARALLEL_LOGS[0])
        wizard_stats = merge_dicts(wizard_stats, get_wizard_counters(access_log_file), sum_args)
    return wizard_stats


def calculate_stats_from_phout_log(phout_log_path):
    ABNORMAL_CONNECT_TIME_US = 100000
    timings = list()
    http_error_count = 0
    net_error_count = 0
    with open(phout_log_path, 'r') as f:
        for line in f:
            split_line = line.split('\t')
            request_time = float(split_line[2]) / 1000
            connect_time = int(split_line[3])
            err_code = int(split_line[10])
            http_code = int(split_line[11])
            if err_code:
                net_error_count += 1
            if http_code // 100 == 5:
                http_error_count += 1
            # Не учитываем запросы с ненормально высоким временем установки соединения.
            if connect_time >= ABNORMAL_CONNECT_TIME_US:
                continue
            timings.append(request_time)
    timings.sort()
    results = list()
    for percentile in PERCENTILES:
        index = min(int(len(timings) * percentile / 100.0), len(timings) - 1)
        results.append(timings[index])
    return results, http_error_count, net_error_count


class ReportGenerator(object):
    def __init__(self, artifacts_dir, session):
        self.root_path = os.path.join(artifacts_dir, session)
        self.test_ids = [int(name) for name in os.listdir(self.root_path) if is_int(name)]
        self.test_ids.sort()

        self.report = 'Session id: {0}\n'.format(session)
        self.report += 'Artifacts are in {host}:{dir}\n'.format(
            host=socket.getfqdn(),
            dir=os.path.join(artifacts_dir, session)
        )

        errors = list()
        for test_id in self.test_ids:
            error_path = os.path.join(self.root_path, str(test_id), ERROR_FILE)
            if not os.path.isfile(error_path):
                continue
            with open(error_path, 'r') as f:
                error_text = f.read().rstrip(CTRL_CHARS)
            errors.append((test_id, error_text))
        if errors:
            self.report += create_warning('{0} test(s) failed!'.format(len(errors)))
            for test_id, error_text in errors:
                self.report += '{0}:\n{1}\n'.format(test_id, error_text)

        for percentile in PERCENTILES:
            self.report += '{0:>7}%'.format(format_float(percentile))
        self.report += '\n'

        self.reference_percentiles = None
        self.reference_process_stats = None
        self.reference_snippet_process_stats = None
        self.reference_wizard_stats = None

        for report_type in REPORT_TYPES:
            report_for_type = self.gen_report_for_type(report_type)
            if report_for_type:
                self.report += REPORT_NAMES[report_type] + '\n' + report_for_type

    def __str__(self):
        return self.report

    def gen_report_for_type(self, report_type):
        report = ''
        self.reference_percentiles = None
        self.reference_process_stats = None
        self.reference_snippet_process_stats = None
        self.reference_wizard_stats = None

        for test_id in self.test_ids:
            try:
                report += self.gen_report_for_test_id(report_type, test_id)
            except Exception:
                report += '{0}: Report generation failed\n{1}'.format(test_id, traceback.format_exc())

        return report

    def gen_report_for_test_id(self, report_type, test_id):
        report = ''
        error_path = os.path.join(self.root_path, str(test_id), ERROR_FILE)
        if os.path.isfile(error_path):
            return report

        test_path = os.path.join(self.root_path, str(test_id), REPORT_NAMES[report_type])
        if not os.path.isdir(test_path):
            return report
        sub_test_ids = [int(name) for name in os.listdir(test_path) if is_int(name)]
        sub_test_ids.sort()
        if not sub_test_ids:
            return report

        saved_config = import_config_from_ini_file(os.path.join(self.root_path, str(test_id)))
        report += '{0}: {1}\n'.format(test_id, get_revision_descr(saved_config))
        report += 'index:' + saved_config.index_gen
        report += ' ammo:' + saved_config.ammo
        report += ' rps:' + get_rps_sched(saved_config, report_type) + '\n'

        current_percentiles = None
        current_process_stats = dict()
        current_snippet_process_stats = dict()
        current_wizard_stats = dict()
        for sub_test_id in sub_test_ids:
            sub_test_dir = os.path.join(test_path, str(sub_test_id))
            web_link_path = os.path.join(sub_test_dir, WEB_LINK_FILE)
            with open(web_link_path, 'r') as f:
                report += f.read() + '\n'
            phout_log_path = os.path.join(sub_test_dir, PHOUT_LOG)
            percentiles, http_error_count, net_error_count = calculate_stats_from_phout_log(phout_log_path)
            if current_percentiles is None:
                current_percentiles = percentiles
            else:
                current_percentiles = map(min, current_percentiles, percentiles)
            if http_error_count > REPORT_ERROR_COUNT_THRESHOLD:
                report += create_warning('5xx error count is {0}!'.format(http_error_count))
            if net_error_count > REPORT_ERROR_COUNT_THRESHOLD:
                report += create_warning('net error count is {0}!'.format(net_error_count))
            for percentile in percentiles:
                report += '{0:>8}'.format(format_float(percentile))
            report += '\n'

            process_stats, snippet_process_stats = gather_process_stats(sub_test_dir)
            current_process_stats = merge_dicts(
                current_process_stats, process_stats, max)
            if snippet_process_stats:
                current_snippet_process_stats = merge_dicts(
                    current_snippet_process_stats, snippet_process_stats, max)

            if report_type == REPORT_PARALLEL:
                wizard_stats = gather_wizard_stats(sub_test_dir)
                current_wizard_stats = merge_dicts(current_wizard_stats, wizard_stats, sum_args)

        if self.reference_percentiles is None:
            self.reference_percentiles = current_percentiles
            self.reference_process_stats = current_process_stats
            self.reference_snippet_process_stats = current_snippet_process_stats
            self.reference_wizard_stats = current_wizard_stats
        else:
            report += 'Relative change:\n'
            for current, ref in zip(current_percentiles, self.reference_percentiles):
                rel_change = 100.0 * (current - ref) / ref
                report += '{0:+7.1f}%'.format(rel_change) if rel_change else 7 * ' ' + '-'
            report += '\n'

        def generate_stats(stats, ref_stats, formatter=str):
            result = ''
            for key, value in stats.iteritems():
                result += ' {0}={1}'.format(key, formatter(value))
                if key in ref_stats:
                    ref_value = ref_stats[key]
                    value_diff = float(value - ref_value) / ref_value
                    if value != ref_value and abs(value_diff) > DEFAULT_SUPPRESS_DIFF_THRESHOLD:
                        message = '{}%'.format(round(value_diff * 100, DIFF_ROUNDING_PRECISION))
                        if abs(value_diff) > DEFAULT_WARNING_THRESHOLD:
                            message = create_warning(message, prefix='', newline=False)
                        result += ' [{}]'.format(message)
            return result

        report += 'Process stats:{0}\n'.format(generate_stats(
            current_process_stats, self.reference_process_stats,
            formatter=get_human_readable_size_str))
        report += 'Snippet process stats:{0}\n'.format(generate_stats(
            current_snippet_process_stats, self.reference_snippet_process_stats,
            formatter=get_human_readable_size_str))

        if report_type == REPORT_PARALLEL:
            report += 'Wizard stats:{0}\n'.format(generate_stats(current_wizard_stats, self.reference_wizard_stats))

        return report


def generate_report_from_artifacts(artifacts_dir, session):
    return str(ReportGenerator(artifacts_dir, session))


def main():
    arg_parser = argparse.ArgumentParser()
    arg_parser.add_argument('--artifacts-dir', default=DEFAULT_ARTIFACTS_DIR)
    arg_parser.add_argument('--session', metavar='GUID', required=True)
    args = arg_parser.parse_args()

    artifacts_dir = expand_path(args.artifacts_dir)

    print generate_report_from_artifacts(artifacts_dir, args.session)


if __name__ == "__main__":
    main()
