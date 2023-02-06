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


def map(params, client):

    @yt.with_context
    def generate_postalcodes(row, context):
        parent_companies = row.get('parent_companies')
        if parent_companies:
            parent = parent_companies[0]
            parent_company_id = parent.get('company_id')
            if parent_company_id == 72141 and row.get('publishing_status') == 'publish':
                names = row.get('names')
                address = row.get('address')
                formatted_address = address.get('formatted').get('value')
                numbers = set()
                for name in names:
                    #if name.get('type') == 'main' and name.get('value').get('locale') == 'ru':
                    if name.get('value').get('locale') == 'ru':
                        office = name.get('value').get('value')
                        num = office.rsplit(' ', 2)[-1]
                        if num.isdigit() and len(num) == 6:
                            numbers.add(num)
                for num in numbers:
                    data = {'Url': '{code}~ru'.format(code=num),
                            'permalink': row.get('permalink'),
                            params.get('snippet_name'): json.dumps({'postalCodeNumber': num,
                                                                    'postOffice': formatted_address,
                                                                    'postOfficeGeocoded': formatted_address},
                                                                   ensure_ascii=False)}
                    yield data

    client.run_map(generate_postalcodes,
                   yt.TablePath(params.get('pre_processing_out') or params.get('input_table'),
                                columns=['permalink', 'parent_companies', 'names', 'address', 'publishing_status']),
                   params.get('generating_out') or params.get('processing_out'),
                   format=yt.JsonFormat(control_attributes_mode="row_fields",
                                        attributes={'encode_utf8': False}))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Postalcodes snippets mapper')
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
