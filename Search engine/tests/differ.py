#! /usr/bin/env python
# coding=utf-8

import codecs
import inspect
import json
import os
import sys

# add include directory for directory '...convertor/diff'
diff_directory = os.path.join(os.path.dirname(os.path.dirname(inspect.getfile(inspect.currentframe()))), 'diff')
sys.path.append(diff_directory)

import tests_common

import diff_json
import html_export


class Comparator:
    def __init__(self):
        pass

    def get_out_rlzt_path(self, output_path):
        return os.path.join(output_path, 'output')

    def save_diff(self, diff_file_name, dispersed_files_names_with_canon_md5, subtest_paths, main_paths):
        # usual diff
        tests_common.save_diff(diff_file_name, dispersed_files_names_with_canon_md5, subtest_paths)

        # json diff
        out_rlzt_path = self.get_out_rlzt_path(subtest_paths.output_path())
        base_diff_file_path = os.path.join(subtest_paths.diff_path(), diff_file_name + '.diff')
        diff_file_path = base_diff_file_path + '.json'
        html_diff_file_path = base_diff_file_path + '.html'
        if os.path.exists(diff_file_path):
            os.remove(diff_file_path)
        if os.path.exists(html_diff_file_path):
            os.remove(html_diff_file_path)

        for (file_name, canon_md5) in sorted(dispersed_files_names_with_canon_md5.items()):
            output_result_filepath = os.path.join(out_rlzt_path, file_name)
            if not os.path.exists(output_result_filepath):
                raise OSError('Output file does not exist: {0}'.format(output_result_filepath))
            canon_file_path = tests_common.canon_storage.getFilePath(canon_md5)
            scheme_file_path = os.path.join(diff_directory, 'card-scheme.json')

            output_json = json.load(open(output_result_filepath, 'r'))
            canon_json = json.load(open(canon_file_path, 'r'))
            scheme_json = json.load(open(scheme_file_path, 'r'))
            diff = diff_json.make_diff(scheme_json, canon_json, output_json)
            json.dump(diff, codecs.getwriter('utf8')(open(diff_file_path, 'w')), indent=4)
            # export to html
            exp = html_export.THtmlExporter(diff)
            exp.Print(open(html_diff_file_path, 'w'))

            sys.stdout.write('\tSee json diff ' + diff_file_path + '\n')
            sys.stdout.write('\tSee html diff ' + html_diff_file_path + '\n')
