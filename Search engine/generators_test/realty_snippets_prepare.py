#!/usr/bin/env python
#  -*- coding: utf-8 -*-

import os
import sys
import json
import argparse
import yt.wrapper as yt

sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'system'))
import misc


def map_similar(row):
    for similar_id in row["similar"]:
        yield {"@table_index": 1,
               "similar_id": similar_id,
               "permalink": row["permalink"],
               "realty_id": row["realty_id"],
               "with_tycoon": row["with_tycoon"]}

    prices = []
    if "prices" in row:
        prices = [
            row["prices"][price_type]["price-from"]
            for price_type in row["prices"]
            if row["prices"][price_type].get("price-from") is not None
        ]
    if len(prices) > 0:
        min_price = min(prices)
    else:
        min_price = None

    yield {
        "@table_index": 0,
        "similar_id": row["realty_id"],
        "data": {
            "name": row["name"],
            "photos": row["photos"],
            "price_from": min_price,
            "address": row["address"],
            "realty_id": row["realty_id"]
        }
    }


@yt.with_context
def reduce_similar(key, rows, context):
    similar_data = None
    lst = list(rows)
    for row in lst:
        if row["@table_index"] == 0:
            similar_data = row["data"]
    if similar_data:
        for row in lst:
            if row["@table_index"] == 1:
                del row["@table_index"]
                row["similar_data"] = similar_data
                yield row


@yt.with_context
def aggregate_similar(key, rows, context):
    result = None
    for row in rows:
        if not result:
            result = row
            result["similars"] = []
        result["similars"].append(row["similar_data"])
    del result["similar_data"]
    yield result


def map_altay_companies(row):
    altay_company = row
    for provider in altay_company.get("providers", []):
        if provider["provider_id"] == 423381598:
            yield {
                "realty_id": int(provider["original_id"]),
                "permalink": row["permalink"],
                "duplicate_permalink": row["permalink"],
                "is_head": row.get("duplicate_company_id") is None
            }


@yt.with_context
def reduce_with_altay(key, rows, context):
    permalink = None
    with_tycoon = False
    lst = list(rows)
    for row in lst:
        if row["@table_index"] == 1:
            permalink = row["permalink"]
            with_tycoon = row["with_tycoon"]
    for row in lst:
        if row["@table_index"] == 0:
            row["permalink"] = permalink
            row["with_tycoon"] = with_tycoon
            yield row


@yt.with_context
def reduce_realty_altay_with_head(key, rows, context):
    lst = list(rows)
    head_permalink = None
    for row in lst:
        if row["@table_index"] == 1:
            head_permalink = row["company_permalink"]
    for row in lst:
        if row["@table_index"] == 0:
            if not row["is_head"]:
                row["permalink"] = head_permalink
            del row["duplicate_permalink"]
            del row["is_head"]
            yield row


@yt.with_context
def reduce_tycoon_with_head(key, rows, context):
    lst = list(rows)
    head_permalink = None
    for row in lst:
        if row["@table_index"] == 1:
            head_permalink = row["company_permalink"]
    for row in lst:
        if row["@table_index"] == 0:
            if head_permalink is not None:
                yield {
                    "permalink": head_permalink
                }
            else:
                yield {
                    "permalink": row["duplicate_permalink"]
                }


@yt.with_context
def reduce_altay_with_tycoon(key, rows, context):
    lst = list(rows)
    with_tycoon = False
    for row in lst:
        if row["@table_index"] == 1:
            with_tycoon = True
    for row in lst:
        if row["@table_index"] == 0:
            yield {
                "permalink": row["permalink"],
                "realty_id": row["realty_id"],
                "with_tycoon": with_tycoon
            }


def map_tycoon(row):
    if row["for_export"]:
        yield {
            "duplicate_permalink": row["company_permanent_id"]
        }


