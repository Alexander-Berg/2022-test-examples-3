# -*- coding: utf-8 -*-

import travel.avia.admin.init_project  # noqa

import logging
import requests

from django.conf import settings
from django import setup as django_setup

from travel.avia.library.python.common.models.partner import Partner
from travel.avia.admin.lib.logs import create_current_file_run_log

log = logging.getLogger(__name__)

AVIA_BACKEND_PRODUCTION_URL = settings.AVIA_BACK_PROD_URL
headers = {
    'Accept': 'application/json',
    'Content-Type': 'application/json',
}

param = (('name', 'partnersData'),)
data = '[{"name":"partnersData"}]'


def _main():
    if settings.YANDEX_ENVIRONMENT_TYPE != 'testing':
        return

    test_partner_codes = set(Partner.objects.values_list('code', flat=True))

    response = requests.post(url=AVIA_BACKEND_PRODUCTION_URL, headers=headers, params=param, data=data)
    prod_partners = response.json()['data'][0]

    for partner in prod_partners:
        partner_code = partner.get('code')

        if partner_code in test_partner_codes or not partner_code:
            continue

        try:
            partner = Partner(
                title=partner.get('title'),
                site_url=partner.get('siteUrl'),
                code=partner_code,
                enabled=partner.get('enabled'),
                click_price=partner.get('clickPrice'),
                marker=partner.get('marker'),
            )
            partner.save()
        except Exception as e:
            log.warning('The new partner %s was not created. Error: %s', partner_code, e)

        log.info('The partner %s was created successfully', partner_code)


def main():
    create_current_file_run_log()
    django_setup()
    _main()
