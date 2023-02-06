# -*- coding: utf-8 -*-

import json
import os
import subprocess
import unittest
import logging

from market.idx.yatf.utils.mmap.mmapviewer import json_view
from mapreduce.yt.python.yt_stuff import YtStuff, YtConfig

from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.resources.yt_token_resource import YtTokenResource
import yatest


PBSNCAT_PATH = yatest.common.binary_path(
    'market/idx/tools/pbsncat/bin/pbsncat'
)

ALLOWED_PARAM_FILTER_PATH = yatest.common.binary_path(
    'market/idx/generation/allowed-param-filter/allowed-param-filter'
)

MAKEGENLOG_PATH = yatest.common.binary_path(
    'market/idx/stats/tests/make_genlog/make_genlog'
)

SORT_LENVAL_PATH = yatest.common.binary_path(
    'market/idx/stats/tests/sort_lenval/sort_lenval'
)

RUN_STATS_CALC_PATH = yatest.common.test_source_path(
    'run_stats_calc.sh'
)

CONFIG_PATH = yatest.common.source_path(
    'market/idx/stats/config/statscalc_config.prototxt'
)

DATA_PATH = yatest.common.test_source_path(
    'data'
)

YT_SERVER = None


def start_yt_server():
    """
    Local YT server
    Empty //home cypress directory already created
    """
    global YT_SERVER
    if not YT_SERVER:
        logging.info('Starting local yt ...')
        YT_SERVER = YtStuff(YtConfig(wait_tablet_cell_initialization=True))
        YT_SERVER.start_local_yt()
        logging.info('Local yt started on {connect_point}'.format(
            connect_point=YT_SERVER.get_server()
        ))
        YT_SERVER.get_yt_client().config['prefix'] = get_yt_prefix()
    return YT_SERVER


class CsvHeaderParser(object):
    def __init__(self, columns_string):
        self._name2column = {}
        self._parse(columns_string)

    def _parse(self, columns_string):
        columns = columns_string.split('\t')
        if columns[0] != '@':
            raise Exception('Format violation')

        for idx, column in enumerate(columns):
            self._name2column[column] = idx

    def get(self, name):
        return self._name2column.get(name, None)


class StatsCalcBaseTestCase(unittest.TestCase):
    STATS_CALC_PATH = yatest.common.binary_path(
        'market/idx/stats/bin/stats-calc/stats-calc'
    )

    STATS_CONVERT_PATH = yatest.common.binary_path(
        'market/idx/stats/bin/stats-convert/stats-convert'
    )

    def setUp(self):
        self.maxDiff = None
#        self.yt_server = 'hahn'
#        self.yt_tokenpath = '/home/aavdonkin/yt-market-indexer'
        self.yt_server = start_yt_server()
        if self.yt_server is not None:
            self.yt_tokenpath = YtTokenResource().path
            logging.info('Got server')
            print 'yt_server', self.yt_server.get_server()
            print 'token', self.yt_tokenpath
        else:
            self.yt_server = None

    @property
    def tmp_dir(self):
        path = yatest.common.test_output_path('tmp')
        try:
            os.makedirs(path)
        except OSError:
            pass  # normal when the directory already exists
        return path

    def tmp_file_path(self, file_name):
        return os.path.join(self.tmp_dir, file_name)

    def run_stats_calc(self, statistic, gen_log_json, fmt='orig'):
        filepath = self.tmp_file_path('genlog.json')
        with open(filepath, 'w') as _f:
            _f.write(gen_log_json)

        self.run_stats_calc_from_file(statistic, filepath, fmt)

    def run_stats_calc_from_file(self, statistic, filepath, fmt='orig'):
        genlog_path = self.run_makegenlog(filepath)
        self.run_stats_calc_sh(statistic, genlog_path, fmt)

    def run_makegenlog(self, filepath):
        genlog_path = os.path.join(self.tmp_dir, 'genlog.pbuf.sn')
        if self.yt_server:
            args = "{bin_path} '{f}' '{p}' '' //tmp/in/0000".format(
                bin_path=MAKEGENLOG_PATH,
                f=filepath,
                p=self.yt_server.get_server()
            )
        else:
            args = "{bin_path} '{f}' > {gl}".format(
                bin_path=MAKEGENLOG_PATH,
                f=filepath,
                gl=genlog_path,
            )
        subprocess.check_call(args=args, shell=True)
        return genlog_path

    def run_stats_calc_sh(self, statistic, genlog_path='', fmt='orig'):
        stats_calc = (
            '{bin_path} '
            '-c {config_path} '
            '-s {stat} '
            '-t {tmpdir} '
            '-f {fmt} '
            '-P {pbsncat_path} '
            '-F {filter_path} '
            '-L {sort_lenval_path} '
            '-S {stats_calc_path} '
            '-C {stats_convert_path} '
            '-d {data_dir} '
            '-g {genlog_path} '
            '-p {yt_proxy} '
            '-T {yt_tokenpath} '
            '2>>{logpath} '
        ).format(
            bin_path=RUN_STATS_CALC_PATH,
            config_path=CONFIG_PATH,
            stat=statistic,
            tmpdir=self.tmp_dir,
            fmt=fmt,
            pbsncat_path=PBSNCAT_PATH,
            filter_path=ALLOWED_PARAM_FILTER_PATH,
            sort_lenval_path=SORT_LENVAL_PATH,
            stats_calc_path=self.STATS_CALC_PATH,
            stats_convert_path=self.STATS_CONVERT_PATH,
            data_dir=DATA_PATH,
            genlog_path=genlog_path,
            logpath=self.tmp_file_path('run_stats_calc.log'),
            yt_proxy=self.yt_server.get_server(),
            yt_tokenpath=self.yt_tokenpath
        )
        logging.info('start {}'.format(stats_calc))
        subprocess.check_call(args=stats_calc, shell=True)
        logging.info('finish {}'.format(stats_calc))

    def get_stats_from_pbufsn(self, pbufsn_path):
        args = [PBSNCAT_PATH, '--format', 'json', pbufsn_path]
        out_path = self.tmp_file_path('stats.json')

        with open(out_path, 'w') as out:
            subprocess.check_call(args=args, stdout=out)
        with open(out_path, 'r') as fobj:
            return [json.loads(line) for line in fobj]

    def get_stats_from_mmap(self, mmap_path, mmap_type=None):
        code, result = json_view(mmap_path, type=mmap_type)
        if code:
            raise RuntimeError('some error during converting mmap to json')
        return result
