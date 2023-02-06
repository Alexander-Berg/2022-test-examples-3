# -*- coding: utf-8 -*-
import tempfile
import os
import pytest
from contextlib import contextmanager
from urllib import quote_plus

from yt.wrapper import ypath_join
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.pylibrary import slug


MODEL_BIN = os.path.join('market', 'idx', 'export', 'awaps', 'market-banner-models', 'bin', 'market-banner-models')
MODEL_BIN_EX = os.path.join('market', 'idx', 'export', 'awaps', 'market-banner-models', 'bin-ex', 'market-banner-models-ex')


def msku_url(blue_domain, model_id, market_sku, title, ware_md5, published_on_market, utm_term="", utm_source=""):
    utm_term = quote_plus(utm_term) if utm_term else None
    utm_source = quote_plus(utm_source) if utm_source else None
    return 'https://{blue_domain}/product--{slug}/{id}?sku={msku}{add_utm_source}{add_utm_term}'.format(
        blue_domain=blue_domain,
        id=str(model_id),
        msku=str(market_sku),
        slug=slug.translit(title),
        add_utm_term='&utm_term={}'.format(utm_term) if utm_term else '',
        add_utm_source='&utm_source={}'.format(utm_source) if utm_source else ''
    ) if published_on_market else 'https://{blue_domain}/offer/{ware_md5}{add_utm_source}{add_utm_term}'.format(
        blue_domain=blue_domain,
        ware_md5=ware_md5,
        add_utm_term='&utm_term={}'.format(utm_term) if utm_term else '',
        add_utm_source='?utm_source={}'.format(utm_source) if utm_source else ''
    )


def msku_url_doware(blue_domain, model_id, market_sku, title, ware_md5, published_on_market, utm_term=""):
    utm_term = quote_plus(utm_term) if utm_term else None
    return 'https://{blue_domain}/product--{slug}/{id}?offerid={ware_md5}&sku={msku}{add_utm_term}'.format(
        blue_domain=blue_domain,
        id=str(model_id),
        msku=str(market_sku),
        slug=slug.translit(title),
        ware_md5=ware_md5,
        add_utm_term='&utm_term={}'.format(utm_term) if utm_term else '',
    ) if published_on_market else 'https://{blue_domain}/offer/{ware_md5}{add_utm_term}'.format(
        blue_domain=blue_domain,
        ware_md5=ware_md5,
        add_utm_term='?utm_term={}'.format(utm_term) if utm_term else '',
    )


def create_table(yt, table_name, schema):
    yt.create(
        'table',
        table_name,
        ignore_existing=True,
        recursive=True,
        attributes=dict(schema=schema)
    )


@contextmanager
def TemporaryDirectory():
    import shutil

    name = tempfile.mkdtemp()
    try:
        yield name
    finally:
        shutil.rmtree(name)


@pytest.fixture(scope='module')
def categories_table(yt_server):
    table_name = ypath_join(get_yt_prefix(), 'in', 'categories')

    yt = yt_server.get_yt_client()
    create_table(
        yt, table_name,
        schema=[dict(name="hyper_id", type="int64"),
                dict(name="id", type="int64"),
                dict(name="nid", type="int64"),
                dict(name="blue_nid", type="int64"),
                dict(name="name", type="string"),
                dict(name="uniq_name", type="string"),
                dict(name="parent", type="int64"),
                dict(name="parents", type="string"),
                dict(name="type", type="string")])

    data = [dict(hyper_id=1111111, id=1, nid=123, blue_nid=111, name="Base", uniq_name="Base", parent=0, parents="1,", type="simple"),
            dict(hyper_id=2222222, id=2, nid=125, blue_nid=222, name="Derived", uniq_name="Derived", parent=1111111, parents="1,2,",
                 type="guru")]

    yt.write_table(table_name, data)

    return table_name


def cantor_pairing(first, second):
    return int((first + second) * (first + second + 1) // 2) + second
