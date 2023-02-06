#!/usr/bin/env python
# coding=utf-8

import unittest
import os
import requests
import json
from lxml import etree
import collections
import time
import case_log
import mbi_common_consts
import subprocess
from benchmark import benchmark
from datetime import datetime
from urlparse import urlparse

ShopParam = collections.namedtuple('ShopParam', 'type_id type name value')
OrganizationInfo = collections.namedtuple('OrganizationInfo', 'info_id organization_type organization_type_code name ogrn fact_address juridical_address url info_source registration_number')
AssessorClaim = collections.namedtuple('AssessorClaim', 'type_id active text cpa')
PreCampaign = collections.namedtuple('PreCampaign', 'campaign_id datasource_id')
DatasourceMissedInfo = collections.namedtuple('DatasourceMissedInfo', 'type name')
User = collections.namedtuple('User', 'id email')
ShopAboCheckRequest = collections.namedtuple('ShopAboCheckRequest', 'id offline testing_type try_num')

# https://wiki.yandex-team.ru/market/pokupka/projects/new-prepayment-russia/dev/mbi-api/entities/organizationInfo/
PrepaymentOrganizationInfo = collections.namedtuple('PrepaymentOrganizationInfo', 'name type ogrn inn kpp juridicalAddress factAddress postcode accountNumber corrAccountNumber bik bankName url licenseNumber licenseDate workSchedule')
# https://wiki.yandex-team.ru/market/pokupka/projects/new-prepayment-russia/dev/mbi-api/entities/contactInfo/
PrepaymentContactInfo = collections.namedtuple('PrepaymentContactInfo', 'name phoneNumber email')
# https://wiki.yandex-team.ru/market/pokupka/projects/new-prepayment-russia/dev/mbi-api/entities/signatory/
PrepaymentSignatoryInfo = collections.namedtuple('PrepaymentSignatoryInfo', 'name docType docInfo')


class PrepayStatus:
    INIT = 0
    IN_PROGRESS = 1
    COMPLETED = 2
    FROZEN = 3
    CLOSED = 4
    DECLINED = 5
    INTERNAL_CLOSED = 6
    NEW = 7
    NEED_INFO = 8
    CANCELLED = 9


class PremoderationResult:
    # https://github.yandex-team.ru/market-java/mbi/blob/release/new-sandbox-premoderation/mbi-core/src/java/ru/yandex/market/core/moderation/qc/result/PremoderationResult.java
    # https://wiki.yandex-team.ru/Market/Development/abo/cpc/premoderation/#rezultat
    PASSED = 0
    FAILED = 1
    SKIPPED = 2
    HALTED = 3


class CutoffType:
    # https://github.yandex-team.ru/market-java/mbi/blob/master/mbi-core/src/java/ru/yandex/market/core/cutoff/model/CutoffType.java
    FINANCE = 3
    FORTESTING = 6
    QMANAGER_CLONE = 10
    COMMON_OTHER = 42
    CPC_PARTNER = 44

    # List of fatal cutoffs (27.06.2017):
    #
    # CutoffType.YAMANAGER,
    # CutoffType.QMANAGER_CHEESY,
    # CutoffType.QMANAGER_FRAUD,
    # CutoffType.QMANAGER_CLONE,
    # CutoffType.CPA_QUALITY_CHEESY,
    # CutoffType.COMMON_QUALITY,
    # CutoffType.CPA_QUALITY_AUTO,
    # CutoffType.CPA_FEED


class TestingType:
    CPC_PREMODERATION = 0
    CPA_PREMODERATION = 3


class Environment:
    env = None

    @classmethod
    def get(cls):
        if cls.env is None:
            cls.env = cls()
        return cls.env

    def __init__(self):
        # mt-10
        #self.mbi_api_url = 'http://mbi10et.haze.yandex.net:34820'
        #self.mbi_partner_url = 'http://mbi10et.haze.yandex.net:38271'
        #self.mbi_shops_url = 'http://braavos.yandex.ru'
        #self.mbi_billing_url = 'http://mbi10et.haze.yandex.net:34852'

        # BT
        self.mbi_api_url = 'http://mbi-back.tst.vs.market.yandex.net:34820'
        self.mbi_partner_url = 'http://mbi-partner.tst.vs.market.yandex.net:38271'
        self.mbi_shops_url = 'http://shopinfo.tst.vs.market.yandex.net:38110'
        self.mbi_billing_url = 'http://mbi1gt.market.yandex.net:12346'


class MbiShops:
    def __init__(self, base_url=None):
        self.base_url = base_url if base_url else Environment.get().mbi_shops_url
        self.logger = case_log.CaseLogger.get()

    def get_shop_info(self, shop_id, json_format=True):
        shop_id = int(shop_id)
        path = '{}/shopInfo?shop-id={}'.format(self.base_url, shop_id)
        return self._request(path)

    def _request(self, path):
        r = requests.get(path)
        self.logger.log_request(r)
        return r.json()


