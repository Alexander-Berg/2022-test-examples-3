#!/usr/bin/env python
#  -*- coding: utf-8 -*-

import os
import sys
import json
import argparse
import yt.wrapper as yt
import yt.yson as yson
from yt.wrapper.common import GB

from edadeal_popular_segments import popular_segments, default_image_urls

sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'system'))
import misc


EDADEAL_URL = 'https://edadeal.ru'


def shop_offer(offer):
    if offer.get('shopsEdadealIds') is not None and offer.get('shopsEdadealIds', 0) != 0:
        for shop_edadeal_id in offer.get('shopsEdadealIds'):
            new_offer = {'dateStart': offer['dateStart'],
                         'dateEnd': offer['dateEnd'],
                         'discount': offer.get('discount', 0),
                         'segment_id': offer['segmentId']}
            yield {'shop_edadeal_id': shop_edadeal_id,
                   'offer': new_offer,
                   'segment_id': offer['segmentId']}


def aggregate_shop_info(key, rows):
    res = {}
    res.update(key)
    offers = list(rows)
    res["shop_offers_count"] = len(offers)
    res["min_discount"] = min((r["offer"].get("discount", 0) for r in offers))
    res["max_discount"] = max((r["offer"].get("discount", 0) for r in offers))
    segments = dict()
    other_segments = dict()
    for offer in offers:
        segment_id = offer["offer"]["segment_id"]
        segment_static_data = popular_segments.get(segment_id)
        if segment_static_data is None:
            segment_static_data = popular_segments.get(offer["parent_segment_id"])

        if segment_static_data is not None:
            segments.update([(segment_static_data["id"], segment_static_data)])
        else:
            other_segments.update([(segment_id, {
                "id": segment_id,
                "name": offer["segment_slug"],
                "name_ru": offer["segment_title"],
                "image_urls": default_image_urls
            })])
    res["segments"] = sorted(
        segments.values(),
        key=lambda x: x["order_num"],
        reverse=False
    ) + other_segments.values()[:(3 - len(segments.values()))]
    res["min_date_start"] = min(
        (r["offer"]["dateStart"] for r in offers)
    )
    res["max_date_end"] = max(
        (r["offer"]["dateEnd"] for r in offers)
    )
    if len(res["segments"]) > 0:
        yield res


@yt.with_context
def build_result(key, rows, context):
    rr = list(rows)
    if len(rr) == 4 \
            and rr[0]["@table_index"] == 0 \
            and rr[1]["@table_index"] == 1 \
            and rr[2]["@table_index"] == 2 \
            and rr[3]["@table_index"] == 3:
        shop_offer_aggregated_row = rr[0]
        shop_wizard_row = rr[1]
        shop_name_row = rr[2]
        shop_chain_info = rr[3]
        res = {}
        res.update(key)
        res.update(shop_offer_aggregated_row)
        if shop_wizard_row.get('catalog_cover_urls') is not None:
            urls = shop_wizard_row["catalog_cover_urls"]
            for url_size in urls:
                urls[url_size] = urls[url_size][6:]
            res["catalog_cover_urls"] = urls
        if shop_wizard_row["shop"].get('shareURL') is not None:
            res["shop_url"] = shop_wizard_row["shop"]["shareURL"]
            res["chain_id"] = shop_wizard_row["shop"]["chainId"]
            res["geo_id"] = shop_wizard_row["shop"]["geoId"]
        res["name"] = shop_name_row["name"]
        res["chain_shops_count"] = shop_chain_info["chain_shops_count"]
        res["edadeal_url"] = EDADEAL_URL
        for segment in res["segments"]:
            segment["url"] = "https://edadeal.ru/{0}/retailers/{1}?segment={2}&source=yandex_wizard".format(
                shop_wizard_row["shop"].get("localitySlug", ""),
                shop_name_row["retailer_slug"],
                segment["name"]
            )
        del res["@table_index"]
        yield {
            "edadeal_id": res["shop_edadeal_id"],
            "value": res,
            "chain_permalink": shop_wizard_row["shop"]["chainId"],
            "geo_id": shop_wizard_row["shop"]["geoId"]
        }


