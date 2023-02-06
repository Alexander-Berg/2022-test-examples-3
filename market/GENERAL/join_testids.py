import argparse

import pandas as pd
import offline_basket
from utils import TSV_OUT_PARAMS, NONE_POS


def read_joined_tsv(input_file):
    data = pd.read_csv(input_file, sep='\t', encoding='utf8')

    if (data.dtypes == 'object').all():
        # drop headers from concatenated tsv files
        data = data[data.daas_id != 'daas_id']
        for i in data:
            data[i] = pd.to_numeric(data[i], errors='ignore')
    return data.reset_index(drop=True)


def filter_data_for_toloka(df):
    toloka_filter = (~df.basket.str.contains('CountBasket')) & (df.res_url != '-')
    df['toloka_id'] = NONE_POS
    df.loc[toloka_filter, 'toloka_id'] = toloka_filter[toloka_filter].index

    toloka_cols = ['query_text', 'reg_id', 'res_url', 'url_type', 'toloka_id']

    if ('hid' in df.columns):
        toloka_cols.append('hid')

    toloka_data = df.loc[toloka_filter, toloka_cols].copy()
    if len(toloka_data.columns) == 5:
        toloka_data.columns = ['query', 'rids', 'url_field', 'url_type', 'id']
    else:
        toloka_data.columns = ['query', 'rids', 'url_field', 'url_type', 'id', 'hid']

    toloka_data.rids = toloka_data.rids.astype(str)
    return toloka_data


def main(args):
    data = read_joined_tsv(args.input1)
    if args.basket_size_limit:
        basket_size_limit_ = int(args.basket_size_limit)
    else:
        basket_size_limit_ = 1000

    if args.doc_type:
        doc_type = args.doc_type
    else:
        doc_type = 'all'

    baskets = offline_basket.get_basket_collection(data, args.place, sample_limit=basket_size_limit_, doc_type_=doc_type)

    basket_df = offline_basket.BasketIo.basket_collection_to_df(baskets)

    if not args.ignore_toloka:
        toloka_data = filter_data_for_toloka(basket_df)
    else:
        toloka_data = pd.Series([])

    basket_df.to_csv(args.output1, **TSV_OUT_PARAMS)
    toloka_data.to_json(args.output2, orient='records', force_ascii=False)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(add_help=True)
    parser.add_argument('-i', dest='input1', required=True, help='joined serpsets before toloka assessment')
    parser.add_argument('-place', dest='place', required=True, help='offline place',
                        choices=['serp_all', 'backend', 'serp_adg', 'serp_wiz'])
    parser.add_argument('--ignore', dest='ignore_toloka', help='do not send data for toloka assessment',
                        action='store_true')
    parser.add_argument('-stats', dest='stats', help='kind of statistics used',
                        choices=['independent'], default='independent')
    parser.add_argument('-o1', dest='output1', required=True, help='basket serpsets')
    parser.add_argument('-o2', dest='output2', required=True, help='json for toloka assessment')
    parser.add_argument('--basket_size_limit', default=None, dest='basket_size_limit', required=False, help='maximum size for each basket')
    parser.add_argument('--doc_type', default=None, dest='doc_type', required=False,
                        help='specify document type for WizardRelevance basket')
    args = parser.parse_args()
    main(args)
