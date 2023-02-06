import yatest.common as yc
import subprocess
import os
import sys


def do_test(bin_name):
    print >> sys.stderr, '=== START of ' + bin_name + ' test ==='
    path = yc.binary_path(os.path.join('search/cache/test', bin_name, bin_name))
    retcode = subprocess.call([path], shell=False)
    print >> sys.stderr, '=== END of ' + bin_name + ' test ==='
    assert retcode == 0


def test_with_lfalloc():
    do_test('with_lfalloc')


def test_with_balloc():
    do_test('with_balloc')


def test_with_jemalloc():
    do_test('with_jemalloc')


def test_with_sysalloc():
    do_test('with_sysalloc')
