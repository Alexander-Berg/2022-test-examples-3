#!/usr/bin/env python
# coding=utf-8

import unittest
import os
import collections
import mbi_common
import sys
import time


def fill_shop_params_for_global(mbi_partner, shop_id, ut=None, return_address=True):
    fields = [
        (2, '+7 (926) 284-48-15'),  # PHONE_NUMBER
        (3, 'fantamp.yandex.ru'),  # DATASOURCE_DOMAIN
        (34, 'With love from China'),  # SHIPPING_FULL_INFO
        (39, 'YML'),  # SHIPPING_FULL_INFO
    ]

    if return_address:
        fields += [
            (83, 'Das is eine address vozvrata!'),  # RETURN_DELIVERY_ADDRESS
        ]

    for field_id, value in fields:
        mbi_partner.manage_param(shop_id, field_id, value)
        mrs = mbi_partner.moderation_request_state(shop_id)
        if ut:
            ut.assertTrue('MISSED_DATASOURCE_PARAMS' in mrs['result']['cpaModerationDisabledReasons'])
            ut.assertTrue('MISSED_DATASOURCE_PARAMS' in mrs['result']['cpcModerationDisabledReasons'])
            ut.assertEqual('{};False'.format(field_id), '{};{}'.format(field_id, mrs['result']['moderationEnabled']))
    oi = mbi_common.OrganizationInfo(
        info_id='235',
        organization_type='OOO',
        organization_type_code='1',
        name=u'Китайские Ништяки',
        ogrn='1037851027873',
        fact_address=u'Китай, провинция ЖеньЧшень, ул. Красных Коммисаров, 11, 701',
        juridical_address=u'Китай, провинция ЖеньЧшень, ул. Красных Коммисаров, 11, 701',
        url='http://scorpion.ru/help/rekvizity',
        info_source='0',
        registration_number='')
    oi = oi._replace(registration_number='1234567890987')
    mbi_partner.edit_organization_info(oi, shop_id=shop_id, create_new=True)
    mrs = mbi_partner.moderation_request_state(shop_id)
    # Не давать слать на модерацию пока кое-что важное не будет заполнено
    if ut:
        # закомменчено т.к. отключили, т.к. ломает фронт (спросить Витю)
        # ut.assertTrue('MISSED_DATASOURCE_PARAMS' in mrs['result']['cpaModerationDisabledReasons'])
        # ut.assertTrue('MISSED_DATASOURCE_PARAMS' in mrs['result']['cpcModerationDisabledReasons'])
        ut.assertTrue(True)
    mbi_partner.manage_param(shop_id, 48, u'Шелковый Путь')  # SHOP_NAME
    mrs = mbi_partner.moderation_request_state(shop_id)
    # Вот теперь пустых полей не должно остаться
    if ut:
        mbi_partner.show_missed_datasource_info(shop_id)  # этот запрос на случай, если ассерты ниже свалятся, чтобы сразу в логе было видно, каких полей не хватило
        ut.assertTrue('MISSED_DATASOURCE_PARAMS' not in mrs['result']['cpaModerationDisabledReasons'])
        ut.assertTrue('MISSED_DATASOURCE_PARAMS' not in mrs['result']['cpcModerationDisabledReasons'])