class MbiPartner:
    # https://wiki.yandex-team.ru/mbi/newdesign/components/market-payment/
    # https://wiki.yandex-team.ru/mbi/newdesign/components/market-payment/ruchki/
    # Ручки, в которые ходит ПИ
    def __init__(self, base_url=None, user_id=None):
        self.base_url = base_url if base_url else Environment.get().mbi_partner_url
        self.user_id = user_id if user_id else 79668854
        self.logger = case_log.CaseLogger.get()

    def show_missed_datasource_info(self, shop_id):
        # https://wiki.yandex-team.ru/mbi/newdesign/components/market-payment/showmisseddatasourceinfo/
        dom =  self._request_xml('showMissedDatasourceInfo', datasource_id=shop_id)
        res = {}
        for usi in dom.xpath('/data/uni-shop-information'):
            dmi = DatasourceMissedInfo(
                type=usi.xpath('type')[0].text,
                name=usi.xpath('name')[0].text)
            res[dmi.name] = dmi
        return res

    def get_datasource_params(self, shop_id):
        # https://wiki.yandex-team.ru/MBI/NewDesign/components/market-payment/getDatasourceParams/
        dom = self._request_xml('getDatasourceParams', datasource_id=shop_id)
        params = {}
        for p in dom.xpath('/data/param-value'):
            pa = ShopParam(
                type_id=p.attrib['type-id'],
                type=p.attrib['type'],
                name=p.attrib['name'],
                value=p.text)
            params[pa.type] = pa
        return params

    def manage_param(self, shop_id, param_id, value=None):
        # https://wiki.yandex-team.ru/mbi/newdesign/components/market-payment/manageparam/
        # Злая ручка. Не дает, например, после установки номера телефона, откатить это назад
        params = {
            'datasource_id': shop_id,
            'type': str(int(param_id))}
        if value:
            params['value'] = value
        dom = self._request_xml('manageParam', **params)
        if len(dom.xpath('/data/errors')) > 0:
            raise Exception(etree.tostring(dom))
        p = dom.xpath('/data/param-value')[0]
        pa = ShopParam(
            type_id=p.attrib['type-id'],
            type=p.attrib['type'],
            name=p.attrib['name'],
            value=p.text)
        return pa

    def _organization_info_from_dom(self, dom):
        def toe(l): # text or empty
            return l[0].text if l else ''

        info = OrganizationInfo(
            info_id=toe(dom.xpath('//data/organization-info/info-id')),
            organization_type=toe(dom.xpath('//data/organization-info/organization-type')),
            organization_type_code=toe(dom.xpath('//data/organization-info/organization-type-code')),
            name=toe(dom.xpath('//data/organization-info/name')),
            ogrn=toe(dom.xpath('//data/organization-info/ogrn')),
            fact_address=toe(dom.xpath('//data/organization-info/fact-address')),
            juridical_address=toe(dom.xpath('//data/organization-info/juridical-address')),
            url=toe(dom.xpath('//data/organization-info/url')),
            info_source=toe(dom.xpath('//data/organization-info/info-source')),
            registration_number=toe(dom.xpath('//data/organization-info/registration-number')),
        )
        return info

    def show_organization_info(self, shop_id):
        # https://wiki.yandex-team.ru/MBI/NewDesign/components/market-payment/showOrganizationInfo/
        dom = self._request_xml('showOrganizationInfo', datasource_id=shop_id)
        return self._organization_info_from_dom(dom)

    def edit_organization_info(self, info, shop_id=None, create_new=False):
        # https://wiki.yandex-team.ru/MBI/NewDesign/components/market-payment/editOrganizationInfo/
        d = info.__dict__.copy()
        if create_new:
            assert shop_id is not None
            d['datasource_id'] = shop_id
            d['info_id'] = 0  # Секретная технология для создания новой информации
        if shop_id:
            d['datasource_id'] = shop_id
        d['type'] = '6'
        dom = self._request_xml(
                'editOrganizationInfo',
                **d)
        if len(dom.xpath('/data/errors')) > 0:
            raise Exception(etree.tostring(dom))

    def get_cpa_state(self, shop_id):
        # https://wiki.yandex-team.ru/MBI/NewDesign/components/market-payment/getCPAState/
        dom = self._request_xml('getCPAState', datasource_id=shop_id)
        if len(dom.xpath('/data/cpastate/cpa')) <= 0:
            raise Exception(etree.tostring(dom))
        return dom

    def get_cpa_cutoffs(self, shop_id):
        # Обертка над getCPAState
        cpa_state = self.get_cpa_state(shop_id)
        return set([x.text for x in cpa_state.xpath('/data/shop-cutoffs/cutoff-type')])

    def cpc_state(self, shop_id):
        # https://wiki.yandex-team.ru/mbi/newdesign/components/market-payment/cpcstate/
        j = self._request_json('cpcState', datasource_id=shop_id)
        assert 'result' in j
        assert 'cpc' in j['result']
        return j

    def pre_campaign_create(self, region=134, local_delivery_region=10590, user=None, is_global=True, is_online=True):
        """
        @param user: User
        """
        if not user:
            user = User(id=self._user_id, email='ikhudyshev38@yandex.ru')
        owner = user.email.split('@')[0]
        # https://wiki.yandex-team.ru/MBI/NewDesign/components/market-payment/preCampaign/
        params = {
            'a': 'c',
            'is_online': '1' if is_online else '0',
            'url': 'fantamp{}.yandex.ru'.format(int(time.time())),
            'region': str(int(region)),
            'owner': owner,
            'contactName': 'Ilya',
            'contactLastName': 'Khudyshev',
            'contactEmail': user.email,
            'contactPhone': '79993334422',
            'isGlobal': 'true' if is_global else 'false',
            'localDeliveryRegion': str(int(local_delivery_region)),
        }
        dom = self._request_xml('preCampaign', **params)
        if len(dom.xpath('/data/pre-campaign')) <= 0:
            raise Exception(etree.tostring(dom))
        return PreCampaign(
            campaign_id=dom.xpath('/data/pre-campaign/campaign-id')[0].text,
            datasource_id=dom.xpath('/data/pre-campaign/datasource-id')[0].text)

    def pre_campaign_update(self, campaign_id, is_global, region=134):
        # https://wiki.yandex-team.ru/MBI/NewDesign/components/market-payment/preCampaign/
        params = {
            'a': 'u',
            'id': str(int(campaign_id)),
            'region': str(int(region)),
            'isGlobal': 'true' if is_global else 'false',
            'step': '2',
            'feedUrl': 'https://bfg9000.yandex.ru/yashops/norvos/shop2/feed',
        }
        dom = self._request_xml('preCampaign', **params)
        if len(dom.xpath('/data/pre-campaign')) <= 0:
            raise Exception(etree.tostring(dom))

    def pre_campaign_request(self, campaign_id):
        # https://wiki.yandex-team.ru/MBI/NewDesign/components/market-payment/preCampaign/
        params = {
            'a': 'r',
            'id': str(int(campaign_id)),
        }
        dom = self._request_xml('preCampaign', **params)
        if len(dom.xpath('/data/pre-campaign')) <= 0:
            raise Exception(etree.tostring(dom))

    def register_campaign(self, campaign_id, enable_programs_tumblers=True):
        # https://wiki.yandex-team.ru/MBI/NewDesign/components/market-payment/registerCampaign/
        params = {
            '_user_id': '505977008',
            'id': str(int(campaign_id)),
        }
        if enable_programs_tumblers:
            params['enableProgramsTumblers'] = '1'
        dom = self._request_xml('registerCampaign', **params)
        if len(dom.xpath('/data')) <= 0 or len(dom.xpath('/data/errors')) > 0:
            raise Exception(etree.tostring(dom))

    def moderation_request_state(self, shop_id):
        # https://wiki.yandex-team.ru/MBI/NewDesign/components/market-payment/moderationRequestState/
        # https://github.yandex-team.ru/market-java/mbi/blob/release/2017.2.54_MBI-21242/mbi-core/src/java/ru/yandex/market/core/moderation/DefaultModerationService.java
        return self._request_json('moderationRequestState', datasource_id=int(shop_id))

    def get_premoderation_info(self, shop_id):
        # https://wiki.yandex-team.ru/mbi/newdesign/components/market-payment/getpremoderationinfo/
        dom = self._request_xml('getPremoderationInfo', datasource_id=int(shop_id))
        if len(dom.xpath('/data/datasource-premoderation-info')) <= 0 or len(dom.xpath('/data/testing-details')) <= 0:
            raise Exception(etree.tostring(dom))

    def push_ready_for_testing(self, shop_id, cpa_only_ready=True):
        # Это нажатие кнопки "Я исправился"
        # https://wiki.yandex-team.ru/MBI/NewDesign/components/market-payment/pushReadyForTesting/
        dom = self._request_xml('pushReadyForTesting', datasource_id=int(shop_id), cpa_only_ready='true' if cpa_only_ready else 'false')
        if len(dom.xpath('/data/ok')) <= 0:
            raise Exception(etree.tostring(dom))



    def update_cpc_placement(self, shop_id, is_enabled):
        # https://wiki.yandex-team.ru/mbi/newdesign/components/market-payment/updatecpcplacement/
        j = self._request_json(
            'updateCpcPlacement',
            method='POST',
            datasource_id=int(shop_id),
            enabled='true' if is_enabled else 'false')
        if 'errors' in j:
            msg = 'Errors: {}'.format('; '.join(map(lambda e: e.get('message', 'None'), j['errors'])))
            raise Exception(msg)
        assert j['result']['status'] == 'OK'

    def cpa_status_update(self, shop_id, is_enabled):
        # https://wiki.yandex-team.ru/mbi/newdesign/components/market-payment/cpastatusupdate/
        dom = self._request_xml(
            'cpaStatusUpdate',
            datasource_id=int(shop_id),
            enabled='true' if is_enabled else 'false')
        if len(dom.xpath('/data/errors')) > 0:
            raise Exception(etree.tostring(dom.xpath('/data/errors')[0]))
        assert dom.xpath('/data/result')[0].text == 'OK'

    def get_campaign_finance_info(self, shop_id):
        # https://wiki.yandex-team.ru/mbi/newdesign/components/market-payment/getcampaignfinanceinfo/
        dom = self._request_xml(
            'getCampaignFinanceInfo',
            datasource_id=int(shop_id))
        if len(dom.xpath('/data/campaign-balance-info')) <= 0:
            raise Exception(etree.tostring(dom))
        return dom

    def get_shop_balance(self, shop_id):
        return float(self.get_campaign_finance_info(shop_id).xpath('/data/campaign-balance-info/actual-balance')[0].text)

    def get_campaign_states(self, shop_id):
        # https://wiki.yandex-team.ru/mbi/newdesign/components/market-payment/getcampaignstates/
        dom = self._request_xml(
            'getCampaignStates',
            datasource_id=int(shop_id))
        if len(dom.xpath('/data/campaign-state')) <= 0:
            raise Exception(etree.tostring(dom))
        return dom.xpath('/data/campaign-state')[0].attrib['id']

    def generate_push_api_token(self, shop_id):
        # https://wiki.yandex-team.ru/mbi/newdesign/components/market-payment/generatepushapitoken/
        dom = self._request_xml(
            'generatePushAPIToken',
            datasource_id=int(shop_id))
        if len(dom.xpath('/data/result')) <= 0 or dom.xpath('/data/result')[0].text != 'OK':
            raise Exception(etree.tostring(dom))

    def activate_push_api_token(self, shop_id):
        # https://wiki.yandex-team.ru/MBI/NewDesign/components/market-payment/activatePushAPIToken/
        dom = self._request_xml(
            'activatePushAPIToken',
            datasource_id=int(shop_id))
        if len(dom.xpath('/data/result')) <= 0 or dom.xpath('/data/result')[0].text != 'OK':
            raise Exception(etree.tostring(dom))

    def push_api_settings(self, shop_id):
        # https://wiki.yandex-team.ru/MBI/NewDesign/components/market-payment/pushApiSettings/
        # pushApiSettings
        dom = self._request_xml(
            'pushApiSettings',
            datasource_id=int(shop_id),
            a='u',  # u - update
            pr_api_url='https://bfg9000.yandex.ru/yashops/norvos/shop2/api',
            pr_sha1='cf4306b95a2601dcd6c27fc8c460961ef6605e72',
            pr_auth_type='HEADER',
            pr_data_type='JSON')
        if len(dom.xpath('/data/result')) <= 0 or dom.xpath('/data/result')[0].text != 'OK':
            raise Exception(etree.tostring(dom))

    def cpa_order_processing_mode_update(self, shop_id, mode):
        # https://wiki.yandex-team.ru/mbi/newdesign/components/market-payment/cpaorderprocessingmodeupdate/
        assert mode in ('API', 'MARKET')
        dom = self._request_xml(
            'cpaOrderProcessingModeUpdate',
            datasource_id=int(shop_id),
            p_mode=mode)
        if len(dom.xpath('/data/result')) <= 0 or dom.xpath('/data/result')[0].text != 'OK':
            raise Exception(etree.tostring(dom))

    def get_prepay_info(self, shop_id):
        response = self._request_json(handle='prepay-request', datasourceId=shop_id)
        return response

    def _prepay_request_post_body_json(self, shop_id, poi, pci, psi):
        """
        poi == PrepaymentOrganizationInfo
        pci == PrepaymentContactInfo
        psi == PrepaymentSignatoryInfo
        """
        j = {
            "datasourceIds": [shop_id],
            "organizationInfo": {
                "factAddress": poi.factAddress,
                "juridicalAddress": poi.juridicalAddress,
                "name": poi.name,
                "inn": poi.inn,
                "accountNumber": poi.accountNumber,
                "corrAccountNumber": poi.corrAccountNumber,
                "bik": poi.bik,
                "bankName": poi.bankName,
                "ogrn": poi.ogrn,
                "type": poi.type,
                "url": poi.url,
                "licenseNumber": poi.licenseNumber,
                "licenseDate": poi.licenseDate,
                "workSchedule": poi.workSchedule,
                "postcode": poi.postcode,
                "kpp": poi.kpp,
            },
            "contactInfo": {
                "name": pci.name,
                "phoneNumber": pci.phoneNumber,
                "email": pci.email
            },
            "signatory": {
                "name": psi.name,
                "docType": psi.docType,
                "docInfo": psi.docInfo
            }
        }
        return j

    def new_prepay_request(self, shop_id, prepayment_organization_info, prepayment_contact_info, prepayment_signatory_info):
        j = self._prepay_request_post_body_json(shop_id=shop_id, poi=prepayment_organization_info, pci=prepayment_contact_info, psi=prepayment_signatory_info)
        response = self._request_json(handle='prepay-request', method='POST', data=json.dumps(j), ct='application/json', datasourceId=shop_id)
        return response

    def is_prepay_available(self, shop_id):
        """
        Доступна ли программа предоплаты вообще
        """
        response = self.get_prepay_info(shop_id)
        if response['result']['availabilityStatus'] == 'AVAILABLE':
            return True
        else:
            return False

    def prepay_requests_count(self, shop_id):
        """
        Сколько заявок есть
        """
        return len(self.get_prepay_info(shop_id)['result']['prepayRequests'])

    def prepay_request_application_form(self, request_id, shop_id):
        response = self._request(handle='prepay-request/{}/application-form'.format(request_id), datasourceId=shop_id)
        return response

    def _prepay_request_status_put(self, status, request_id, shop_id):
        response = self._request_json(handle='prepay-request/{}/status'.format(request_id), method='PUT', format='json', datasourceId=shop_id, status=status)
        return response

    def prepay_request_status_init(self, request_id, shop_id):
        return self._prepay_request_status_put(status=PrepayStatus.INIT, request_id=request_id, shop_id=shop_id)

    def prepay_request_status_in_progress(self, request_id, shop_id):
        return self._prepay_request_status_put(status=PrepayStatus.IN_PROGRESS, request_id=request_id, shop_id=shop_id)

    def prepay_request_status_completed(self, request_id, shop_id):
        return self._prepay_request_status_put(status=PrepayStatus.COMPLETED, request_id=request_id, shop_id=shop_id)

    def prepay_request_post_document(self, request_id, shop_id, file_path):
        files=dict(
            type=(None, '1'),
            name=(None, 'testing file'),
            file=(file_path, open(file_path, 'rb').read(), 'image/jpg')
        )
        response = self._request_json(handle='prepay-request/{}/document'.format(request_id), method='POST', datasourceId=shop_id, files=files)
        return response

    def _request_json(self, handle, method='GET', files=None, **params):
        j = self._request(handle, method=method, files=files, **params)
        return json.loads(j)

    def _request_xml(self, handle, **params):
        xml_raw = self._request(handle, **params)
        return etree.fromstring(xml_raw)

    def _request(self, handle, method='GET', data=None, json=None, files=None, **params):
        p = params.copy()
        p.update({'_user_id': int(self.user_id)})
        url = '{base_url}/{handle}'.format(base_url=self.base_url, handle=handle)
        if json is not None:
            data = _request(url, method=method, json=json, logger=self.logger, files=files, **p)
        else:
            data = _request(url, method=method, data=data, logger=self.logger, files=files, **p)
        return data


