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


def gen_snippets(params, client):

    @yt.with_context
    def generate(key, rows, context):
        altay_row = {}
        ugc_row = {}
        for row in rows:
            if row['@table_index'] == 0:
                ugc_row = row
            if row['@table_index'] == 1:
                altay_row = row
        if altay_row and ugc_row:
            rating_field = altay_row.get('rating', {})
            if rating_field:
                rating = rating_field.get('rating', 0.0) / 2.0
                if params.get('snippet_name') == 'businessrating/2.x':
                    rating = rating_field.get('rating', 0.0) / 1.0
                amount = rating_field.get('amount', 0)
            else:
                rating = 0.0
                amount = 0
            reviews = ugc_row.get('count')
            if filter(bool, [rating, amount, reviews]):
                score_text = '<score>{rating}</score>'.format(rating=round(rating, 1)) if rating else ''
                ratings_text = '<ratings>{rating_count}</ratings>'.format(rating_count=amount)
                reviews_text = '<reviews>{reviews}</reviews>'.format(reviews=reviews)
                snippet_text = ('<BusinessRating xmlns="http://maps.yandex.ru/snippets/{snpt_name}">'
                                    '{score}'
                                    '{ratings}'
                                    '{reviews}'
                                '</BusinessRating>').format(snpt_name=params.get('snippet_name'),
                                                            score=score_text,
                                                            ratings=ratings_text,
                                                            reviews=reviews_text)
                data = {'key': ugc_row.get('permalink'),
                        'value': snippet_text}
                yield data

    reviews_count_name = '{name}_{rnd_string}'.format(name=os.path.basename(params.get('input_table')),
                                                      rnd_string=uuid4())
    reviews_count_path = os.path.join('//tmp', reviews_count_name)
    altay_ratings_name = '{name}_{rnd_string}'.format(name=os.path.basename(params['temp_tables'].get('altay_companies')),
                                                      rnd_string=uuid4())
    altay_ratings_path = os.path.join('//tmp', altay_ratings_name)
    client.copy(params.get('input_table'), reviews_count_path, force=True)
    client.copy(params['temp_tables'].get('altay_companies'), altay_ratings_path, force=True)
    client.run_sort(reviews_count_path, sort_by=['permalink'])
    client.run_reduce(generate,
                      [reviews_count_path, altay_ratings_path],
                      params.get('pre_processing_out'),
                      format=yt.JsonFormat(control_attributes_mode="row_fields"),
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
    gen_snippets(params, yt_client)