def chain_shop_count(key, rows):
    data = {}
    data.update(key)
    data['count'] = len(list(rows))
    yield data


@yt.with_context
def shop_chain_shop_count(key, rows, context):
    rr = list(rows)
    if rr[0]['@table_index'] == 0:
        chain_info_row = rr[0]
        for shop_row in rr:
            if shop_row['@table_index'] != 0:
                yield {'shop_edadeal_id': shop_row['shop_edadeal_id'],
                       'chain_shops_count': chain_info_row['count']}


def map_chain_permalinks(params, client):

    @yt.with_context
    def reduce(key, rows, context):
        offer_rows = []
        chain_rows = []
        for row in rows:
            if row['@table_index'] == 0:
                offer_rows.append(row)
            if row['@table_index'] == 1:
                chain_rows.append(row)
        for row in offer_rows:
            row.update({'chain_id': chain_rows[0].get('chain_id')})
            yield row

    client.run_reduce(reduce,
                   [params['temp_tables'].get('result_shop_offers'),
                    params['temp_tables'].get('company_to_chain_tmp')],
                   params['temp_tables'].get('offers_altay_companies_tmp'),
                   format=yt.JsonFormat(attributes={"encode_utf8": True},
                                        control_attributes_mode='row_fields'),
                   reduce_by=['chain_permalink'])


def map_altay_companies(params, client):

    @yt.with_context
    def map(row, context):
        altay_company = row
        parent_companies = altay_company.get('parent_companies', [])
        chain_id = None if len(parent_companies) == 0 else parent_companies[0]['company_id']
        geo_id = None
        if altay_company.get('is_exported'):
            if altay_company.get('address') and altay_company['address'].get('geo_id'):
                geo_id = altay_company['address']['geo_id']
            if chain_id is not None and geo_id is not None and row['publishing_status'] == 'publish':
                yield {'@table_index': 0,
                       'permalink': row['permalink'],
                       'chain_id': chain_id,
                       'geo_id': geo_id}

    client.run_map(map,
                   params['temp_tables'].get('altay_companies'),
                   #params['temp_tables'].get('all_companies'),
                   '//tmp/filtered_companies_for_edadeal',
                   format=yt.JsonFormat(attributes={"encode_utf8": True},
                                        control_attributes_mode='row_fields'))


@yt.with_context
def map_altay_companies_to_snippets(key, rows, context):
    rr = list(rows)
    if len(rr) == 2:
        snippet_info = rr[0]
        company_info = rr[1]
        if snippet_info['@table_index'] == 0 and company_info['@table_index'] == 1:
            snippet_info.update(company_info)
            snippet_info['@table_index'] = 0
            yield snippet_info


def chain_offers(params, client):

    @yt.with_context
    def reduce(key, rows, context):
        rr = list(rows)
        snippets = []
        companies = []
        for row in rr:
            if row['@table_index'] == 0:
                snippets.append(row)
            if row['@table_index'] == 1:
                companies.append(row)
        if len(snippets) > 0 and len(snippets[0]['value']['segments']) > 0:
            for company in companies:
                yield {'key': str(company['permalink']),
                       'value': misc.format_snippet(params.get('snippet_name'),
                                                    json.dumps(snippets[0]['value']))}

    client.run_reduce(reduce,
                      [params['temp_tables'].get('offers_altay_companies_tmp'),
                       params['temp_tables'].get('all_companies')],
                      params.get('processing_out'),
                      format=yt.JsonFormat(control_attributes_mode='row_fields',
                                           attributes={'encode_utf8': False}),
                      reduce_by=['chain_id', 'geo_id'])
    client.set_attribute(params.get('processing_out'),
                         'expiration_time',
                         misc.get_ttl(params.get('yt_ttl', 1)))
    #client.set_attribute(params.get('ferryman_out'),
                         #'expiration_time',
                         #misc.get_ttl(params.get('yt_ttl', 1)))


