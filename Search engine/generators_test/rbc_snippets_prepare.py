#!/usr/bin/env python
# -*- coding: utf-8 -*

import os
import sys
import json
import argparse
import requests
import yt.wrapper as yt
import xml.etree.ElementTree as ET

sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'system'))
import misc


PROVIDER_NAME = 'rbc'


def uncomment(data):
    return data.replace("<!--", "").replace("-->", "")


def gen_snippet(company_currencies, timestamp):
    currencies_list = [xml_currency(company_currencies[currency_data]) for currency_data in company_currencies]
    return ('<CurrencyExchange xmlns="http://maps.yandex.ru/snippets/exchange/1.x">'
            '<timestamp>{timestamp}</timestamp>'
            '{currencies}'
            '</CurrencyExchange>').format(timestamp=timestamp,
                                          currencies=''.join(currencies_list))


def xml_currency(currency_data):
    return ('<Currency>'
                '<name>{currency_to}</name>'
                '<Buy>'
                    '<value>{buy_value}</value>'
                    '<text>{buy_value} {currency}</text>'
                    '<currency>{currency_from}</currency>'
                '</Buy>'
                '<Sell>'
                    '<value>{sell_value}</value>'
                    '<text>{sell_value} {currency}</text>'
                    '<currency>{currency_from}</currency>'
                '</Sell>'
            '</Currency>').format(currency_to=currency_data.get('currency_to'),
                                  buy_value=currency_data.get('buy_value'),
                                  currency_from=currency_data.get('currency_from'),
                                  sell_value=currency_data.get('sell_value'),
                                  currency=misc.LOCALIZED_CURRENCIES.get(currency_data['currency_from'],
                                                                         currency_data['currency_from']))


def gen_table(xml_data):
    snippets = []
    print 'Parsing XML file...'
    root = ET.XML(xml_data)
    ET.register_namespace('', 'http://maps.yandex.ru/snippets/exchange/1.x')
    for company in root.findall('./company'):
        company_id = company.find('./company-id').text
        timestamp = long(company.find('./actualization-date').text) / 1000    # Canonization
        company_currencies = {}
        for feature in company.findall('./feature'):
            currencies = feature.find("./feature-enum-single[@name='currency_short_enum']").get('value')
            volume_min = int(feature.find("./feature-numeric-single[@name='volume_min']").get('value'))
            if company_currencies.get(currencies) is None or company_currencies.get(currencies)['volume_min'] > volume_min:
                currencies_split = currencies.split('/')
                try:
                    company_currencies[currencies] = {'currency_to': currencies_split[0],
                                                      'currency_from': currencies_split[1],
                                                      'volume_min': volume_min,
                                                      'volume_max': int(feature.find("./feature-numeric-single[@name='volume_max']").get('value')),
                                                      'sell_value': float(feature.find("./feature-numeric-single[@name='sell']").get('value')),
                                                      'buy_value': float(feature.find("./feature-numeric-single[@name='buy']").get('value'))}
                except Exception:
                    pass
        if company_currencies:
            snippet_text = ET.tostring(ET.XML(gen_snippet(company_currencies, timestamp)))
            snippet_text = snippet_text.replace('\n', '')
            snippet_text = snippet_text.replace('    ', '')
            snippets.append({'key': company_id,
                             'value': snippet_text})
    return snippets


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Prepare RBC snippets data')
    parser.add_argument('--cluster',type=str, help='YT cluster')
    parser.add_argument('--parameters', type=str, help='Dict with job parameters')
    args = parser.parse_args()
    yt_client = misc.get_client(os.environ['YT_TOKEN'], args.cluster)
    params = json.loads(args.parameters)
    xml_data = uncomment(misc.download(params.get('input_table')))
    snippets = gen_table(xml_data)
    yt_client.write_table(params.get('pre_processing_out'),
                          snippets,
                          format=yt.JsonFormat(attributes={"encode_utf8": False}))