class MbiApi:
    # https://wiki.yandex-team.ru/MBI/NewDesign/components/mbi-api/
    # Коды катофов АБО: https://github.yandex-team.ru/market-java/mbi/blob/release/2017.2.54_MBI-21242/mbi-core/src/java/ru/yandex/market/core/abo/AboCutoff.java
    # Коды одного и того же катофа могут отличаться в ABO и MBI. Так сложилось. Отображение: https://github.yandex-team.ru/market-java/mbi/blob/6ea0f654013cd141fc21e6690f6822994796c379/mbi-core/src/java/ru/yandex/market/core/abo/AboConfig.java
    def __init__(self, base_url=None):
        self.base_url = base_url if base_url else Environment.get().mbi_api_url
        self.logger = case_log.CaseLogger.get()

    def shop_abo_cutoffs(self, shop_id=None, only_active=True):
        path = '/shop-abo-cutoffs/{}'.format(int(shop_id))
        dom = self._get(path)
        cutoffs = {}
        for x in dom.xpath('/shop_abo_cutoffs_response/abo_cutoffs/abo_cutoff'):
            cutoff = {
                'type': x.xpath('type')[0].text,
                'is_active': x.xpath('is_active')[0].text == 'true'
            }
            cutoffs[cutoff['type']] = cutoff
        return {co['type']: co for co in cutoffs.values() if (only_active and co['is_active']) or not only_active}

    def abo_cutoff_open(self, shop_id, cutoff_type):
        """ Example:
            mbi_api.abo_cutoff_open(defaultShopId, 'CPC_QUALITY')
        """
        # https://github.yandex-team.ru/market-java/mbi/blob/f9471f7aa4b8cd088a4dda992f1bf6a81c740ee7/mbi-core/src/java/ru/yandex/market/core/abo/AboConfig.java
        path = '/abo-cutoff/{}/open'.format(int(shop_id))
        data = '''<?xml version="1.0" encoding="UTF-8" ?>
            <shop_abo_cutoffs_open_request>
                <uid>421258654</uid>
                <cutoff_type>{cutoff_type}</cutoff_type>
                <cutoff_comment>тест</cutoff_comment>
                <mail_subject></mail_subject>
                <mail_body></mail_body>
            </shop_abo_cutoffs_open_request>'''.format(cutoff_type=cutoff_type)
        dom = self._post(path, data)
        if dom.xpath('/shop_abo_cutoffs_open_response/status')[0].text != 'OK':
            raise Exception(etree.tostring(dom))
        return dom

    def abo_cutoff_close(self, shop_id, cutoff_type):
        path = '/abo-cutoff/{}/close'.format(int(shop_id))
        data = '''<?xml version="1.0" encoding="UTF-8" ?>
            <shop_abo_cutoffs_close_request>
                <uid>421258654</uid>
                <cutoff_type>{cutoff_type}</cutoff_type>
                <cutoff_comment>тест</cutoff_comment>
                <mail_subject></mail_subject>
                <mail_body></mail_body>
            </shop_abo_cutoffs_close_request>'''.format(cutoff_type=cutoff_type)
        dom = self._post(path, data)
        if dom.xpath('/shop_abo_cutoffs_close_response/status')[0].text != 'OK':
            raise Exception(dom.xpath('/shop_abo_cutoffs_close_response/error')[0].text)
        return dom

    def close_cpa_cutoff(self, shop_id, cutoff_type):
        # Можно дергать много раз. Статус все рано будет OK даже если катоффа не было
        data = '<cutoffs><cutoff shop-id="{shop_id}" type="{cutoff_type}" uid="12345" tid="105"/></cutoffs>'.format(
            shop_id=int(shop_id),
            cutoff_type=cutoff_type)
        dom = self._post('/close-cpa-cutoff', data)
        assert dom.xpath('/cutoff-responses/cutoff-response')[0].attrib['status'] == 'OK'

    def close_all_cutoffs(self, shop_id):
        cutoffs = [x for x in self.shop_abo_cutoffs(shop_id).values() if x['is_active']]
        for cutoff in cutoffs:
            self.abo_cutoff_close(shop_id, cutoff['type'])

    def get_cpa_shops(self, shop_id):
        path = '/get-cpa-shops'
        params = {'shop-id': int(shop_id)}
        dom = self._get(path, params=params)
        assert len(dom.xpath('/paged-shops/shops')) > 0
        return dom.xpath('/paged-shops/shops/shop')

    def accessor_claims(self, shop_id):
        # https://wiki.yandex-team.ru/MBI/NewDesign/components/mbi-api/accessor-claims/
        path = '/accessor-claims'
        params = {'datasourceId': int(shop_id)}
        dom = self._get(path, params)
        assert len(dom.xpath('/list-claim-info-response')) > 0
        claims = []
        for claim in dom.xpath('/list-claim-info-response/claim-info'):
            c = AssessorClaim(
                type_id=int(claim.xpath('typeId')[0].text),
                active=claim.xpath('active')[0].text == 'true',
                text=claim.xpath('text')[0].text,
                cpa=claim.xpath('cpa')[0].text == 'true')
            claims.append(c)
        return claims

    def accessor_claims_close(self, shop_id, claim_type_id):
        # https://wiki.yandex-team.ru/MBI/NewDesign/components/mbi-api/accessor-claims/close/
        path = '/accessor-claims/close'
        data = '''<claim-close-request>
                  <datasource-id>{shop_id}</datasource-id>
                  <claim>{claim_type_id}</claim>
                  <uid>123</uid>
                  <send-message>false</send-message>
                  <comment>asdf</comment>
                </claim-close-request>'''.format(
            shop_id=shop_id,
            claim_type_id=claim_type_id)
        dom = self._post(path, data)
        print etree.tostring(dom, pretty_print=True)
        assert dom.xpath('/response/status')[0].text == 'OK'

    def accessor_claims_close_all(self, shop_id):
        claims = self.accessor_claims(shop_id)
        for c in claims:
            if c.active:
                self.accessor_claims_close(shop_id, c.type_id)

    def testing_shops_premoderation_feed_check_delete(self, shop_id, program):
        # https://wiki.yandex-team.ru/users/wadim/cpa-premoderation-test/
        """
        @param program: 'CPC' or 'CPA'
        """
        assert program in ('CPC', 'CPA')
        path = '/testing/shops/{}/premoderation/{}/feed/check'.format(int(shop_id), program)
        dom = self._request(path, method='DELETE')
        if len(dom.xpath('/response')) <= 0 or dom.xpath('/response')[0].attrib.get('status') != 'OK':
            raise Exception(etree.tostring(dom))

    def qc_shops_premoderations(self):
        # https://wiki.yandex-team.ru/mbi/newdesign/components/mbi-api/premoderation-ready-shops/
        return self._get('/qc/shops/premoderations')

    def qc_shops_premoderations_get_shop_checks_list(self, shop_id):
        """ Проверяет, выставлен ли данный магазин для АБО для модерации
        """
        dom = self.qc_shops_premoderations()
        shs = [sh for sh in dom.xpath('/shops/shops/shop') if sh.attrib.get('id') == str(shop_id)]
        if not shs:
            raise KeyError('Shop with id={} not found in /qc/shops/premoderations'.format(shop_id))
        res = []
        for sh in shs:
            exposed = ShopAboCheckRequest(
                id=sh.attrib['id'],
                offline=sh.attrib['offline'] == 'true',
                testing_type=sh.attrib['testing-type'],
                try_num=int(sh.attrib['try-num']))
            res.append(exposed)
        return res

    def qc_shops_premoderations_result(self, shop_id, testing_type, quality_cs=PremoderationResult.PASSED, clone_cs=PremoderationResult.PASSED, order_cs=PremoderationResult.SKIPPED):
        """
        @param testing_type see Testing
        """
        # https://wiki.yandex-team.ru/mbi/newdesign/components/mbi-api/premoderation-result/
        body_tpl = \
            '''<result shop-id="{shop_id}" testing-type="{testing_type}" quality-check-status="{quality_cs}" clone-check-status="{clone_cs}" order-check-status="{order_cs}">\n''' \
            '''    <message subject="subject" template-id="321">body</message>\n''' \
            '''</result>'''
        body = body_tpl.format(
            shop_id=shop_id,
            testing_type=testing_type,
            quality_cs=quality_cs,
            clone_cs=clone_cs,
            order_cs=order_cs)
        dom = self._post('/qc/shops/premoderations/result', body)
        if len([e for e in dom.xpath('/response/status') if e.text == 'OK']) <= 0:
            raise Exception(etree.tostring(dom))

    def qc_shops_light_checks(self, shop_id):
        # https://wiki.yandex-team.ru/MBI/NewDesign/components/mbi-api/light-check-ready-shops/
        dom = self._get('/qc/shops/light-checks')
        return dom

    def hack_set_shop_param_value(self, shop_id, param_type_id, value):
        # Секретная ручка. Не описана.
        # http://mbi10et.haze.yandex.net:34820/testing/shops/{shopId}/params/{paramTypeId}?value=true
        path = '/testing/shops/{shop_id}/params/{param_type_id}?value={value}'.format(
            shop_id=int(shop_id),
            param_type_id=int(param_type_id),
            value=value)
        dom = self._post(path, '')
        assert dom.xpath('/response')[0].attrib['status'] == 'OK'

    def hack_manage_cutoff(self, shop_id, cutoff_numeric_id, action='open'):
        # Секретная ручка. Не описана.
        path = '/testing/shops/{shop_id}/cutoffs/{cutoff_numeric_id}'.format(
            shop_id=int(shop_id),
            cutoff_numeric_id=int(cutoff_numeric_id))
        method = {'open': 'PUT', 'close': 'DELETE'}[action]
        dom = self._request(path, method, '')

    def _prepay_request_status_put(self, status, request_id, shop_id):
        j = {
            'status': str(status),
            'uid': 1,
        }
        response = self._request_json(handle='/prepay-request/{}/status'.format(request_id), method='PUT', params={'datasourceId': shop_id, 'status': status, 'uid': 1}, json=j, load_json=False)
        return response

    def prepay_request_status_in_progress(self, request_id, shop_id):
        return self._prepay_request_status_put(status=PrepayStatus.IN_PROGRESS, request_id=request_id, shop_id=shop_id)

    def prepay_request_status_completed(self, request_id, shop_id):
        return self._prepay_request_status_put(status=PrepayStatus.COMPLETED, request_id=request_id, shop_id=shop_id)

    def prepay_request(self, shop_id):
        response = self._request_json(handle='/prepay-request', method='GET', params={'datasourceId': shop_id, 'format': 'json'}, load_json=False)
        return response

    def push_param_check(self, shop_id, param, status):
        response = self._request(path='/push-param-check', method='GET', params={'shop-id':shop_id,'param-type-id':param,'status':status}, xml=False, load_json=False)
        return response

    def old_prepay_type_status(self, status, shop_id):
        body = """<ya-money-shop-status-update>
            <datasources>
                <datasource>
                    <datasource-id>{datasource_id}</datasource-id>
                    <ya-money-status>{status}</ya-money-status>
                    <ya-money-shop-id>{shop_id}</ya-money-shop-id>
                    <ya-money-payment-types>
                        <type>PC</type>
                        <article-id>12345</article-id>
                        <scid-id>12345</scid-id>
                    </ya-money-payment-types>
                    <org-info>
                        <name>Kitay</name>
                        <org-type>OOO</org-type>
                        <address>Kitay</address>
                        <ogrn>1234567890987</ogrn>
                    </org-info>
                    <timestamp>{timestamp}</timestamp>
                </datasource>
            </datasources>
        </ya-money-shop-status-update>""".format(datasource_id=shop_id, status=status, timestamp=datetime.today().strftime('%d-%m-%Y %H:%M:%S'), shop_id=shop_id)

        response = self._request(path='/ya-money-shop-status', data=body, method='PUT', ct='application/xml')
        return response

    def _request_json(self, handle, method='GET', data=None, json=None, params={}, load_json=True):
        j = self._request(handle, method=method, params=params, xml=False, data=data, json=json, ct='application/json', load_json=load_json)
        return j

    def _get(self, path, params={}):
        return self._request(path, 'GET', params=params)

    def _post(self, path, data, params={}):
        return self._request(path, 'POST', data=data, ct='application/xml', params=params)

    def _request(self, path, method, data=None, json=None, params={}, ct=None, xml=True, load_json=True):
        assert path.startswith('/')
        url = '{base_url}{path}'.format(base_url=self.base_url, path=path)
        status_code = None
        data = _request(url, method=method, data=data, json=json, logger=self.logger, ct=ct, **params)
        if xml:
            return etree.fromstring(data)
        else:
            if load_json:
                json.loads(data)
            else:
                return data