def find_company(row):
    if row["key"] == "realty_yandex~166185":
        yield row


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Prepare realty snippets data')
    parser.add_argument('--cluster',type=str, help='YT cluster')
    parser.add_argument('--parameters', type=str, help='Dict with job parameters')
    args = parser.parse_args()
    yt_client = misc.get_client(os.environ['YT_TOKEN'],
                                os.environ['YT_POOL'],
                                args.cluster)
    params = json.loads(args.parameters)
    yt_client.run_map(map_altay_companies,
                      params['temp_tables'].get('altay_companies'),
                      params['temp_tables'].get('realty_altay_companies'),
                      format=yt.JsonFormat())
    yt_client.run_sort(params['temp_tables'].get('realty_altay_companies'),
                       sort_by="duplicate_permalink")
    yt_client.copy(params['temp_tables'].get('altay_duplicate_companies'),
                   params['temp_tables'].get('tmp_altay_duplicate_companies'),
                   force=True)
    yt_client.run_sort(params['temp_tables'].get('tmp_altay_duplicate_companies'),
                       sort_by="duplicate_permalink")
    yt_client.run_reduce(reduce_realty_altay_with_head,
                         [params['temp_tables'].get('realty_altay_companies'),
                          params['temp_tables'].get('tmp_altay_duplicate_companies')],
                         params['temp_tables'].get('tmp_altay_duplicate_companies2'),
                         format=yt.JsonFormat(control_attributes_mode="row_fields"),
                         reduce_by=["duplicate_permalink"])
    yt_client.run_map(map_tycoon,
                      params['temp_tables'].get('tycoon'),
                      params['temp_tables'].get('tmp_tycoon'),
                      format=yt.JsonFormat())
    yt_client.run_sort(params['temp_tables'].get('tmp_tycoon'),
                       sort_by="duplicate_permalink")
    yt_client.run_reduce(reduce_tycoon_with_head,
                         [params['temp_tables'].get('tmp_tycoon'),
                          params['temp_tables'].get('tmp_altay_duplicate_companies')],
                         params['temp_tables'].get('tmp_tycoon2'),
                         format=yt.JsonFormat(control_attributes_mode="row_fields"),
                         reduce_by=["duplicate_permalink"])
    yt_client.run_sort(params['temp_tables'].get('tmp_tycoon2'),
                       sort_by="permalink")
    yt_client.run_sort(params['temp_tables'].get('tmp_altay_duplicate_companies2'),
                       sort_by="permalink")
    yt_client.run_reduce(reduce_altay_with_tycoon,
                         [params['temp_tables'].get('tmp_altay_duplicate_companies2'),
                          params['temp_tables'].get('tmp_tycoon2')],
                         params['temp_tables'].get('tmp_altay_with_tycoon'),
                         format=yt.JsonFormat(control_attributes_mode="row_fields"),
                         reduce_by=["permalink"])
    yt_client.run_sort(params['temp_tables'].get('tmp_altay_with_tycoon'),
                       sort_by="realty_id")
    yt_client.copy(params.get('input_table'),
                   params['temp_tables'].get('tmp_input'),
                   force=True)
    yt_client.run_sort(params['temp_tables'].get('tmp_input'),
                       sort_by="realty_id")
    yt_client.run_reduce(reduce_with_altay,
                         [params['temp_tables'].get('tmp_input'),
                          params['temp_tables'].get('tmp_altay_with_tycoon')],
                         params['temp_tables'].get('tmp_input_with_altay'),
                         format=yt.JsonFormat(control_attributes_mode="row_fields"),
                         reduce_by=["realty_id"])
    yt_client.run_map(map_similar,
                      params['temp_tables'].get('tmp_input_with_altay'),
                      [params['temp_tables'].get('similar_orig'),
                       params['temp_tables'].get('similar')],
                      format=yt.JsonFormat(control_attributes_mode="row_fields"))
    yt_client.run_sort(params['temp_tables'].get('similar_orig'),
                       sort_by=["similar_id"])
    yt_client.run_sort(params['temp_tables'].get('similar'),
                       sort_by=["similar_id"])
    yt_client.run_reduce(reduce_similar,
                         [params['temp_tables'].get('similar_orig'),
                          params['temp_tables'].get('similar')],
                         params['temp_tables'].get('similar_data'),
                         format=yt.JsonFormat(control_attributes_mode="row_fields"),
                         reduce_by=["similar_id"])
    yt_client.run_sort(params['temp_tables'].get('similar_data'),
                       sort_by=["realty_id"])
    yt_client.run_reduce(aggregate_similar,
                         params['temp_tables'].get('similar_data'),
                         params['temp_tables'].get('prepared_table'),
                         format=yt.JsonFormat(control_attributes_mode="row_fields"),
                         reduce_by=["realty_id"])
