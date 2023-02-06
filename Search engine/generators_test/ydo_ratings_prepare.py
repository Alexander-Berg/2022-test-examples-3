#!/usr/bin/env python
# -*- coding: utf-8 -

import os
import sys
import json
import argparse
import yt.wrapper as yt
from uuid import uuid4

sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'system'))
import misc


def map_to_int64(params, client, input_table, output_table):

    def int64(row):
        yield {'permalink': int(row['permalink']),
               'count': row['count']}

    client.run_map(int64,
                   input_table,
                   output_table,
                   format=yt.JsonFormat())


def gen_snippets(params, client):

    @yt.reduce_aggregator
    class PrepareAvgRatings(object):

        def __call__(self, row_groups):
            for key, rows in row_groups:
                data = {}
                data.update(key)
                summ_rating = 0
                rating_count = 0
                for row in rows:
                    summ_rating += row.get('rating')
                    rating_count += 1
                data.update({'rating': summ_rating / rating_count})
                yield data

    @yt.with_context
    def generate(key, rows, context):
        altay_row = {}
        ugc_row = {}
        reviews_row = {}
        for row in rows:
            if row['@table_index'] == 0:
                ugc_row = row
            if row['@table_index'] == 1:
                altay_row = row
            if row['@table_index'] == 2:
                reviews_row = row
        if altay_row and ugc_row and reviews_row:
            rating_field = altay_row.get('rating', {})
            #rating = rating_field.get('rating', 0.0) / 2.0
            if rating_field:
                amount = rating_field.get('amount', 0)
            else:
                amount = 0
            snippet = {'score': ugc_row.get('rating', 0),
                       'ratings': amount,
                       'reviews': reviews_row.get('count', 0)}
            data = {'key': ugc_row.get('permalink'),
                    'value': snippet}
            yield data

    tmp_reviews_count_name = '{name}_{rnd_string}'.format(name=os.path.basename(params.get('input_table')),
                                                      rnd_string=uuid4())
    tmp_reviews_count_path = os.path.join('//tmp', tmp_reviews_count_name)
    client.copy(params.get('input_table'), tmp_reviews_count_path, force=True)
    client.run_sort(tmp_reviews_count_path, sort_by=['permalink'])
    reviews_count_name = '{name}_{rnd_string}'.format(name=os.path.basename(params.get('input_table')),
                                                      rnd_string=uuid4())
    reviews_count_path = os.path.join('//tmp', reviews_count_name)

    client.run_reduce(
        PrepareAvgRatings(),
        yt.TablePath(tmp_reviews_count_path, columns=['permalink', 'rating']),
        reviews_count_path,
        reduce_by=['permalink'],
        format=yt.JsonFormat()

    )

    altay_ratings_name = '{name}_{rnd_string}'.format(name=os.path.basename(params['temp_tables'].get('altay_companies')),
                                                      rnd_string=uuid4())
    altay_ratings_path = os.path.join('//tmp', altay_ratings_name)
    client.copy(params['temp_tables'].get('altay_companies'), altay_ratings_path, force=True)

    client.run_sort(reviews_count_path, sort_by=['permalink'])
    client.run_sort(altay_ratings_path, sort_by=['permalink'])

    reviews_name = '{name}_{rnd_string}'.format(name=os.path.basename(params['temp_tables'].get('reviews_table')),
                                                      rnd_string=uuid4())
    reviews_path = os.path.join('//tmp', reviews_name)
    map_to_int64(params, client, params['temp_tables'].get('reviews_table'), reviews_path)
    client.run_sort(reviews_path, sort_by=['permalink'])

    client.run_reduce(generate,
                      [reviews_count_path,
                       altay_ratings_path,
                       reviews_path],
                      params.get('pre_processing_out'),
                      format=yt.JsonFormat(control_attributes_mode="row_fields"),
                      reduce_by=['permalink'])


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='YDO ratings snippets mapper')
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
