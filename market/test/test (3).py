# -*- coding: utf-8 -*-
import os
import textwrap
import re
import yatest.common
import pytest
from yt.wrapper import ypath_join
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from util import create_table, TemporaryDirectory, categories_table, MODEL_BIN

assert categories_table


def _create_models_table(yt, data, table_name):
    create_table(yt, table_name,
                 schema=[dict(name="id", type="int64"),
                         dict(name="category_id", type="int64"),
                         dict(name="price", type="double"),
                         dict(name="oldprice", type="double"),
                         dict(name="picture", type="string"),
                         dict(name="title", type="string"),
                         dict(name="vendor", type="string"),
                         dict(name="typePrefix", type="string"),
                         dict(name="noffers", type="int64"),
                         dict(name="region", type="int32")])

    yt.write_table(table_name, data)


@pytest.mark.skip(reason='MARKETINDEXER-23168 remove this feed')
def test_banner_models_cart(yt_server, categories_table):  # noqa
    data = [dict(id=239, category_id=2222222, price=100500.1, picture='//avatars.mds.yandex.net/get-mpic/1111/img_id1111/orig', title='Stadium Spb Arena',
                 vendor='SPb', typePrefix='Stadium', noffers=6, oldprice=100501.2, region=213)]

    result = """\
    <?xml version="1.0" encoding="UTF-8"?>
    <yml_catalog date="2017-03-29 15:23">
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
       <offer id="239" available="true">
        <url>https://market.yandex.ru/cart</url>
        <price>100500.1</price>
        <currencyId>RUB</currencyId>
        <categoryId>2222222</categoryId>
        <picture>https://avatars.mds.yandex.net/get-mpic/1111/img_id1111/9</picture>
        <name>Stadium Spb Arena</name>
        <vendor>SPb</vendor>
       </offer>
      </offers>
     </shop>
    </yml_catalog>
    """

    result = textwrap.dedent(result)

    models_table_name = ypath_join(get_yt_prefix(), 'in', 'models_cart')
    _create_models_table(yt_server.get_yt_client(), data, models_table_name)
    with TemporaryDirectory() as tempdir:
        path = os.path.join(tempdir, 'models-cart.xml')
        cmdlist = [
            yatest.common.binary_path(MODEL_BIN),
            '--proxy', yt_server.get_server(),
            '--input', models_table_name,
            '--categories-tree', categories_table,
            '--output', path,
            '--feed', 'models-cart',
            '--min-offers', '5'
        ]

        yatest.common.execute(cmdlist)
        with open(path) as f:
            content = re.sub(r'date="(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2})"', 'date="2017-03-29 15:23"', f.read())

        assert content == result


