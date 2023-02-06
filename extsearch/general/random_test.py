# coding: utf-8

import base64
import random
import subprocess
import sys
from crc64 import crc64
from yatest import common


MAX_POWER = 20  # 2**20 = 1M - max size of data


def random_data():
    size_data = int(2**random.randrange(MAX_POWER))
    raw_data = [chr(i % 256) for i in range(size_data)]
    random.shuffle(raw_data)
    return ''.join(raw_data)


def compare_results(runner, data, known_crc=None):
    based64_data = base64.standard_b64encode(data)
    crc_py = crc64(data)
    if known_crc and crc_py != known_crc:
        raise Exception('calculated invalid crc64 {} for data "{}" (expected crc64 {})'.format(hex(crc_py), data, hex(known_crc)))
    runner._process.stdin.write(based64_data + '\n')
    runner._process.stdin.flush()
    hex_crc_cpp = runner._process.stdout.readline().strip()
    crc_cpp = long(hex_crc_cpp, 16)
    if crc_py != crc_cpp:
        error_header = 'crc64 value in python differ from value in c++!'
        print >>sys.stderr, error_header
        print >>sys.stderr, 'crc64 value in python (hex): ' + hex(crc_py)
        print >>sys.stderr, 'crc64 value in c++ (hex): ' + hex_crc_cpp
        print >>sys.stderr, 'source data (base64): ' + based64_data
        raise Exception(error_header)


def test_random():
    random.seed()
    runner = common.execute(common.binary_path("extsearch/images/robot/library/calcimgsignature/ut2/sample/sample"),
                            stdin=subprocess.PIPE, stdout=subprocess.PIPE, check_exit_code=True, wait=False)
    compare_results(runner, '', 0xffffffffffffffff)
    compare_results(runner, 'Hello!', 0x21403426df8da0dc)
    for i in range(10):
        compare_results(runner, random_data())
    runner._process.stdin.close()
    runner.wait()