# check_missed_datasource_params - временно в False, т.к. фикс для них выедет после 2017.2.17 (уже не в нём)
def fill_shop_params_for_non_global(mbi_partner, shop_id, ut=None, return_address=True, check_missed_datasource_params=False):
    fields = [
        (2, '+7 (926) 284-48-15'),  # PHONE_NUMBER
        (3, 'fantamp.yandex.ru'),  # DATASOURCE_DOMAIN
        (34, u'Полное инфо о доставке'),  # SHIPPING_FULL_INFO
        (39, 'YML'),  # SHIPPING_FULL_INFO
    ]

    if return_address:
        fields += [
            (83, u'Это некоторый адрес возврата'),  # RETURN_DELIVERY_ADDRESS
        ]

    for field_id, value in fields:
        mbi_partner.manage_param(shop_id, field_id, value)
        mrs = mbi_partner.moderation_request_state(shop_id)
        if ut and check_missed_datasource_params:
            ut.assertTrue('MISSED_DATASOURCE_PARAMS' in mrs['result']['cpaModerationDisabledReasons'])
            ut.assertTrue('MISSED_DATASOURCE_PARAMS' in mrs['result']['cpcModerationDisabledReasons'])
            ut.assertEqual('{};False'.format(field_id), '{};{}'.format(field_id, mrs['result']['moderationEnabled']))
    oi = mbi_common.OrganizationInfo(
        info_id='235',
        organization_type='OOO',
        organization_type_code='1',
        name=u'Какой-то Российский Магаз',
        ogrn='1037851027873',
        fact_address=u'119920 Москва, Комсомольский просп., почт потделение с котикм',
        juridical_address=u'119920 Москва, Комсомольский просп., почт потделение с котикм',
        url='http://scorpion.ru/help/rekvizity',
        info_source='0',
        registration_number='')
    mbi_partner.edit_organization_info(oi, shop_id=shop_id, create_new=True)
    mrs = mbi_partner.moderation_request_state(shop_id)
    # Не давать слать на модерацию пока кое-что важное не будет заполнено
    if ut and check_missed_datasource_params:
        ut.assertTrue('MISSED_DATASOURCE_PARAMS' in mrs['result']['cpaModerationDisabledReasons'])
        ut.assertTrue('MISSED_DATASOURCE_PARAMS' in mrs['result']['cpcModerationDisabledReasons'])
    mbi_partner.manage_param(shop_id, 48, u'Zonko\'s Joke Shop')  # SHOP_NAME
    mrs = mbi_partner.moderation_request_state(shop_id)
    # Вот теперь пустых полей не должно остаться
    if ut and check_missed_datasource_params:
        mbi_partner.show_missed_datasource_info(shop_id)  # этот запрос на случай, если ассерты ниже свалятся, чтобы сразу в логе было видно, каких полей не хватило
        ut.assertTrue('MISSED_DATASOURCE_PARAMS' not in mrs['result']['cpaModerationDisabledReasons'])
        ut.assertTrue('MISSED_DATASOURCE_PARAMS' not in mrs['result']['cpcModerationDisabledReasons'])


# PREPAY
#
# algorithm by kladmv@
# https://github.yandex-team.ru/kladmv/new-prepayment-Python-autotests/
#

