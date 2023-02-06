#  -*- coding: utf-8 -*-
import os
import sys
import json
import argparse
import yt.wrapper as yt

sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'system'))
import misc


def map(params, yt_client):

    def generate_snippets(row):
        drug_id = str(row['id'])
        drug_yandex_id = "drug-yandex-" + drug_id

        drug_data = {
            'id': drug_yandex_id,
            'name': row['name'],
        }

        fields_mapping = {
            'dosageForm': 'dosage_form',
            'dosage': 'dosage',
            'quantityOfUnitsInSecondaryPack': 'quantity_of_units_in_secondary_pack',
            'unitName': 'unit_name',
            'country': 'country',
            'manufacturer': 'manufacturer',
            'photoLink': 'picture',
        }

        for snippet_field, table_field in fields_mapping.iteritems():
            if row[table_field] is not None:
                drug_data[snippet_field] = row[table_field]

        if row['inn'] is not None:
            drug_data['inn'] = row['inn'].split('+')

        yield {'Url': drug_yandex_id,
               params['snippet_name']: json.dumps(drug_data),
               '@table_index': 0}

        drug_data['statistics'] = []
        drug_data['id'] = drug_id
        yield {'Url': 'drug-' + drug_id,
               params['snippet_name']: json.dumps(drug_data),
               '@table_index': 0}


    yt_client.run_map(binary=generate_snippets,
                      source_table=params.get('pre_processing_out'),
                      destination_table=params.get('generating_out') or params.get('processing_out'),
                      format=yt.JsonFormat(control_attributes_mode='row_fields',
                                           attributes={'encode_utf8': False}))

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Generates drug_cart_data snippets')
    parser.add_argument('--cluster', type=str, help='YT cluster')
    parser.add_argument('--parameters', type=str, help='Dict with job parameters')
    args = parser.parse_args()
    params = json.loads(args.parameters)
    yt_client = misc.get_client(os.environ['YT_TOKEN'],
                                os.environ.get('YT_POOL') or '',
                                args.cluster)
    map(params, yt_client)
