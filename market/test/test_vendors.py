# -*- coding: utf-8 -*-
import re
import textwrap
import os
import yatest.common
from yt.wrapper import ypath_join
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from util import create_table, TemporaryDirectory, categories_table, MODEL_BIN

assert categories_table


def _create_vendors_table(yt, data, table_name):
    create_table(yt, table_name,
                 schema=[dict(name="category_id", type="int64"),
                         dict(name="category_name", type="string"),
                         dict(name="category_nid", type="int64"),
                         dict(name="picture", type="string"),
                         dict(name="vendor", type="string"),
                         dict(name="vendor_id", type="int64"),
                         dict(name="price", type="double"),
                         dict(name="noffers", type="int64"),
                         dict(name="shops", type="int64"),
                         ]
                 )

    yt.write_table(table_name, data)


def test_banner_offers(yt_server, categories_table):
    """
    Положительный тест на создание офера категория-вендор
    Расчет id для offer происходит по формуле https://en.wikipedia.org/wiki/Pairing_function
    для обеспечения уникальности.
    """

    data = [dict(category_id=1111111,
                 category_name="Base",
                 category_nid=123,
                 picture='//avatars.mds.yandex.net/get-mpic/1111/img_id1111/orig',
                 vendor='Spb',
                 vendor_id=1,
                 price=100500.1,
                 noffers=23,
                 shops=1),
            dict(category_id=2222222,
                 category_name="Derived",
                 picture='//avatars.mds.yandex.net/get-mpic/2222/img_id2222/orig',
                 vendor='Msk',
                 vendor_id=2,
                 price=100500.2,
                 noffers=32,
                 shops=10)
            ]

    result = """\
    <?xml version="1.0" encoding="UTF-8"?>
    <yml_catalog date="2017-03-28 15:22">
     <shop>
      <name>Yandex Market</name>
      <company>Yandex</company>
      <url>https://market.yandex.ru</url>
      <currencies>
       <currency id="RUB" rate="1"/>
      </currencies>
      <categories>
       <category id="1111111">Base</category>
       <category id="2222222" parentId="1111111">Derived</category>
      </categories>
      <offers>
       <offer id="617285493829" available="true">
        <url>https://market.yandex.ru/catalog--base/123/list?hid=1111111&amp;glfilter=7893318%3A1</url>
        <price>100500</price>
        <currencyId>RUB</currencyId>
        <categoryId>1111111</categoryId>
        <picture>https://avatars.mds.yandex.net/get-mpic/1111/img_id1111/9</picture>
        <name>Base</name>
        <vendor>Spb</vendor>
        <quantity>23</quantity>
        <merchants>1</merchants>
       </offer>
       <offer id="2469140864202" available="true">
        <url>https://market.yandex.ru/catalog--derived/2222222/list?glfilter=7893318%3A2</url>
        <price>100500</price>
        <currencyId>RUB</currencyId>
        <categoryId>2222222</categoryId>
        <picture>https://avatars.mds.yandex.net/get-mpic/2222/img_id2222/9</picture>
        <name>Derived</name>
        <vendor>Msk</vendor>
        <quantity>32</quantity>
        <merchants>10</merchants>
       </offer>
      </offers>
     </shop>
    </yml_catalog>
    """

    result = textwrap.dedent(result)

    vendors_table_name = ypath_join(get_yt_prefix(), 'in', 'vendors')
    _create_vendors_table(yt_server.get_yt_client(), data, vendors_table_name)

    with TemporaryDirectory() as tempdir:
        path = os.path.join(tempdir, 'models.xml')
        cmdlist = [
            yatest.common.binary_path(MODEL_BIN),
            '--proxy', yt_server.get_server(),
            '--input', vendors_table_name,
            '--categories-tree', categories_table,
            '--output', path,
            '--feed', 'vendors',
        ]

        yatest.common.execute(cmdlist)
        with open(path) as f:
            content = re.sub(r'date="(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2})"', 'date="2017-03-28 15:22"', f.read())

        assert content == result
