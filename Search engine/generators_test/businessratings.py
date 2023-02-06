#!/usr/bin/env python
# -*- coding: utf-8 -*

import os
import sys
import json
import argparse
import yt.wrapper as yt

sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'system'))
import misc
import xml_validator
from basic_mapper import BasicSnippetMapper


@yt.with_context
class Mapper(BasicSnippetMapper):

    def __call__(self, key, rows, context):
        altay_row = {}
        ugc_row = {}
        for row in rows:
            if row['@table_index'] == 0:
                ugc_row = row
            if row['@table_index'] == 1:
                altay_row = row
        if altay_row and ugc_row:
            if altay_row.get('is_exported'):
                rating_field = altay_row.get('rating')
                if rating_field:
                    if self.params.get('snippet_name') == 'businessrating/1.x':
                        rating = rating_field.get('rating') / 2.0
                    else:
                        rating = rating_field.get('rating') / 1.0
                    snippet_text = ('<BusinessRating xmlns="http://maps.yandex.ru/snippets/businessrating/1.x">'
                                        '<score>{rating}</score>'
                                        '<ratings>{rating_count}</ratings>'
                                        '<reviews>{reviews}</reviews>'
                                    '</BusinessRating>').format(rating=round(rating, 1),
                                                                rating_count=rating_field.get('amount'),
                                                                reviews=ugc_row.get('count'))
                    if self.params.get('validate'):
                        if self.validator.validate(xml_validator.parse(snippet_text)):
                            data = {'@table_index': 2,
                                    'key': ugc_row.get('permalink'),
                                    self.params.get('snippet_name'): snippet_text}
                            yield data
                        else:
                            data.update({'errors': str(self.__call__validator.error_log)})
                            data.update({'@table_index': 3})
                            yield data


def reduce(params, client):
    if params.get('schema'):
        files = [params.get('schema')]
    else:
        files = []
    validation_errors = params.get('error_log')
    client.run_map(Mapper(params),
                      #[params.get('pre') or params.get('input_table'),
                      source_table=[params.get('pre') or '//tmp/org_reviews_count',
                                    params['temp_tables'].get('altay_companies')],
                      destination_table=[params.get('generating_out') or params.get('processing_out'),
                                         validation_errors],
                      format=yt.JsonFormat(control_attributes_mode="row_fields"),
                      local_files=files,
                      reduce_by=['permalink'])


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Businessratings snippets mapper')
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
    yt_client.copy(params.get('input_table'), '//tmp/org_reviews_count', force=True)
    yt_client.run_sort('//tmp/org_reviews_count',
                       sort_by=['permalink'])
    reduce(params, yt_client)
