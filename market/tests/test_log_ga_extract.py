import pandas as pd
import pytest
import yatest.common
import yt.wrapper as yt
from market.assortment.ecom_log.lib.parsing_utils import get_shop_address


@pytest.fixture
def prepare_data():
    prod_ga_sales_1 = '//home/market/production/analytics_platform/shops/ga_sales/2021-03-16'
    prod_ga_sales_2 = '//home/market/production/analytics_platform/shops/ga_sales/2021-03-15'
    prod_ga_sales_3 = '//home/market/production/analytics_platform/shops/ga_sales/2021-03-14'
    prod_ga_dict = '//home/market/production/analytics_platform/analyst/ga_sales/geo_dictionary'
    test_ga_daily_sales_1 = '//home/market/testing/analytics_platform/refactoring/shops/ga_daily_sales/2021-03-15'
    test_ga_daily_sales_2 = '//home/market/testing/analytics_platform/refactoring/shops/ga_daily_sales/2021-03-14'
    test_ga_daily_sales_3 = '//home/market/testing/analytics_platform/refactoring/shops/ga_daily_sales/2021-03-13'
    prod_ga_daily_sales_3 = '//home/market/production/analytics_platform/shops/ga_daily_sales/2021-03-13'
    test_ga_sales_1 = '//home/market/testing/analytics_platform/refactoring/shops/ga_sales/2021-03-16'
    test_ga_sales_2 = '//home/market/testing/analytics_platform/refactoring/shops/ga_sales/2021-03-15'
    test_ga_dict = '//home/market/testing/analytics_platform/refactoring/analyst/dicts/geo_dictionary_ga'

    if yt.exists(test_ga_daily_sales_1):
        yt.remove(test_ga_daily_sales_1)

    if yt.exists(test_ga_daily_sales_2):
        yt.remove(test_ga_daily_sales_2)

    if yt.exists(test_ga_daily_sales_3):
        yt.remove(test_ga_daily_sales_3)

    df1 = pd.DataFrame(yt.read_table(prod_ga_sales_1, format=yt.YsonFormat(encoding='utf8')))
    df2 = pd.DataFrame(yt.read_table(prod_ga_sales_2, format=yt.YsonFormat(encoding='utf8')))
    df3 = pd.DataFrame(yt.read_table(prod_ga_sales_3, format=yt.YsonFormat(encoding='utf8')))
    daily_sales_3 = pd.DataFrame(yt.read_table(prod_ga_daily_sales_3, format=yt.YsonFormat(encoding='utf8')))
    geo_dict_real = pd.DataFrame(yt.read_table(prod_ga_dict, format=yt.YsonFormat(encoding='utf8')))

    df1['shop_address'] = df1.apply(lambda x: get_shop_address(x['shop_city'], x['shop_region'], None), axis=1)
    df2['shop_address'] = df2.apply(lambda x: get_shop_address(x['shop_city'], x['shop_region'], None), axis=1)
    df3['shop_address'] = df3.apply(lambda x: get_shop_address(x['shop_city'], x['shop_region'], None), axis=1)

    df1 = df1.sample(30, random_state=42)
    yt.write_table(test_ga_sales_1,
                   df1.to_dict(orient='records'), format=yt.JsonFormat(attributes={"encode_utf8": False})
                   )
    # симулируем ситуацию, когда в одной таблице ga_sales содержатся данные за несколько дней
    df2 = df2.sample(30, random_state=42)
    df3 = df3.sample(30, random_state=42)
    df2 = pd.concat([df2, df3])
    yt.write_table(test_ga_sales_2,
                   df2.to_dict(orient='records'), format=yt.JsonFormat(attributes={"encode_utf8": False})
                   )
    # дополнительно симулируем ситуацию, когда данные за день от df3 уже частично содержатся в daily_sales
    key_fields = ['shop_id_str', 'transaction_id', 'view_id']
    daily_sales_3 = \
        pd.merge(df3[key_fields], daily_sales_3, on=key_fields) \
        .sample(15, random_state=42)
    yt.write_table(test_ga_daily_sales_3,
                   daily_sales_3.to_dict(orient='records'), format=yt.JsonFormat(attributes={"encode_utf8": False})
                   )
    # симулируем отсутствие новых адресов в словаре geo_dict
    # генерим geo_dict, состоящий из половины адресов, фигурирующих во всех датасетах
    geo_dict = pd.merge(pd.concat([df1, df2, df3]), geo_dict_real)[['shop_address', 'geo_id']] \
        .drop_duplicates()
    geo_dict = geo_dict.to_dict(orient='records')
    geo_dict = [{'shop_address': x['shop_address'],
                 'geo_id': int(x['geo_id']) if pd.notnull(x['geo_id']) else None} for x in geo_dict]
    yt.write_table(test_ga_dict,
                   geo_dict, format=yt.JsonFormat(attributes={"encode_utf8": False})
                   )

    cli_call = [
        yatest.common.binary_path('market/assortment/ecom_log/ecom_log'),
        'PrepareDailyGALog',
        '--date', '2021-03-16',
        '--environment', 'testing'
    ]
    yatest.common.execute(cli_call)
    cli_call = [
        yatest.common.binary_path('market/assortment/ecom_log/ecom_log'),
        'PrepareDailyGALog',
        '--date', '2021-03-15',
        '--environment', 'testing'
    ]
    yatest.common.execute(cli_call)
    cli_call = [
        yatest.common.binary_path('market/assortment/ecom_log/ecom_log'),
        'GetGALog',
        '--date', '2021-03-13..2021-03-15',
        '--environment', 'testing'
    ]
    yatest.common.execute(cli_call)


def test_log_ga_extract(prepare_data):
    geo_dict = pd.DataFrame(
        yt.read_table('//home/market/testing/analytics_platform/refactoring/analyst/dicts/geo_dictionary_ga',
                      format=yt.YsonFormat(encoding='utf8'))
    )
    assert len(geo_dict) == 50
    assert len(geo_dict) == len(geo_dict['shop_address'].unique())

    daily_sales_1 = pd.DataFrame(
        yt.read_table('//home/market/testing/analytics_platform/refactoring/shops/ga_daily_sales/2021-03-15',
                      format=yt.YsonFormat(encoding='utf8'))
    )
    assert len(daily_sales_1) == 30

    daily_sales_2 = pd.DataFrame(
        yt.read_table('//home/market/testing/analytics_platform/refactoring/shops/ga_daily_sales/2021-03-14',
                      format=yt.YsonFormat(encoding='utf8'))
    )
    assert len(daily_sales_2) == 30

    daily_sales_3 = pd.DataFrame(
        yt.read_table('//home/market/testing/analytics_platform/refactoring/shops/ga_daily_sales/2021-03-13',
                      format=yt.YsonFormat(encoding='utf8'))
    )
    assert len(daily_sales_3) == 30

    sales_1 = pd.DataFrame(
        yt.read_table('//home/market/testing/analytics_platform/refactoring/analyst/sources/ga/2021-03-15',
                      format=yt.YsonFormat(encoding='utf8'))
    )
    assert len(sales_1) == 30

    sales_2 = pd.DataFrame(
        yt.read_table('//home/market/testing/analytics_platform/refactoring/analyst/sources/ga/2021-03-14',
                      format=yt.YsonFormat(encoding='utf8'))
    )
    assert len(sales_2) == 30

    sales_3 = pd.DataFrame(
        yt.read_table('//home/market/testing/analytics_platform/refactoring/analyst/sources/ga/2021-03-13',
                      format=yt.YsonFormat(encoding='utf8'))
    )
    assert len(sales_3) == 29
