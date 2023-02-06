#!/usr/bin/env python
# -*- coding: utf-8 -*

import os
import sys
import json
import argparse
import requests
from lxml import etree
import yt.wrapper as yt

sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'system'))
import misc
import xml_validator
from basic_mapper import BasicSnippetMapper


class RealtyMapper(BasicSnippetMapper):

    def __call__(self, row):
        data =  {'Url': row.get('key'),
                 self.params.get('snippet_name'): row.get('value')}
        try:
            if self.validator.validate(xml_validator.parse(row.get('value'))):
                data.update({'@table_index': 0})
            else:
                data.update({'errors': str(self.validator.error_log),
                             '@table_index': 1})
            yield data
        except etree.XMLSyntaxError as err:
            data.update({'errors': 'Could not parse xml: {error}'.format(error=err),
                         '@table_index': 1})
            yield data

def map(params, client):
    if params.get('schema'):
        files = [params.get('schema')]
    else:
        files = []
    validation_errors = params.get('error_log')
    client.run_map(RealtyMapper(params),
                   params.get('pre_processing_out'),
                   [params.get('generating_out') or params.get('processing_out'),
                    validation_errors],
                   format=yt.JsonFormat(control_attributes_mode="row_fields"),
                   local_files=files)
    client.set_attribute(params.get('generating_out') or params.get('processing_out'),
                         'expiration_time',
                         misc.get_ttl(params.get('yt_ttl', 1)))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Generates realty snippets')
    parser.add_argument('--cluster',type=str, help='YT cluster')
    parser.add_argument('--parameters',
                        type=str,
                        help='Dict with job parameters')
    args = parser.parse_args()
    yt_client = misc.get_client(os.environ['YT_TOKEN'],
                                os.environ['YT_POOL'],
                                args.cluster,
                                generation_stage=True)
    params = json.loads(args.parameters)
    map(params, yt_client)
