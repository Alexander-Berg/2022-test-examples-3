#!/usr/bin/env python
# -*- coding: utf-8 -*

import os
import sys
import json
import tarfile
import argparse
import yt.wrapper as yt

sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'system'))
import misc
import add_timestamp
import xml_validator
from basic_mapper import BasicSnippetMapper


class Mapper(BasicSnippetMapper):

    def __call__(self, row):
        if self.is_row_banned(row):
            return
        if self.params['format'] == 'xml':
            value = self._get_value(row)
            if value:
                data = self._gen_key(row)
                snippet_text = misc.xml_snippet_body(self.params['xml_root_name'],
                                                     misc.gen_xml(value,
                                                                  self.params['xml_root_name']))
                if self.params.get('validate'):
                    if self.validator.validate(xml_validator.parse(snippet_text)):
                        data['value'] = misc.format_snippet(self.params['snippet_name'],
                                                            snippet_text)
                        data.update({'@table_index': 0,
                                     self.params['snippet_name']: snippet_text})
                        yield data
                    else:
                        data.update({'errors': str(self.validator.error_log)})
                        data.update({'@table_index': 1})
                        yield data
                else:
                    data.update({'@table_index': 0,
                                 self.params['snippet_name']: snippet_text})
                    yield data
        elif self.params['format'] == 'flat':
            if row.get(self.params['data_field']):
                value_list = row.get(self.params['data_field']).split('\n')
                value = ''.join([string.strip() for string in value_list])
                value = value.replace("<?xml version='1.0' encoding='utf8'?>", '')
                data = self._gen_key(row)
                if self.params.get('validate'):
                    if self.validator.validate(xml_validator.parse(value)):
                        data.update({'@table_index': 0,
                                     self.params['snippet_name']: value})
                        yield data
                    else:
                        data.update({'errors': str(self.validator.error_log)})
                        data.update({'@table_index': 1})
                        yield data
                else:
                    data.update({'@table_index': 0,
                                 self.params['snippet_name']: value})
                    yield data
        elif self.params['format'] == 'raw':
            value = self._get_value(row)
            if value:
                data = self._gen_key(row)
                data.update({self.params['snippet_name']: value})
            data.update({'@table_index': 0})
            yield data
        else:
            row_size = self._get_size(row)
            if self.params.get('single_snippet_size_limit', 1048576) >= row_size:
                raw_value = self._get_value(row)
                if raw_value:
                    data = self._gen_key(row)
                    data.update({self.params['snippet_name']: json.dumps(self._get_value(row), ensure_ascii=False)})
                    if self.validator.is_valid(raw_value):
                        data.update({'@table_index': 0})
                        yield data
                    else:
                        data.update({'errors': [str(err) for err in  self.validator.iter_errors(raw_value)],
                                     '@table_index': 1})
                        yield data
            else:
                data = self._gen_key(row)
                error = ('Row size {size} is greater than '
                         '"single_snippet_size_limit": {limit}').format(size=row_size,
                                                                        limit=self.params.get('single_snippet_size_limit'))
                data.update({'errors': error,
                             '@table_index': 1})
                yield data
        if self.params.get('make_sprav_feed'):
            feed_row = self._gen_feed_row(row)
            feed_row.update({'@table_index': 2})
            yield feed_row


def _make_columns(params):
    return filter(
        bool,
        [params.get('permalink_field') or params.get('original_id_field'),
         params.get('data_field'),
         params.get('feed_original_id_field')]
    )


def map(params, client):
    files = []
    if params.get('schema'):
        files.append(params.get('schema'))
    if os.path.exists(params.get('banlist')):
        files.append(params.get('banlist'))
    validation_errors = params.get('error_log')
    if params.get('encode_utf'):
        output_format = yt.JsonFormat(control_attributes_mode="row_fields")
    else:
        output_format = yt.JsonFormat(control_attributes_mode="row_fields",
                                      attributes={'encode_utf8': False})
    output_tables = [
        params.get('generating_out') or params.get('processing_out'),
        validation_errors
    ]
    if params.get('sprav_feed_path'):
        output_tables.append(params.get('sprav_feed_path'))
    client.run_map(Mapper(params, client),
                   yt.TablePath(params.get('pre_processing_out') or params.get('input_table'),
                                columns=_make_columns(params),
                                start_index=0,
                                end_index=params.get('number_of_snippets')),
                   output_tables,
                   format=output_format,
                   local_files=files,
                   spec=misc.get_job_spec(params.get('cluster')))
    client.set_attribute(params.get('generating_out') or params.get('processing_out'),
                         'expiration_time',
                         misc.get_ttl(params.get('yt_ttl', 1)))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Addrs snippets mapper')
    parser.add_argument('--cluster', type=str, help='YT cluster')
    parser.add_argument('--parameters',
                        type=str,
                        help='Dict with job parameters')
    args = parser.parse_args()
    yt_client = misc.get_client(os.environ['YT_TOKEN'],
                                os.environ.get('YT_POOL'),
                                args.cluster,
                                generation_stage=True)
    params = json.loads(args.parameters)
    map(params, yt_client)