def shop_wizard_info(shop):
    yield {'shop_edadeal_id': shop['edadeal_id'],
           'name': shop['name'],
           'edadeal_retailer_id': shop['edadeal_retailer_id']}


@yt.with_context
def add_retailer_slug(key, rows, context):
    rr = list(rows)
    shops = []
    retailers = []
    for row in rr:
        if row['@table_index'] == 0:
            shops.append(row)
        else:
            retailers.append(row)
    if len(retailers) > 0:
        for shop in shops:
            shop['retailer_slug'] = retailers[0]['slug']
            yield shop


@yt.with_context
def add_segment_info(key, rows, context):
    rr = list(rows)
    shops = []
    segments = []
    for row in rr:
        if row['@table_index'] == 0:
            shops.append(row)
        else:
            segments.append(row)
    if len(segments) > 0:
        for shop in shops:
            shop['parent_segment_id'] = segments[0]['parent_segment_id']
            shop['segment_slug'] = segments[0]['segment']['slug']
            shop['segment_title'] = segments[0]['segment']['title']
            yield shop


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Generate Edadeal snippets')
    parser.add_argument('--cluster',type=str, help='YT cluster')
    parser.add_argument('--parameters', type=str, help='Dict with job parameters')
    args = parser.parse_args()
    yt_client = misc.get_client(os.environ['YT_TOKEN'],
                                args.cluster)
    params = json.loads(args.parameters)
    logger = misc.get_logger('edadeal')

    #logger.info(u'раскидываем исходную запись акции на записи по магазинам')
    #yt_client.run_map(shop_offer,
                      #params['temp_tables'].get('offer'),
                      #params['temp_tables'].get('shop_offer'),
                      #format=yt.JsonFormat(attributes={"encode_utf8": True}))

    #logger.info(u'добавляем сегменты')
    #yt_client.run_sort(params['temp_tables'].get('wizard_segment'),
                       #sort_by=['segment_id'])
    #yt_client.run_sort(params['temp_tables'].get('shop_offer'),
                       #sort_by=['segment_id'])
    #yt_client.run_reduce(add_segment_info,
                         #[params['temp_tables'].get('shop_offer'),
                          #params['temp_tables'].get('wizard_segment')],
                         #params['temp_tables'].get('shop_offer'),
                         #format=yt.JsonFormat(attributes={"encode_utf8": True},
                                              #control_attributes_mode='row_fields'),
                         #reduce_by=['segment_id'],
                         #memory_limit=2 * GB)

    #logger.info(u'аггрегируем данные по магазинам')
    #yt_client.run_sort(params['temp_tables'].get('shop_offer'),
                       #sort_by=['shop_edadeal_id'])
    #yt_client.run_reduce(aggregate_shop_info,
                         #params['temp_tables'].get('shop_offer'),
                         #params['temp_tables'].get('shop_offer_aggregated'),
                         #format=yt.JsonFormat(attributes={"encode_utf8": False}),
                         #reduce_by=['shop_edadeal_id'])
    #yt_client.run_sort(params['temp_tables'].get('shop_offer_aggregated'),
                       #sort_by=['shop_edadeal_id'])

    #logger.info(u'собираем названия магазинов из данных едадила по всем изместных магазинам')
    #yt_client.run_map(shop_wizard_info,
                      #params['temp_tables'].get('shop_sprav'),
                      #params['temp_tables'].get('shop_name'),
                      #format=yt.JsonFormat(attributes={"encode_utf8": True}))

    #logger.info(u'добавляем retailer.slug к имени магазина')
    #yt_client.run_sort(params['temp_tables'].get('shop_name'),
                       #sort_by=['edadeal_retailer_id'])
    #yt_client.run_sort(params['temp_tables'].get('wizard_retailer'),
                       #sort_by=['edadeal_retailer_id'])
    #yt_client.run_reduce(add_retailer_slug,
                         #[params['temp_tables'].get('shop_name'),
                          #params['temp_tables'].get('wizard_retailer')],
                         #params['temp_tables'].get('shop_name'),
                         #format=yt.JsonFormat(attributes={"encode_utf8": True},
                                              #control_attributes_mode='row_fields'),
                         #reduce_by=['edadeal_retailer_id'])

    #logger.info(u'считаем количество магазинов в сети по данным едадила')
    #yt_client.run_sort(params['temp_tables'].get('shop_wizard'),
                       #sort_by=['chain_id'])
    #yt_client.run_reduce(chain_shop_count,
                         #params['temp_tables'].get('shop_wizard'),
                         #params['temp_tables'].get('chain_shop_count'),
                         #format=yt.JsonFormat(attributes={"encode_utf8": True}),
                         #reduce_by=['chain_id'])

    #logger.info(u'добавляем количество магазино в сети в сниппет')
    #yt_client.run_sort(params['temp_tables'].get('chain_shop_count'),
                       #sort_by=['chain_id'])
    #yt_client.run_reduce(shop_chain_shop_count,
                         #[params['temp_tables'].get('chain_shop_count'),
                          #params['temp_tables'].get('shop_wizard')],
                         #params['temp_tables'].get('shop_chain_shop_count'),
                         #format=yt.JsonFormat(attributes={"encode_utf8": True},
                                              #control_attributes_mode='row_fields'),
                         #reduce_by=['chain_id'])
    #yt_client.run_sort(params['temp_tables'].get('shop_chain_shop_count'),
                       #sort_by=['shop_edadeal_id'])

    #logger.info(u'добавляем окончательные данные в сниппет')
    #yt_client.run_sort(params['temp_tables'].get('shop_name'),
                       #sort_by=['shop_edadeal_id'])
    #yt_client.run_sort(params['temp_tables'].get('shop_wizard'),
                       #sort_by=['shop_edadeal_id'])
    #yt_client.run_reduce(build_result,
                         #[params['temp_tables'].get('shop_offer_aggregated'),
                          #params['temp_tables'].get('shop_wizard'),
                          #params['temp_tables'].get('shop_name'),
                          #params['temp_tables'].get('shop_chain_shop_count')],
                         #params['temp_tables'].get('result_shop_offers'),
                         #format=yt.JsonFormat(attributes={"encode_utf8": True},
                                              #control_attributes_mode='row_fields'),
                         #reduce_by=['shop_edadeal_id'])

    logger.info(u'раскидываем алтаевские компании на едадиловские и все остальные')
    map_altay_companies(params, yt_client)

    #logger.info(u'мержим едадиловскую компанию(со сниппетом) с алтаемскими')
    #yt_client.run_sort(params['temp_tables'].get('edadeal_altay_companies'),
                       #sort_by=['edadeal_id'])
    #yt_client.run_sort(params['temp_tables'].get('result_shop_offers'),
                       #sort_by=['edadeal_id'])
    #yt_client.run_reduce(map_altay_companies_to_snippets,
                         #[params['temp_tables'].get('edadeal_altay_companies'),
                          #params['temp_tables'].get('result_shop_offers')],
                         #params['temp_tables'].get('offers_altay_companies'),
                         #format=yt.JsonFormat(attributes={"encode_utf8": True},
                                                          #control_attributes_mode='row_fields'),
                         #reduce_by=['edadeal_id'])

    #logger.info(u'Добавляем корректные chain_id')
    #yt_client.copy(params['temp_tables'].get('company_to_chain'),
                #params['temp_tables'].get('company_to_chain_tmp'),
                #force=True)
    #yt_client.run_sort(params['temp_tables'].get('company_to_chain_tmp'),
                    #sort_by=['chain_permalink'])
    #yt_client.run_sort(params['temp_tables'].get('result_shop_offers'),
                    #sort_by=['chain_permalink'])
    #map_chain_permalinks(params, yt_client)

    #logger.info(u'множим сниппеты по всем компаниям сети по данным алтая')
    #yt_client.run_sort(params['temp_tables'].get('offers_altay_companies_tmp'),
                       #sort_by=['chain_id', 'geo_id'])
    #yt_client.run_sort(params['temp_tables'].get('all_companies'),
                       #sort_by=['chain_id', 'geo_id'])
    #chain_offers(params, yt_client)