class MbiBilling:
    # https://wiki.yandex-team.ru/MBI/NewDesign/components/mbi-billing/

    def __init__(self, base_url=None):
        self.base_url = base_url if base_url else Environment.get().mbi_billing_url
        self.logger = case_log.CaseLogger.get()

    def refill_balance(self, cmpg_id, amount):
        tpl = mbi_common_consts.BALANCE_CLIENT_NOTIFY_ORDER2_TPL
        data = tpl.format(
            cmpg_id=cmpg_id,
            amount=amount,
            tid=int(time.time()))
        url = '{base_url}/BalanceClient'.format(base_url=self.base_url)
        dom = etree.fromstring(_request(url, method='POST', data=data, logger=self.logger))
        assert len([t for t in dom.xpath('/methodResponse/params/param/value/array/data/value') if t.text == 'Success']) > 0

    def _run(self, command, sleep_for=10):
        netloc = urlparse(self.base_url).netloc
        host, port = netloc.split(':')
        cmd = "( echo '{command}' ; echo 'quit' ; sleep 1 ) | nc {host} {port}".format(host=host, port=port, command=command)
        self.logger.log('Executing: {}'.format(cmd))
        output = subprocess.check_output(cmd, shell=True)
        self.logger.log('Result: [[{}]]'.format(output))
        self.logger.log('Sleep {} sec...\n'.format(sleep_for))
        print 'Sleep {} sec...'.format(sleep_for)
        time.sleep(sleep_for)

    def _run_executer(self, executer_name):
        netloc = urlparse(self.base_url).netloc
        host, port = netloc.split(':')
        cmd = './mbi-billing-run.sh {host} {port} {executer}'.format(host=host, port=port, executer=executer_name)
        self.logger.log('CMD: [{}]'.format(cmd))
        subprocess.check_call(cmd, shell=True)

    def force_run_moderation_executor(self):
        self._run_executer('moderationExecutor')

    def force_run_fincnce_executor(self):
        self._run_executer('financeCutoffExecutor')
        # self.force_run_shop_state_report_executor()

    def force_run_cpa_technical_cutoff_need_info_executor(self):
        self._run_executer('cpaTechnicalCutoffNeedInfoExecutor')

    def disable_finance_executor(self):
         self._run('tms-reschedule getOrdersExecutor "20 0/1 * * * ? 2040"', sleep_for=0)
         self._run('tms-reschedule updateActualBalanceExecutor "35 0/1 * * * ? 2040"', sleep_for=0)
         self._run('tms-reschedule financeCutoffExecutor "0 0/5 * * * ? 2040"', sleep_for=0)

    def enable_finance_executor(self):
         print "ENABLE FINANCE EXECUTOR"
         self._run('tms-reschedule getOrdersExecutor "20 0/1 * * * ? "', sleep_for=0)
         self._run('tms-reschedule updateActualBalanceExecutor "35 0/1 * * * ? "', sleep_for=0)
         self._run('tms-reschedule financeCutoffExecutor "0 0/5 * * * ? "', sleep_for=0)

    def disable_moderation_executor(self):
         self._run('tms-reschedule moderationExecutor "0 2/5 * * * ? 2040"', sleep_for=0)

    def enable_moderation_executor(self):
         self._run('tms-reschedule moderationExecutor "0 2/5 * * * ?"', sleep_for=0)

    def force_run_shop_state_report_executor(self):
        # https://st.yandex-team.ru/MBI-20818#1496745966000
        # Александр Першуков
        # 6 июн 2017 13:46
        # Можно запустить джобу по требованию. Для этого нужно подключиться к порту 12346 и отправить туда строку tms-run shopStateReportExecutor.
        self._run_executer('shopStateReportExecutor')


