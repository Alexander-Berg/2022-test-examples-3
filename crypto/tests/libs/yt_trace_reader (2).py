#!/usr/bin/python
# -*- coding: UTF-8 -*-
import os
import re
import ast
from collections import namedtuple
from datetime import datetime, timedelta

LOG_PATH = "/var/log/syslog"
YtTraceReport = namedtuple(
    "YtTraceReport",
    "operations_list operations_count no_start_count no_end_count exec_time sum_time max_time errors errors_data",
)
YtTraceRecord = namedtuple("YtTraceRecord", "metadata datetime uid status sync operation_type source")
YtTraceRecordNotSync = namedtuple("YtTraceRecordNotSync", "metadata datetime uid operation_id status")
YrErrorsData = namedtuple("YrErrorsData", "func_hash operation_name input_paths output_paths reduce_by")
yt_trace_record_pattern = (
    r"(.*?) (\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2},\d{3}) (\w{32})\|\|(\S{3,5})\|\|(.*?)\|\|(.*?)\|\|(.*?)$"
)
record_uid_pattern = re.compile(r" (\w{32})\|\|")
record_datetime_pattern = re.compile(r" (\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2},\d{3}) ")
yt_trace_record_not_sync_pattern = r"(.*?) (\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2},\d{3}) (.*?)\|\|(.*?)\|\|(.*?)$"
DATETIME_PATTERN = "%Y-%m-%d %H:%M:%S,%f"
OPERATION_START_STATUS_NAME = "start"
OPERATION_STOP_STATUS_NAME = "end"
OPERATION_ERROR_STATUS_NAME = "error"
OPERATION_NOSYNC_PROGRESS = "progress"
YT_TRACE_KEY_WORD = ":yt_trace.py:"
LUIGI_INTERFACE_KEY_WORD = "luigi-log:"
YT_TRACE_PATH_PREFIX = "yt-trace-"


class BaseReader(object):
    def __init__(self, start_datetime=timedelta(), log_folder=LOG_PATH):
        self.start_datetime = start_datetime
        self.log_folder = log_folder

    def parse_log(self):
        raise NotImplementedError()

    def pick_logs(self, folder, key_word, start_datetime, log_prefix=""):
        end_datetime = datetime.now()
        log_paths = self.get_all_log_paths(end_datetime, log_prefix)
        all_pick_records = []
        for path in log_paths:
            os.system("sudo chmod 777 " + path)
            with open(path, "r") as raw_log:
                log = filter(lambda record: key_word in record, raw_log)
                log = filter(
                    lambda record: datetime.strptime(record_datetime_pattern.findall(record)[0], DATETIME_PATTERN)
                    > start_datetime,
                    log,
                )
                all_pick_records += map(lambda record: record.split(key_word)[1].rstrip(), log)
        return all_pick_records

    def get_all_log_paths(self, end_datetime, log_prefix):
        import yatest

        return [yatest.common.output_path("stdout")]


class LuigiReader(BaseReader):
    def parse_log(self):
        return self.pick_logs(self.log_folder, LUIGI_INTERFACE_KEY_WORD, self.start_datetime)