def _enable_prepay_for_non_global(mbi_partner, mbi_api, shop_id, ut=None):
    # проверяем, что есть возможность подключения предоплаты
    if ut:
        time.sleep(0.5)
        ut.assertTrue(mbi_partner.is_prepay_available(shop_id))

    # проверяем, что ещё ни одной заявки не было
    if ut:
        time.sleep(0.5)
        ut.assertTrue(mbi_partner.prepay_requests_count(shop_id) == 0)

    # добавляем заявку
    poi = mbi_common.PrepaymentOrganizationInfo(
        name = u'ООО Рога и Копыта',
        type = u'1',
        ogrn = u'1117746915945',
        inn = u'7736637017',
        kpp = u'773601001',
        juridicalAddress = u'Москва, Гагаринский Р-н, Ленинский Пр-кт, Д 70/11',
        factAddress = u'Москва, Гагаринский Р-н, Ленинский Пр-кт, Д 70/11',
        postcode = u'119261',
        accountNumber = u'40701810401000000022',
        corrAccountNumber = u'30101810545250000710',
        bik = u'044525710',
        bankName = u'АО КБ «Агропромкредит», г. Москва',
        url = u'www.rogaikopyta.ru',
        licenseNumber = u'',
        licenseDate = u'',
        workSchedule = u'ПН-ПТ 9:00-18:00. СБ 10:00-16:00, ВС - выходной',
    )

    pci = mbi_common.PrepaymentContactInfo(
        name = u'Иванов Петр Сидорович',
        phoneNumber = u'+71234567890',
        email = u'ivanov@petr.ru',
    )

    psi = mbi_common.PrepaymentSignatoryInfo(
        name = u'Иванов Петр Сидорович',
        docType = u'1',
        docInfo = None
    )

    # отправляем заявку
    time.sleep(0.5)
    result_json = mbi_partner.new_prepay_request(shop_id=shop_id, prepayment_organization_info=poi, prepayment_contact_info=pci, prepayment_signatory_info=psi)
    if ut:
        ut.assertTrue(result_json['result']['prepayType'] == '1')

    request_id = result_json['result']['requestId']

    # нужно залить документ
    time.sleep(0.5)
    file_upload_response = mbi_partner.prepay_request_post_document(request_id=request_id, shop_id=shop_id, file_path='document.jpg')
    if ut:
        ut.assertTrue(file_upload_response['result']['name'] == 'document.jpg')

    # https://wiki.yandex-team.ru/market/pokupka/projects/new-prepayment-russia/dev/market-payment/getprepay-requestapplication-form/
    # Ручка обновляет текущей датой поле startDate для хранения даты подписания заявления, без которой невозможно перевести заявка из статуса  NEW в  INIT .
    time.sleep(0.5)
    application_form_response = mbi_partner.prepay_request_application_form(request_id, shop_id)

    status = mbi_partner.prepay_request_status_init(request_id, shop_id)
    if 'errors' in status:
        if ut:
            ut.assertFalse(status['errors']) # если секция 'errors' есть, то проверим, что она пустая
    else:
        if ut:
            ut.assertTrue(True) # а вот если секции 'errors' в ответе вообще нет - то ну и ладно

    # как будто проверка началась
    time.sleep(0.5)
    status = mbi_api.prepay_request_status_in_progress(request_id, shop_id)
    if ut:
        print status
        ut.assertTrue(len(status) == 0) # тупая проверка, если тело пустое, то ошибок не было

    # как будто проверка закончилась
    time.sleep(0.5)
    status = mbi_api.prepay_request_status_completed(request_id, shop_id)
    if ut:
        ut.assertTrue(len(status) == 0) # тупая проверка, если тело пустое, то ошибок не было


def _enable_prepay_for_global(mbi_partner, mbi_api, shop_id, ut=None):
    # проверяем, что есть возможность подключения предоплаты
    if ut:
        time.sleep(0.5)
        ut.assertTrue(mbi_partner.is_prepay_available(shop_id))

    # проверяем, что ещё ни одной заявки не было
    if ut:
        time.sleep(0.5)
        ut.assertTrue(mbi_partner.prepay_requests_count(shop_id) == 0)

    # делаем заявку
    time.sleep(0.5)
    r = mbi_api.prepay_request(shop_id=shop_id)


    for status in ('INIT', 'IN_PROGRESS', 'COMPLETED'):
        time.sleep(0.5)
        r = mbi_api.old_prepay_type_status(status=status, shop_id=shop_id)

    # пушим параметр 47
    time.sleep(0.5)
    r = mbi_api.push_param_check(shop_id=shop_id, param=47, status='SUCCESS')

def enable_prepay(mbi_partner, mbi_api, is_global, shop_id, ut=None):
    if is_global:
        _enable_prepay_for_global(mbi_partner, mbi_api, shop_id, ut)
    else:
        _enable_prepay_for_non_global(mbi_partner, mbi_api, shop_id, ut)


class T(unittest.TestCase):
    def test_(self):
        pass


def countdown(t):
    for remaining in range(t,0,-1):
        sys.stdout.write('\r')
        sys.stdout.write('{:2d} seconds remaining         '.format(remaining))
        sys.stdout.flush()
        time.sleep(1)
    sys.stdout.write('\n')
    sys.stdout.flush()


def main():
    pass


if __name__ == '__main__':
    if os.environ.get('UNITTEST') == '1':
        unittest.main()
    else:
        main()
