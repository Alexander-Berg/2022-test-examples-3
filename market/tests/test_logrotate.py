# -*- coding: utf-8 -*-
import os

import market.pylibrary.logrotate as logrotate


def test_logrotate_utils():
    name = 'copybases'
    generation_name = '20131004_1423'
    filename = '%s_%s.123.log' % (name, generation_name)
    assert 123 == logrotate._get_part_number(filename, name, generation_name)


def test_logrotate_and_crop(tmpdir):
    testfiles = [
        'T_20131003_1906.log',
        'T_20131003_1907.0.log',
        'T_20131003_1907.1.log',
        'T_20131003_1907.2.log',
        'T_20131003_1908.log',
        'T_20131003_1909.log',
        'T_20131003_1910.log',
        'T.log'
    ]
    for tf in testfiles:
        tf_path = tmpdir / tf
        tf_path.ensure()

    logrotate.rename_and_archive_log('T.log', str(tmpdir), '20131003_1919')
    assert (tmpdir / 'T_20131003_1919.log').check(exists=1)

    file_path = tmpdir / 'T.log'
    file_path.ensure()
    logrotate.rename_and_archive_log('T.log', str(tmpdir), '20131003_1919')

    assert (tmpdir / 'T_20131003_1919.log').check(exists=0)
    assert (tmpdir / 'T_20131003_1919.0.log').check(exists=1)
    assert (tmpdir / 'T_20131003_1919.1.log').check(exists=1)

    file_path = tmpdir / 'T.log'
    file_path.ensure()
    logrotate.rename_and_archive_log('T.log', str(tmpdir), '20131003_1919')
    assert (tmpdir / 'T_20131003_1919.0.log').check(exists=1)
    assert (tmpdir / 'T_20131003_1919.1.log').check(exists=1)
    assert (tmpdir / 'T_20131003_1919.2.log').check(exists=1)

    logrotate.crop_logs('T.log', str(tmpdir), keep=3)

    assert (tmpdir / 'T_20131003_1919.0.log').check(exists=1)
    assert (tmpdir / 'T_20131003_1919.1.log').check(exists=1)
    assert (tmpdir / 'T_20131003_1919.2.log').check(exists=1)
    assert 3 == len(os.listdir(str(tmpdir)))


def test_logrotate_only(tmpdir):
    testfiles = [
        'copybases_20131004_1232.2.log',
        'copybases_20131004_1232.1.log',
        'copybases_20131004_1232.5.log',
        'copybases_20131004_1232.3.log',
        'copybases_20131004_1232.4.log',
        'copybases.log'
    ]
    for tf in testfiles:
        tf_path = tmpdir / tf
        tf_path.ensure()

    logrotate.rename_and_archive_log('copybases.log', str(tmpdir), '20131004_1232')

    assert (tmpdir / 'copybases_20131004_1232.6.log').check(exists=1)
    assert (tmpdir / 'copybases.log').check(exists=0)


def test_logrotate_crop_logs_massindexer(tmpdir):
    testfiles = [
        'massindexer_20131008_1702.log',
        'massindexer_20131008_2105.log',
        'massindexer_20131007_2046.log',
        'massindexer_20131008_0002.log',
        'massindexer_20131008_0325.log',
        'massindexer_20131008_1346.log',
        'massindexer_20131008_1735.log',
        'massindexer_20131008_1020.log',
        'massindexer_20131008_0655.log',
        'massindexer_20131009_0026.log',
        'massindexer.log'
    ]
    for tf in testfiles:
        tf_path = tmpdir / tf
        tf_path.ensure()

    logrotate.rename_and_archive_log('massindexer.log', str(tmpdir), '20131009_0355')
    logrotate.crop_logs('massindexer.log', str(tmpdir), keep=10)

    assert (tmpdir / 'massindexer_20131009_0355.log').check(exists=1)
    assert (tmpdir / 'massindexer_20131007_2046.log').check(exists=0)