class TraceReader(BaseReader):
    def parse_log(self):
        yt_trace_log = self.pick_logs(self.log_folder, YT_TRACE_KEY_WORD, self.start_datetime, YT_TRACE_PATH_PREFIX)
        operations = self.create_operations(yt_trace_log)
        operations_list = list(operations.items())
        operations_list_sorted = sorted(operations_list, key=self.sortByExecutionTime)
        operations_exec_times_list = [self.get_exec_time(operation) for operation in operations.itervalues()]
        record_start_datetimes_list = [
            self.get_record_datetime(operation, OPERATION_START_STATUS_NAME)
            for operation in operations.itervalues()
            if OPERATION_START_STATUS_NAME in operation
        ]
        record_end_datetimes_list = [
            self.get_record_datetime(operation, OPERATION_STOP_STATUS_NAME)
            for operation in operations.itervalues()
            if OPERATION_STOP_STATUS_NAME in operation
        ]
        if record_end_datetimes_list and record_start_datetimes_list:
            true_exec_time = max(record_end_datetimes_list) - min(record_start_datetimes_list)
        else:
            true_exec_time = timedelta()
        sum_exec_time = sum(operations_exec_times_list, timedelta())
        if operations_exec_times_list:
            max_exec_time = max(operations_exec_times_list)
        else:
            max_exec_time = timedelta()
        errors = filter(lambda operation: OPERATION_ERROR_STATUS_NAME in operation, operations.itervalues())
        errors_data = self.get_errors_data(errors)
        no_start_operations = filter(
            lambda operation: OPERATION_START_STATUS_NAME not in operation
            or not operation[OPERATION_START_STATUS_NAME],
            operations.itervalues(),
        )
        no_stop_operations = filter(
            lambda operation: OPERATION_STOP_STATUS_NAME not in operation or not operation[OPERATION_STOP_STATUS_NAME],
            operations.itervalues(),
        )
        return YtTraceReport(
            operations_list_sorted,
            len(operations),
            len(no_start_operations),
            len(no_stop_operations),
            str(true_exec_time),
            (sum_exec_time),
            (max_exec_time),
            errors,
            errors_data,
        )

    def create_operations(self, log):
        operations = {record_uid_pattern.findall(record)[0]: dict() for record in log if record}
        for record in log:
            if not record:
                continue
            try:
                yt_trace_record = self.create_record_by_pattern(YtTraceRecord, record, yt_trace_record_pattern)
                operations[yt_trace_record.uid][yt_trace_record.status] = self.create_record_by_pattern(
                    YtTraceRecord, record, yt_trace_record_pattern
                )
            except:
                yt_trace_not_sync = self.create_record_by_pattern(
                    YtTraceRecordNotSync, record, yt_trace_record_not_sync_pattern
                )
                if OPERATION_NOSYNC_PROGRESS in operations[yt_trace_not_sync.uid]:
                    operations[yt_trace_not_sync.uid][OPERATION_NOSYNC_PROGRESS].append(
                        self.create_record_by_pattern(YtTraceRecordNotSync, record, yt_trace_record_not_sync_pattern)
                    )
                else:
                    operations[yt_trace_not_sync.uid][OPERATION_NOSYNC_PROGRESS] = [
                        self.create_record_by_pattern(YtTraceRecordNotSync, record, yt_trace_record_not_sync_pattern)
                    ]
        return operations

    def create_record_by_pattern(self, record_description, record, pattern):
        return record_description(*re.match(pattern, record, re.M).groups())

    def sortByExecutionTime(self, operation):
        return self.get_exec_time(operation[1])

    def get_exec_time(self, operation):
        if OPERATION_START_STATUS_NAME in operation and OPERATION_STOP_STATUS_NAME in operation:
            start = datetime.strptime(operation[OPERATION_START_STATUS_NAME].datetime, DATETIME_PATTERN)
            end = datetime.strptime(operation[OPERATION_STOP_STATUS_NAME].datetime, DATETIME_PATTERN)
            return end - start
        return timedelta()

    def get_record_datetime(self, operation, record_status):
        if record_status in operation:
            return datetime.strptime(operation[record_status].datetime, DATETIME_PATTERN)
        return timedelta()

    def get_errors_data(self, errors):
        return [self.parse_yt_errors(error_dict["error"]) for error_dict in errors if error_dict.get("error")]

    def parse_yt_errors(self, data):
        operation_data = data[-1]
        operation_name = data[-2]
        operation_pattern = self.select_operation_pattern(operation_data)
        try:
            parsed_params = operation_pattern.match(operation_data).groupdict()
        except:
            raise Exception("ERROR: Not parsed operation data\n" + str(operation_data))
        if not parsed_params:
            raise Exception("ERROR: Not found operation data\n" + str(operation_data))
        parsed_params["operation"] = operation_name
        parsed_params["inputs"] = self.path_eval(parsed_params["inputs"])
        parsed_params["outputs"] = self.path_eval(parsed_params["outputs"])
        return parsed_params

    def select_operation_pattern(self, operation_data):
        ALL_IS_LIST = (True, True)
        INPUT_IS_LIST = (True, False)
        OUTPUT_IS_LIST = (False, True)
        ALL_IS_STRING = (False, False)
        input_list_check = re.compile(r".*?\>,.\[(.*?)$")
        output_list_check = re.compile(r".*?\]\)\|\|(.*?)$")
        OPERATION_PATTERNS = {
            ALL_IS_LIST: re.compile(
                r"\(\<(?P<name>.*?)\>,.\[(?P<inputs>.*?)\],.\[(?P<outputs>.*?)\]\)\|\|(?P<reduce_by>.*?)\|\|(.*?)$",
                re.VERBOSE,
            ),
            INPUT_IS_LIST: re.compile(
                r"\(\<(?P<name>.*?)\>,.\[(?P<inputs>.*?)\],.(?P<outputs>.*?)\)\|\|(?P<reduce_by>.*?)\|\|(.*?)$",
                re.VERBOSE,
            ),
            OUTPUT_IS_LIST: re.compile(
                r"\(\<(?P<name>.*?)\>,.(?P<inputs>.*?),.\[(?P<outputs>.*?)\]\)\|\|(?P<reduce_by>.*?)\|\|(.*?)$",
                re.VERBOSE,
            ),
            ALL_IS_STRING: re.compile(
                r"\(\<(?P<name>.*?)\>,.(?P<inputs>.*?),.(?P<outputs>.*?)\)\|\|(?P<reduce_by>.*?)\|\|(.*?)$", re.VERBOSE
            ),
        }
        return OPERATION_PATTERNS[
            bool(input_list_check.match(operation_data)), bool(output_list_check.match(operation_data))
        ]

    def path_eval(self, path):
        if len(path.split(",")) > 1:
            return list(ast.literal_eval(path))
        else:
            return [ast.literal_eval(path)]
