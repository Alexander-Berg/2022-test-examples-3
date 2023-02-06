#!/usr/bin/env python
# -*- coding: utf-8 -*

import os
import sys
import json
import argparse
import yt.wrapper as yt
from xml.dom import minidom

sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'system'))
import misc


PROVIDER_NAME = 'afisha_ru'

def gen_table(xml_data, snippet_name):
    snippets = []
    xml_doc = minidom.parseString(xml_data)
    compamies = xml_doc.getElementsByTagName('company')
    for company in compamies:
        company_id = misc.get_text(company, 'company_id')
        rating = misc.get_text(company, 'rating')
        snippet = ('{snippet_name}='
                   '<afishaRating>'
                    '{rating}'
                   '</afishaRating>').format(snippet_name=snippet_name,
                                            rating=rating)
        snippets.append({'key': '{provider_name}~{company_id}'.format(provider_name=PROVIDER_NAME,
                                                                      company_id=company_id),
                         'value': snippet})
    return snippets


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Generates Afisha ratings snippets')
    parser.add_argument('--cluster',type=str, help='YT cluster')
    parser.add_argument('--parameters', type=str, help='Dict with job parameters')
    args = parser.parse_args()
    yt_client = misc.get_client(os.environ['YT_TOKEN'], args.cluster)
    params = json.loads(args.parameters)
    xml_data = misc.download(params.get('input_table'))
    snippets = gen_table(xml_data, params.get('snippet_name'))
    with open('/tmp/afisha_ratings', 'w') as f:
        for snippet in snippets:
            f.write(json.dumps(snippet))
    yt_client.write_table(params.get('generating_out') or params.get('processing_out'),
                          snippets,
                          format=yt.JsonFormat(attributes={"encode_utf8": False}))
