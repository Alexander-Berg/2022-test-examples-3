#!/usr/bin/env python
# -*- coding: utf-8 -*

import os
import re
import sys
import json
import tarfile
import argparse
import yt.wrapper as yt
from datetime import datetime
from lxml import etree
from xml.sax.saxutils import quoteattr

sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'system'))
import misc
import xml_validator
from basic_mapper import BasicSnippetMapper


def gen_rating_field(rating):
    if rating != -1:
        return u'<rating>{rating}</rating>'.format(rating=int(rating))
    return u''


def format_time(time):
    return u'{}'.format(datetime.fromtimestamp(time / 1000.0).isoformat())


def gen_review(review):
    return (u'<Review>'
                u'<id>{id}</id>'
                u'{rating_field}'
                u'<snippet>{text}</snippet>'
                u'<atom:updated>{time}</atom:updated>'
            u'</Review>').format(id=review['id'],
                                rating_field=gen_rating_field(review['rating']),
                                text=quoteattr(review['description']),
                                time=format_time(review['time']))


def gen_snippet(reviews):
    return (u'<BusinessReviews '
                u'xmlns="http://maps.yandex.ru/snippets/businessreviews/1.x" '
                u'xmlns:atom="http://www.w3.org/2005/Atom">'
                u'{reviews_data}'
            u'</BusinessReviews>').format(reviews_data=u''.join(gen_review(review) for review in reviews))


class BusinessReviewsReducer(BasicSnippetMapper):

    def __call__(self, key, rows):
        rgx = re.compile(r'/sprav/(\d+)')
        views = []
        permalink = None
        for row in rows:
            result = rgx.search(row['object_id'])
            if result:
                permalink = result.group(1)
                body = row['yson']
                if body['Type'] == '/ugc/review' and body.get('Id') and body.get('Text'):
                    views.append({'id': body['Id'],
                                  'rating': body.get('Rating', {'Val': -1})['Val'],
                                  'description': body['Text'],
                                  'time': long(body['Time'])})
        if permalink and views:
            snippet_text = gen_snippet(sorted(views,
                                       reverse=True,
                                       key=lambda k: k['time'])[:3])    # Only 3 last reviews
            data = {'Url': str(permalink),
                    self.params.get('snippet_name'): snippet_text}
            try:
                if self.validator.validate(xml_validator.parse(snippet_text)):
                    data.update({'@table_index': 0})
                else:
                    data.update({'errors': str([error.message.encode('utf-8') for error in self.validator.error_log]),
                                 '@table_index': 1})
                yield data
            except etree.XMLSyntaxError as err:
                data.update({'errors': 'Could not parse xml: {error}'.format(error=err),
                             '@table_index': 1})
                yield data


def reduce(params, client):
    if params.get('schema'):
        files = [params.get('schema')]
    else:
        files = []
    validation_errors = params.get('error_log')
    client.run_reduce(
        BusinessReviewsReducer(params),
        params.get('pre_processing_out') or params.get('input_table'),
        [
            params.get('generating_out') or params.get('processing_out'),
            validation_errors
        ],
        reduce_by=["object_id"],
        format=yt.JsonFormat(control_attributes_mode="row_fields"),
        local_files=files
    )
    client.set_attribute(params.get('generating_out') or params.get('processing_out'),
                         'expiration_time',
                         misc.get_ttl(params.get('yt_ttl', 1)))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Generates businessreviews snippets')
    parser.add_argument('--cluster', type=str, help='YT cluster')
    parser.add_argument('--parameters',
                        type=str,
                        help='Dict with job parameters')
    args = parser.parse_args()
    params = json.loads(args.parameters)
    yt_client = misc.get_client(os.environ['YT_TOKEN'],
                                os.environ.get('YT_POOL') or '',
                                args.cluster,
                                generation_stage=True)
    reduce(params, yt_client)
