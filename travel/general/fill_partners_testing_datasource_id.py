# -*- coding: utf-8 -*-
"""
Usage:
    $./manage.py run_path scripts/fill_partners_testing_datasource_id.py
"""
import sys

import logging
from lxml import etree

import requests


log = logging.getLogger(__name__)
log.addHandler(logging.StreamHandler(sys.stdout))
log.setLevel(logging.INFO)


def query(uri, params):
    host = 'http://mbi1gt.market.yandex.net:34861/'
    url = host + uri
    r = requests.get(url, params=params)
    return etree.fromstring(r.content)


def get_datasource_by_campaign_id(campaign_id):
    xml = query('getCampaign', {
        'campaignId': str(campaign_id),
        'serviceId': '114'
    })
    return int(xml.find('campaign-info').find('datasource-id').text)


PARTNERS_CAMPAIGNS = {
    'agent': 22,
    'amargo': 21,
    'aviakass': 144,
    'aviaoperator': 203,
    'awad': 125,
    'biletdv': 51,
    'biletexpert': 41,
    'biletix': 20,
    'biletonline': 40,
    'bravoavia': 294,
    'chabooka': 126,
    'charterbilet': 55,
    'chartex': 237,
    'clickavia': 56,
    'davs': 29,
    'eviterra': 28,
    'mau': 265,
    'nabortu': 127,
    'onetwotrip': 46,
    'ozon': 23,
    'pososhok': 24,
    's_seven': 307,
    'senturia': 47,
    'sindbad': 19,
    'svyaznoy': 270,
    'talarii': 43,
    'ticketsua': 52,
    'trip_ru': 48,
    'tripsta': 49,
    'ufs': 220,
}


from travel.avia.library.python.common.models.partner import Partner


def main():
    partners_by_code = {}
    for code in PARTNERS_CAMPAIGNS:
        try:
            partners_by_code[code] = Partner.objects.get(code=code)
        except Partner.DoesNotExist:
            log.info('Fix code %r', code)
            sys.exit(1)

    for code, campaign_id in PARTNERS_CAMPAIGNS.items():
        datasource_id = get_datasource_by_campaign_id(campaign_id)
        print('Partner:{:<14} campaign_id:{:<5} datasource_id:{}'.
              format(code, campaign_id, datasource_id))
        partner = partners_by_code[code]
        partner.billing_datasource_id_testing = datasource_id
        partner.save()


if __name__ == '__main__':
    main()
