#!/usr/bin/env python
# -*- coding: utf-8 -

import os
import sys
import json
import argparse
import yt.wrapper as yt

sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'system'))
import misc
import add_timestamp
from basic_mapper import BasicSnippetMapper


class RenameColumns(object):

    def __call__(self, row):
        data = row
        data.update({'value': data.get('url')})
        del data['url']
        yield data


#@yt.with_context
@yt.reduce_aggregator
class FilterExported(object):

    def __call__(self, row_groups):
        for key, rows in row_groups:
            url_rows = []
            company_row = None
            for row in rows:
                if row['@table_index'] == 0:
                    url_rows.append(row)
                if row['@table_index'] == 1:
                    company_row = row
            if url_rows and company_row:
                if company_row['is_exported']:
                    for row in url_rows:
                        yield row


@yt.reduce_aggregator
class Reducer(BasicSnippetMapper):

    def __call__(self, row_groups):
        for key, rows in row_groups:
            permalink_rows = []
            sitelinks_row = None
            for row in rows:
                if row['@table_index'] == 0:
                    permalink_rows.append(row)
                if row['@table_index'] == 1:
                    sitelinks_row = row
            if permalink_rows and sitelinks_row:
                raw_value = self._get_value(sitelinks_row)
                if raw_value:
                    for permalink_row in permalink_rows:
                        data = {
                            'permalink': permalink_row.get('permalink'),
                            'sitelinks': raw_value
                            }
                        yield data


def gen_snippets(params, client):
    files = []
    if params.get('schema'):
        files.append(params.get('schema'))
    validation_errors = params.get('error_log')
    if params.get('encode_utf'):
        output_format = yt.JsonFormat(control_attributes_mode="row_fields")
    else:
        output_format = yt.JsonFormat(control_attributes_mode="row_fields",
                                      attributes={'encode_utf8': False})
    temp_input_table = os.path.join('//tmp', os.path.basename(params.get('input_table')))
    #client.run_map(
        #RenameColumns(),
        #params.get('input_table'),
        #temp_input_table,
        #format=yt.JsonFormat()
    #)
    #client.run_sort(temp_input_table, sort_by=['value'])
    temp_company2url_table = os.path.join('//tmp', os.path.basename(params.get('permalink2url_table')))
    client.copy(params.get('permalink2url_table'), temp_company2url_table, force=True)
    client.run_reduce(
        FilterExported(),
        [
            temp_company2url_table,
            yt.TablePath(
                params.get('company_table'),
                columns=['permalink', 'is_exported']
            ),
        ],
        temp_company2url_table,
        reduce_by=params.get('permalink_field'),
        format=output_format,
        spec=misc.get_job_spec(params.get('cluster'))
    )
    client.run_sort(temp_company2url_table, sort_by=['value'])
    #input_tables = [
        #yt.TablePath(
            #temp_company2url_table,
            #columns=[params.get('reduce_field'), params.get('permalink_field')]
        #),
        #yt.TablePath(
            #temp_input_table,
            #columns=[params.get('reduce_field'), params.get('data_field')]
        #)
    #]
    #output_table = [
        #params.get('pre_processing_out'),
    #]
    #client.run_reduce(
        #Reducer(params, client),
        #input_tables,
        #output_table,
        #reduce_by=params.get('reduce_field'),
        #format=output_format,
        #local_files=files,
        #spec=misc.get_job_spec(params.get('cluster'))
    #)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Sitelinks snippets prepare')
    parser.add_argument('--cluster', type=str, help='YT cluster')
    parser.add_argument('--parameters',
                        type=str,
                        help='Dict with job parameters')
    args = parser.parse_args()
    yt_client = misc.get_client(os.environ['YT_TOKEN'],
                                os.environ.get('YT_POOL') or '',
                                args.cluster,
                                generation_stage=True)
    params = json.loads(args.parameters)
    gen_snippets(params, yt_client)