def test_banner_models_by_region(yt_server, categories_table):  # noqa
    '''
    Проверяем работу обновленной модельной выгрузки. Помимо фильтрации по количеству оферов, фильтруем по региону.
    '''
    data = [dict(id=239, category_id=2222222, price=100500.1,
                 picture='//avatars.mds.yandex.net/get-mpic/1111/img_id1111/orig', title='Stadium Spb Arena',
                 vendor='SPb', typePrefix='Stadium', noffers=5, oldprice=100501.2, region=213),
            dict(id=95, category_id=1111111, price=100500.2,
                 picture='//avatars.mds.yandex.net/get-mpic/2222/img_id2222/orig', title='Veb Arena',
                 vendor='Msk', typePrefix='Stadium', noffers=5, oldprice=100502.3, region=213),
            dict(id=239, category_id=2222222, price=100501.1,
                 picture='//avatars.mds.yandex.net/get-mpic/1111/img_id1111/orig', title='Stadium Spb Arena',
                 vendor='SPb', typePrefix='Stadium', noffers=5, oldprice=100505.2, region=2),
            dict(id=95, category_id=1111111, price=100502.2,
                 picture='//avatars.mds.yandex.net/get-mpic/2222/img_id2222/orig', title='Veb Arena',
                 vendor='Msk', typePrefix='Stadium', noffers=5, oldprice=100506.3, region=2),
            dict(id=239, category_id=2222222, price=100503.1,
                 picture='//avatars.mds.yandex.net/get-mpic/1111/img_id1111/orig', title='Stadium Spb Arena',
                 vendor='SPb', typePrefix='Stadium', noffers=5, oldprice=100507.2, region=39),
            dict(id=95, category_id=1111111, price=100504.2,
                 picture='//avatars.mds.yandex.net/get-mpic/2222/img_id2222/orig', title='Veb Arena',
                 vendor='Msk', typePrefix='Stadium', noffers=3, oldprice=100508.3, region=39)]

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
       <offer id="239" available="true">
        <url>https://market.yandex.ru/product--stadium-spb-arena/239</url>
        <price>100503.1</price>
        <oldprice>100507.2</oldprice>
        <currencyId>RUB</currencyId>
        <categoryId>2222222</categoryId>
        <picture>https://avatars.mds.yandex.net/get-mpic/1111/img_id1111/9</picture>
        <name>Stadium Spb Arena</name>
        <vendor>SPb</vendor>
       </offer>
      </offers>
     </shop>
    </yml_catalog>
    """

    result = textwrap.dedent(result)

    models_table_name = ypath_join(get_yt_prefix(), 'in', 'models_region')
    _create_models_table(yt_server.get_yt_client(), data, models_table_name)

    with TemporaryDirectory() as tempdir:
        path = os.path.join(tempdir, 'models.xml')
        cmdlist = [
            yatest.common.binary_path(MODEL_BIN),
            '--proxy', yt_server.get_server(),
            '--input', models_table_name,
            '--categories-tree', categories_table,
            '--output', path,
            '--min-offers', '5',
            '--feed', 'models-ex',
            '--region', '39'
        ]

        yatest.common.execute(cmdlist)
        with open(path) as f:
            content = re.sub(r'date="(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2})"', 'date="2017-03-28 15:22"', f.read())

        assert content == result


def test_banner_models_with_description(yt_server, categories_table):  # noqa
    data = [dict(id=239, category_id=2222222, price=100503.1,
                 picture='//avatars.mds.yandex.net/get-mpic/1111/img_id1111/orig', title='Stadium Spb Arena',
                 vendor='SPb', typePrefix='Stadium', noffers=5, oldprice=100507.2, region=39)]

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
       <offer id="239" available="true">
        <url>https://market.yandex.ru/product--stadium-spb-arena/239</url>
        <price>100503.1</price>
        <oldprice>100507.2</oldprice>
        <currencyId>RUB</currencyId>
        <categoryId>2222222</categoryId>
        <picture>https://avatars.mds.yandex.net/get-mpic/1111/img_id1111/9</picture>
        <name>Stadium Spb Arena</name>
        <description>Sale!!!</description>
        <vendor>SPb</vendor>
       </offer>
      </offers>
     </shop>
    </yml_catalog>
    """

    result = textwrap.dedent(result)

    models_table_name = ypath_join(get_yt_prefix(), 'in', 'models_region')
    _create_models_table(yt_server.get_yt_client(), data, models_table_name)

    with TemporaryDirectory() as tempdir:
        path = os.path.join(tempdir, 'models.xml')
        cmdlist = [
            yatest.common.binary_path(MODEL_BIN),
            '--proxy', yt_server.get_server(),
            '--input', models_table_name,
            '--categories-tree', categories_table,
            '--output', path,
            '--min-offers', '5',
            '--feed', 'models-ex',
            '--region', '39',
            '--description', 'Sale!!!'
        ]

        yatest.common.execute(cmdlist)
        with open(path) as f:
            content = re.sub(r'date="(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2})"', 'date="2017-03-28 15:22"', f.read())

        assert content == result
