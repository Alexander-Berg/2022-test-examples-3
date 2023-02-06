import os
import shutil
import subprocess
import tempfile

import yatest

import common

from fml_utils import MAKE_FILE_NAME

UPLOAD_FML = yatest.common.source_path(os.path.join(common.RD_PREFIX, 'upload_fml.py'))
UPDATE_MAKE_FILE = yatest.common.source_path(os.path.join(common.RD_PREFIX, 'update_make_file.py'))
PROLOGUE = ['prologue', 'FROM_SANDBOX(123456789 OUT sample.info)', ]
EPILOGUE = ['epilogue']
FAKE_ID = 'FAKE_ID'


def prepare_make_file(dest_dir):
    with open(os.path.join(dest_dir, MAKE_FILE_NAME), "w") as f:
        f.write('\n'.join(PROLOGUE + EPILOGUE) + '\n')


def check_make_file(dest_dir, formula_list):
    added_lines = []
    for fml in formula_list:
        added_lines += ['FROM_SANDBOX(' + FAKE_ID + ' OUT ' + fml + ")"]
    with open(os.path.join(dest_dir, MAKE_FILE_NAME), "r") as f:
        print f.read()
    with open(os.path.join(dest_dir, MAKE_FILE_NAME), "r") as f:
        file_contents = f.read().splitlines()
        assert file_contents == PROLOGUE + added_lines + EPILOGUE


def test_with_extract():
    work_dir = tempfile.mkdtemp()
    try:
        prepare_make_file(work_dir)
        in_fml = os.path.join(common.DATA_PATH, 'realty_ru_desktop_softmax_ck5_3_otrok.xtd')
        subprocess.check_output([UPLOAD_FML, '--dry-run', '--dest-dir', work_dir, '--debug', in_fml], stderr=subprocess.STDOUT)
        check_make_file(work_dir, [
            'realty_ru_desktop_softmax_ck5_3_otrok_sub1.info',
            'realty_ru_desktop_softmax_ck5_3_otrok_sub2.info',
            'realty_ru_desktop_softmax_ck5_3_otrok_sub3.info',
            'realty_ru_desktop_softmax_ck5_3_otrok_sub4.mnmc',
            'realty_ru_desktop_softmax_ck5_3_otrok.xtd',
        ])
    finally:
        shutil.rmtree(work_dir)


def test_without_extract():
    work_dir = tempfile.mkdtemp()
    try:
        prepare_make_file(work_dir)
        in_fml = os.path.join(common.DATA_PATH, 'realty_ru_desktop_softmax_ck5_3_otrok.xtd')
        subprocess.check_output([UPLOAD_FML, '--dry-run', '--dont-extract', '--dest-dir', work_dir, in_fml], stderr=subprocess.STDOUT)
        check_make_file(work_dir, [
            'realty_ru_desktop_softmax_ck5_3_otrok.xtd',
        ])
    finally:
        shutil.rmtree(work_dir)


def test_update_make_file():
    work_dir = tempfile.mkdtemp()
    try:
        prepare_make_file(work_dir)
        in_fml = 'realty_ru_desktop_softmax_ck5_3_otrok.xtd'
        subprocess.check_output([UPDATE_MAKE_FILE, 'add', '-c', os.path.join(work_dir, MAKE_FILE_NAME), '-r', FAKE_ID, '-f', in_fml])
        check_make_file(work_dir, [
            'realty_ru_desktop_softmax_ck5_3_otrok.xtd',
        ])
    finally:
        shutil.rmtree(work_dir)


def test_update_make_file_same_formula():
    work_dir = tempfile.mkdtemp()
    try:
        prepare_make_file(work_dir)
        in_fml = os.path.join(common.DATA_PATH, 'sample.info')
        subprocess.check_output([UPDATE_MAKE_FILE, 'add', '-c', os.path.join(work_dir, MAKE_FILE_NAME), '-r', '123456789', '-f', in_fml])
        check_make_file(work_dir, [])
    finally:
        shutil.rmtree(work_dir)
