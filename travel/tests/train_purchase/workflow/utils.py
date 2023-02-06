# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from travel.rasp.train_api.train_partners.ufs.get_order_info import TRANSACTION_INFO_ENDPOINT
from travel.rasp.train_api.train_partners.ufs.refund import REFUND_ENDPOINT
from travel.rasp.train_api.train_partners.ufs.refund_amount import REFUND_AMOUNT_ENDPOINT
from travel.rasp.train_api.train_partners.ufs.test_utils import mock_ufs


def mock_ufs_refund(httpretty, communication_error=False, refund_error=False):
    mock_ufs(httpretty, TRANSACTION_INFO_ENDPOINT, body='''\
<?xml version="1.0" encoding="Windows-1251" standalone="yes"?>
<UFS_RZhD_Gate>
    <TransID>48716057</TransID>
    <TStatus>0</TStatus>
    <Passenger BlankID="1">
        <DocType>ЗП</DocType>
        <DocNum>020062005</DocNum>
    </Passenger>
    <Passenger BlankID="2">
        <DocType>ЗП</DocType>
        <DocNum>020062005</DocNum>
    </Passenger>
    <Passenger BlankID="3">
        <DocType>ЗП</DocType>
        <DocNum>020062005</DocNum>
    </Passenger>
    <Passenger BlankID="4">
        <DocType>ЗП</DocType>
        <DocNum>020062005</DocNum>
    </Passenger>
</UFS_RZhD_Gate>''')

    mock_ufs(httpretty, REFUND_AMOUNT_ENDPOINT, body='''\
<?xml version="1.0" encoding="Windows-1251" standalone="yes"?>
<UFS_RZhD_Gate>
    <Blank ID="1">
        <Amount>6031.10</Amount>
    </Blank>
    <Blank ID="2">
        <Amount>2052.40</Amount>
    </Blank>
</UFS_RZhD_Gate>''')

    mock_ufs(httpretty, REFUND_ENDPOINT, body=('''\
<?xml version="1.0" encoding="Windows-1251" standalone="yes"?>
<UFS_RZhD_Gate>
    <Error />
    <Code>32</Code>
    <DescrId>5009</DescrId>
    <Descr>Ошибка авторизации</Descr>
</UFS_RZhD_Gate>''' if refund_error else '''\
<?xml version="1.0" encoding="Windows-1251" standalone="yes"?>
<UFS_RZhD_Gate>
    <Error />
    <Code>0</Code>
    <DescrId>5380</DescrId>
    <Descr></Descr>
</UFS_RZhD_Gate>''' if communication_error else '''\
<?xml version="1.0" encoding="Windows-1251" standalone="yes"?>
<UFS_RZhD_Gate>
    <RefundTransID>48716452</RefundTransID>
    <Fee>0.00</Fee>
    <TaxPercent>0.00</TaxPercent>
    <Amount>8083.5</Amount>
    <Blank ID="75714298" PrevID="2">
        <Amount>8083.5</Amount>
    </Blank>
</UFS_RZhD_Gate>'''))
