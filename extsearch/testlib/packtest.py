#!/usr/bin/env python
# -*- coding: utf-8 -*-

import yatest.common
import errno
import os
import re
import sys
import time
import subprocess
import pytest
from base_process import BaseProcessDescription
from library.python.testing.deprecated import setup_environment
from mapreduce.yt.python.yt_stuff import YtStuff, YtConfig

DONT_STOP_OPTION = 'dont_stop_local_yt'
ENABLE_DEBUG_LOGGING = 'enable_debug_logging'
LOCAL_YT_DISK_LIMIT = 'local_yt_disk_limit'
LOCAL_YT_NODES_COUNT = 'local_yt_nodes_count'


def log_message(msg, stderr_only=False):
    if not stderr_only:
        print msg
    print >>sys.stderr, msg


class ProcessDescription(BaseProcessDescription):
    pass


def equal_files(name, src, dst):
    return name.endswith(".meta")


def merge_dir(target, source):
    prefix = os.path.commonprefix([target, source])
    log_message("Merge {} -> {}".format(source[len(prefix):], target[len(prefix):]))
    for name in os.listdir(source):
        src = os.path.join(source, name)
        dst = os.path.join(target, name)
        if os.path.isdir(src):
            if not os.path.exists(dst):
                os.mkdir(dst)
            merge_dir(dst, src)
        else:
            if os.path.isdir(dst):
                pytest.fail("Cannot merge file to dir {}".format(name))
            elif os.path.exists(dst) and equal_files(name, src, dst):
                log_message("  Skip equal {}".format(name))
                continue
            log_message("  Link file {}".format(name))
            os.link(src, dst)


def merge_dirs(target, sources):
    if not os.path.exists(target):
        os.makedirs(target)
    for src in sources:
        merge_dir(target, src)


