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


@yt.reduce_aggregator
class Reducer(BasicSnippetMapper):

    def __call__(self, row_groups):
        for key, rows in row_groups:
            data = {}
            for row in rows:
                rubric_id = row.get('rubric_id')
                if rubric_id:
                    data.update(
                        {
                            rubric_id: {
                                'image_url': row.get('image_url'),
                                'title': row.get('title'),
                            }
                        }
                    )
            yield {key: data}


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
    client.copy(params.get('input_table'), temp_input_table, force=True)
    client.run_sort(temp_input_table, sort_by=[params.get('reduce_field')])
    client.run_reduce(
        Reducer(params, client),
        yt.TablePath(temp_input_table),
        [
            yt.TablePath(params.get('generating_out') or params.get('processing_out')),
            yt.TablePath(validation_errors)
        ],
        reduce_by=params.get('reduce_field'),
        format=output_format,
        local_files=files,
        spec=misc.get_job_spec(params.get('cluster'))
    )


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
