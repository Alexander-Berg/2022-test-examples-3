#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
import logging
import os
import re
import shutil
import subprocess
import tempfile


ARCHIVE_SUFFIX = '.tar.zstd_10'


def setup_logging():
    class ColoredFormatter(logging.Formatter):
        RED = '\033[1;31m'
        YELLOW = '\033[1;33m'
        RESET = '\033[0;0m'

        def __init__(self, fmt):
            logging.Formatter.__init__(self, fmt)

        def format(self, record):
            if record.levelname == logging.getLevelName(logging.WARNING):
                color = self.YELLOW
            elif record.levelname == logging.getLevelName(logging.ERROR):
                color = self.RED
            else:
                color = None
            text = logging.Formatter.format(self, record)
            if color:
                text = color + text + self.RESET
            return text

    log_formatter = ColoredFormatter('%(asctime)s [%(levelname)-5.5s]  %(message)s')
    root_logger = logging.getLogger()
    root_logger.setLevel(logging.INFO)

    console_handler = logging.StreamHandler()
    console_handler.setFormatter(log_formatter)
    root_logger.addHandler(console_handler)


class TempDir(object):

    def __init__(self):
        self.temp_dir = None

    def __enter__(self):
        self.temp_dir = tempfile.mkdtemp()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        shutil.rmtree(self.temp_dir)

    @property
    def name(self):
        return self.temp_dir


def validate_patch_dir(patch_dir):
    DIST_RE = [
        r'^book-part-\d$',
        r'^model-part-\d$',
        r'^search-cards$',
        r'^search-part-\d{1,2}$',
        r'^search-part-blue-\d$',
        r'^search-report-data$',
        r'^search-stats$',
        r'^search-wizard$',
    ]
    for dir_name in os.listdir(patch_dir):
        sub_dir_path = os.path.join(patch_dir, dir_name)
        if not os.path.isdir(sub_dir_path):
            raise Exception('"{}" is not a directory'.format(sub_dir_path))
        match_found = False
        for dist_re in DIST_RE:
            if re.match(dist_re, dir_name):
                match_found = True
                break
        if not match_found:
            raise Exception('"{}" is not a recognized dist name'.format(sub_dir_path))
        if not os.listdir(os.path.join(patch_dir, dir_name)):
            raise Exception('"{}" is empty'.format(sub_dir_path))


def patch_or_copy_file(source_path, dest_path, uc_binary, patch_dir):
    if source_path.endswith(ARCHIVE_SUFFIX) and patch_dir:
        dist_name = os.path.basename(source_path)[:-len(ARCHIVE_SUFFIX)]
        path_to_patched_files = os.path.join(patch_dir, dist_name)
        if os.path.isdir(path_to_patched_files):
            with TempDir() as temp_dir:
                logging.info('Unpacking archive "%s" into "%s"', source_path, temp_dir.name)
                subprocess.check_call('{} -d -f {} -t {} -x'.format(uc_binary, source_path, temp_dir.name), shell=True)
                for patched_file in os.listdir(path_to_patched_files):
                    if not os.path.isfile(os.path.join(temp_dir.name, patched_file)):
                        raise Exception('Cannot find file "{}" in "{}"'.format(patched_file, source_path))
                    logging.info('Patching file "%s" in "%s"', patched_file, dist_name)
                    shutil.copy(os.path.join(path_to_patched_files, patched_file), temp_dir.name)
                logging.info('Reassembling archive "%s"', dest_path)
                subprocess.check_call('tar cf - * | {} -c -t {} -C zstd_10'.format(uc_binary, dest_path), shell=True, cwd=temp_dir.name)
                return
    logging.info('Copying "%s" -> "%s"', source_path, dest_path)
    shutil.copy(source_path, dest_path)


def main():
    arg_parser = argparse.ArgumentParser(description='Create patched index distribution (e.g. for HP perf tests).')
    arg_parser.add_argument('--dist', required=True, help='Compressed Index path (example: /var/lib/search/marketsearch/20191022_1504)')
    arg_parser.add_argument('--patch-dir', help='Directory with patched files. Files within must reside inside subdirectories named after corresponding distribution archives (e.g. search-part-0, search-stats, etc.)')
    arg_parser.add_argument('--uc', help='Path to uc binary')
    args = arg_parser.parse_args()
    setup_logging()
    try:
        source_path = os.path.normpath(args.dist)
        generation = os.path.basename(source_path)
        generation_match = re.match(r'^(\d{8}_)(\d{4})$', generation)
        if not generation_match:
            raise Exception('Generation "{}" must match yyyymmdd_hhmm pattern'.format(generation))
        patched_generation = '{}{:04}'.format(generation_match.group(1), int(generation_match.group(2)) + 1)
        patched_dist_path = os.path.join(os.path.dirname(source_path), patched_generation)
        if os.path.exists(patched_dist_path):
            logging.warn('Path already exists: "%s"', patched_dist_path)
            shutil.rmtree(patched_dist_path)

        patch_dir = os.path.normpath(args.patch_dir) if args.patch_dir else None
        if patch_dir:
            validate_patch_dir(patch_dir)

        uc_binary = args.uc if args.uc else 'uc'
        try:
            if patch_dir:
                subprocess.check_output([uc_binary, '--help'], stderr=subprocess.STDOUT)
        except Exception as e:
            raise Exception('uc binary is not found: {}'.format(e))

        generation_pattern = re.compile(r'\d{8}_\d{4}')
        for dir_path, _, file_names in os.walk(source_path):
            for file_name in file_names:
                source_path = os.path.join(dir_path, file_name)
                dest_path = re.sub(generation_pattern, patched_generation, source_path)
                dest_dir = os.path.dirname(dest_path)
                if not os.path.isdir(dest_dir):
                    os.makedirs(dest_dir)
                patch_or_copy_file(source_path, dest_path, uc_binary, patch_dir)
    except Exception as e:
        logging.error(e)


if __name__ == '__main__':
    main()