class YtRegressionTestBase(object):
    """ Интеграционный тест

    Attributes:
        pack                 Список этапов теста.
    """

    tables_dir_name = 'tables_dir'
    union_dir_name = 'yt'

    # replace it in subclasses
    pack = None

    stuff = None
    all_results = None
    dump_dir = 'dump_dir'
    config_dir = 'config'
    sysconfig_dir = 'input_files'
    node_chunk_store_quota = None
    yt_nodes_count = None

    @classmethod
    def setup_class(cls):
        cypress_dir = os.path.abspath('./' + cls.tables_dir_name)
        union_dir = os.path.abspath('./' + cls.union_dir_name)
        if os.path.isdir(union_dir):
            merge_dirs(cypress_dir, [os.path.join(union_dir, subdir, cls.tables_dir_name) for subdir in sorted(os.listdir(union_dir))])
        cls.stuff = YtStuff(config=YtConfig(forbid_chunk_storage_in_tmpfs=True,
                                            node_chunk_store_quota=yatest.common.get_param(LOCAL_YT_DISK_LIMIT, '17179869184'),
                                            local_cypress_dir=cypress_dir,
                                            enable_debug_logging=yatest.common.get_param(ENABLE_DEBUG_LOGGING, False),
                                            node_count=yatest.common.get_param(LOCAL_YT_NODES_COUNT, cls.yt_nodes_count)))
        cls.stuff.start_local_yt()
        cls.yt_executable = yatest.common.binary_path('yt/python/yt/wrapper/bin/yt_make/yt')

        cls.bin_dir = setup_environment.setup_bin_dir()
        cls.exit_codes = {}

    @classmethod
    def teardown_class(cls):
        if cls.stuff and not bool(yatest.common.get_param(DONT_STOP_OPTION, False)):
            log_message("Local YT stopped")
            cls.stuff.stop_local_yt()
        else:
            log_message("Local YT wasn't stopped, you can use it to browse state of tables & files")
            log_message("You can find it at {}".format(cls.stuff.get_server()))

    def launcher(self, cmd):
        env = os.environ.copy()
        env['MR_RUNTIME'] = 'YT'
        env['YT_USE_YAMR_STYLE_PREFIX'] = '1'
        env['YT_PREFIX'] = '//'
        env['YT_USE_CLIENT_PROTOBUF'] = '0'
        for k, v in self.stuff.env.items():
            if k == 'PATH' and 'PATH' in env:
                env[k] = v + ":" + env[k]
            else:
                env[k] = v
        process = subprocess.Popen(cmd, shell=True, env=env)
        process.wait()

        if process.returncode != 0:
            pytest.fail("command %s failed with exit code %d " % (cmd, process.returncode))

        return process.returncode

    @classmethod
    def stage_name(cls, stage):
        stage_class_name = stage.__name__ if isinstance(stage, type) else stage.__class__.__name__
        return "%s.%s" % (cls.__name__, stage_class_name)

    def pytest_generate_tests(self, metafunc):
        if 'stage' in metafunc.fixturenames:
            metafunc.parametrize("stage", self.pack, False,
                                 map(lambda x: self.stage_name(x), self.pack))

    def test_run_stage(self, stage):
        stage_name = self.stage_name(stage)
        log_message("%s (%s) started " % (stage_name, stage.desc))
        time_before = time.time()

        xc = self.run_stage(stage)
        self.exit_codes["pre." + stage_name] = xc[0]
        self.exit_codes["stage." + stage_name] = xc[1]
        self.exit_codes["post." + stage_name] = xc[2]
        self.exit_codes["dumper." + stage_name] = xc[3]

        time_after = time.time()
        log_message("%s elapsed in %.02f seconds" % (stage_name, time_after - time_before))

    def run_stage(self, stage):
        pre_stage_exit_code = 0
        stage_exit_code = 0
        post_stage_exit_code = 0
        dumper_exit_code = 0

        bin = self.bin_dir
        program = os.path.join(bin, stage.program or stage.desc.split()[0])
        server = self.stuff.get_server()
        yt = self.yt_executable

        if stage.system_cmd:
            sysconfig_dir = os.path.join(stage.files_dir, self.sysconfig_dir)
            cmd = stage.system_cmd.format(mr_server=server,
                                          config_dir=sysconfig_dir,
                                          files_dir=stage.files_dir,
                                          program=program,
                                          dump_dir=self.dump_dir,
                                          bin_dir=self.bin_dir,
                                          yt=yt,
                                          **stage.custom_fields
                                          )
            pre_stage_exit_code = self.launcher(cmd)
            if pre_stage_exit_code != 0:
                return (pre_stage_exit_code, stage_exit_code, post_stage_exit_code,
                        dumper_exit_code)

        if stage.cmd:
            binary = os.path.join(bin, stage.desc.split()[0]) if not stage.binary else os.path.join(bin, stage.binary)
            config_dir = os.path.join(stage.files_dir, self.config_dir)
            cmd = stage.cmd.format(mr_server=server,
                                   config_dir=config_dir,
                                   files_dir=stage.files_dir,
                                   program=binary,
                                   bin_dir=self.bin_dir,
                                   dump_dir=self.dump_dir,
                                   **stage.custom_fields)
            stage_exit_code = self.launcher(" ".join(cmd.split()))
            if stage_exit_code != 0:
                return (pre_stage_exit_code, stage_exit_code, post_stage_exit_code,
                        dumper_exit_code)

        if stage.post_cmd:
            cmd = stage.post_cmd.format(mr_server=server,
                                        config_dir=self.sysconfig_dir,
                                        program=program,
                                        dump_dir=self.dump_dir,
                                        bin_dir=self.bin_dir,
                                        yt=yt,
                                        **stage.custom_fields
                                        )
            post_stage_exit_code = self.launcher(cmd)
            if post_stage_exit_code != 0:
                return (pre_stage_exit_code, stage_exit_code,
                        post_stage_exit_code, dumper_exit_code)

        if self.dump_dir:
            dumper_exit_code = self.dump_text_results(stage)

        return (pre_stage_exit_code, stage_exit_code, post_stage_exit_code, dumper_exit_code)

    def dump_text_results(self, stage):
        dump_dir = self.dump_dir
        try:
            os.makedirs(dump_dir)
        except OSError as exc:
            if exc.errno == errno.EEXIST and os.path.isdir(dump_dir):
                pass
            else:
                raise
        except:
            log_message('Error: Failed to create dump_dir: {dump_dir}, pwd : {pwd}'.
                        format(dump_dir=dump_dir,
                               pwd=os.getcwd()))

        for t in stage.output_tables:
            table = t.format(**stage.custom_fields)
            script = '''
                            set -e -x
                            dumper={yson_converter}
                            {yt} --proxy {proxy} read --format yson {table} 2>/dev/null | {dumper} > {dump_dir}/{name}.txt
                        '''.format(yt=self.yt_executable,
                                   proxy=self.stuff.get_server(),
                                   table=table,
                                   yson_converter=os.path.join(self.bin_dir, 'yson_converter'),
                                   dumper=self.get_dumper(table, stage.suffix_dump_modes, stage.regex_dump_modes),
                                   dump_dir=dump_dir,
                                   name=table.replace('/', ':'))
            exit_code = self.launcher(script)
            if exit_code != 0:
                return exit_code
        return 0

    @classmethod
    def get_dumper(cls, name, custom_suffix_dumper_modes, custom_regex_dumper_modes):
        mode = None
        for (suffix, m) in custom_suffix_dumper_modes:
            if name.endswith(suffix):
                mode = m
                break

        if not mode:
            for (pattern, m) in custom_regex_dumper_modes:
                if re.search(pattern, name):
                    mode = m
                    break
        if not mode:
            mode = '%s -t hex' % (os.path.join(cls.bin_dir, 'yson_converter'))
        log_message("Dumping %s with %s" % (name, mode), True)
        return mode

    def download_tables_to_dir(self, directory, selection=None):
        self.launcher("{yt} find --path //sandbox --type table | sort > .download_tables_to_dir.tables".format(yt=self.yt_executable))
        tables = [t.strip() for t in open('.download_tables_to_dir.tables')]

        for table_name in tables:
            table_name = table_name
            file_name = table_name.replace('/', ':')
            if selection is None or table_name in selection:
                self.launcher('''
                    set -x
                    {yt} read --format yson {table} | gzip -c -9 > {fn}.yson.gz 2>/dev/null
                    '''.format(yt=self.yt_executable,
                               fn=os.path.join(directory, file_name),
                               table=table_name
                               ))

    def test_files(self):
        canonical_files = {}
        yacf = yatest.common.canonical_file
        for f in os.listdir(self.dump_dir):
            canonical_files[f.replace(':', '/')] = yacf(os.path.join(self.dump_dir, f))

        if yatest.common.get_param('save_regression_test_tables', False):
            result_tables = yatest.common.output_path('result_tables')
            os.mkdir(result_tables)
            log_message("result_tables %s" % (result_tables), True)
            self.download_tables_to_dir(result_tables)

        result = {
            'files': canonical_files
        }
        return result

    def test_exit_codes(self):
        result = {
            'exit_codes': self.exit_codes
        }
        return result

    def test_nothing_failed(self):
        for key, value in self.exit_codes.iteritems():
            if value != 0:
                pytest.fail("An error occurred during test case execution: %s %d" % (key, value))
