#  -*- coding: utf-8 -*-
import os
import sys
import json
import argparse
import yt.wrapper as yt
from datetime import datetime

sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'system'))
import misc
from basic_mapper import BasicSnippetMapper


PROVIDER_NAME = 'booking'
SNIPPET_NAME = 'booking_advert/1.x'


class BookingAdvertMapper(BasicSnippetMapper):

    def __call__(self, row):
        body = {
            'date_range': {
                'check_in_date': datetime.strptime(row['check-in-date'],
                                                   '%d.%m.%Y').isoformat(),
                'check_out_date': datetime.strptime(row['check-out-date'],
                                                    '%d.%m.%Y').isoformat()
            },
            'price': {
                'value': row['price'],
                'text': '{price} {currency_name}'.format(price=row['price'],
                                                         currency_name=misc.LOCALIZED_CURRENCIES.get(row['currency'], row['currency'])),
                'currency': row['currency']
            }
        }
        data = {'key': '{provider_name}~{company_id}'.format(provider_name=PROVIDER_NAME,
                                                             company_id=row['company-id']),
                'value': '{snippet_name}={body}'.format(snippet_name=SNIPPET_NAME,
                                                        body=json.dumps(body))}
        if self.validator.is_valid(body):
            data.update({'@table_index': 0})
        else:
            data.update({'errors': [str(err) for err in  self.validator.iter_errors(body)],
                         '@table_index': 1})
        yield data


def map(params, client):
    if params.get('schema'):
        files = [params.get('schema')]
    else:
        files = []
    validation_errors = params.get('error_log')
    client.run_map(BookingAdvertMapper(params),
                   params.get('pre_processing_out') or params.get('input_table'),
                   [params.get('generating_out') or params.get('processing_out'),
                    validation_errors],
                   job_count=20,
                   format=yt.JsonFormat(control_attributes_mode="row_fields",
                                        attributes={'encode_utf8': False}),
                   local_files=files)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Generates booking snippets')
    parser.add_argument('--cluster', type=str, help='YT cluster')
    parser.add_argument('--parameters', type=str, help='Dict with job parameters')
    args = parser.parse_args()
    params = json.loads(args.parameters)
    yt_client = misc.get_client(os.environ['YT_TOKEN'],
                                args.cluster,
                                generation_stage=True)
    map(params, yt_client)