def is_host_file_exists(self, host, path):
    try:
        subprocess.check_output('ssh {} test -e {}'.format(host, path), shell=True)
        return True
    except subprocess.CalledProcessError:
        return False


def get_host_file_content(self, host, path):
    return subprocess.check_output('ssh {} cat {}'.format(host, path), shell=True)


def register_shop(mbi_partner, is_global=True, enable_programs_tumblers=True, user=None, is_online=True, region=None, local_delivery_region=None):
    """ Create new shop.
    Now function creates shop with property is_global=true

    @param enable_programs_tumblers: https://st.yandex-team.ru/MBI-21731

    @return: PreCampaign
    """
    if region is None:
        region = 225 if not is_global else 134
    if local_delivery_region is None:
        local_delivery_region = 213 if not is_global else 10590
    cmpg = mbi_partner.pre_campaign_create(user=user, is_global=is_global, region=region, local_delivery_region=local_delivery_region, is_online=is_online)
    mbi_partner.pre_campaign_update(campaign_id=cmpg.campaign_id, is_global=is_global, region=region)
    mbi_partner.pre_campaign_request(campaign_id=cmpg.campaign_id)
    mbi_partner.register_campaign(campaign_id=cmpg.campaign_id, enable_programs_tumblers=enable_programs_tumblers)
    return cmpg


def _request(_url, method='GET', data=None, json=None, logger=None, ct=None, files=None, **params):
    # The name '_url' is carefully chosen. Don't change it!
    print 'REQ:', method, _url, params
    f = {'GET': requests.get, 'POST': requests.post, 'DELETE': requests.delete, 'PUT': requests.put}[method]
    headers = {}
    if ct:
        headers['Content-type'] = str(ct)

    b = benchmark()
    b.start()
    r = f(_url, stream=True, params=params, headers=headers, data=data, json=json, files=files)
    b.stop()
    # print("Time elapsed: {}".format(b.get_elapsed()))

    content = ''.join([x for x in r.iter_content(chunk_size=2048)])
    if logger:
        logger.log_request(r, content)
    return content


class T(unittest.TestCase):
    pass


def main():
    pass


if __name__ == '__main__':
    if os.environ.get('UNITTEST') == '1':
        unittest.main()
    else:
        main()
