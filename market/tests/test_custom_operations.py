import pandas as pd
import pytest
import yatest.common
import yt.wrapper as yt


def write_data_frame(path, df):
    if yt.exists(path):
        yt.remove(path)
    rows = df.where(pd.notnull(df), None).to_dict(orient='records')
    for row in rows:
        for key in row:
            if isinstance(row[key], float):
                if int(row[key]) == row[key]:
                    row[key] = int(row[key])
    yt.write_table(path, rows, format=yt.JsonFormat(attributes={"encode_utf8": False}))


@pytest.fixture
def prepare_data():
    fake_pharma_vendors = pd.DataFrame([
        [8367655, 90521, "Mirra", 18057301, 754302048],
        [8367655, 90521, "Mirra", 18057301, 754303027],
        [1017943799, 90521, "Ринфолтил", 17943799, 658440086],
        [1017943799, 90521, "Ринфолтил", 17943799, 674542486],
    ],
        columns=["fake_id", "hid", "medicine_name", "medicine_option", "model_id"]
    )
    write_data_frame('//home/market/testing/analytics_platform/refactoring/analyst/dicts/farma_models_to_fake_vendor',
                     fake_pharma_vendors)

    sales_channels = pd.DataFrame([
        ['wildberries.ru', 'marketplace', None],
        ['apteka.ru', 'specialist_pharmacy', 'Товары для здоровья'],
        ['rbt.ru', 'techChain', None],
    ],
        columns=['domain', 'channel', 'department']
    )
    write_data_frame('//home/market/testing/analytics_platform/refactoring/analyst/sales_channels',
                     sales_channels)

    bwlist = pd.DataFrame([
        [[90401], 'sibkedry.ru', '1.0', 'nan', [None], [None]],
        [[13096190, 16732562, 91183, 91618], 'rbt.ru', '1.0', 'nan', [None], [None]]
    ],
        columns=['black_hids', 'domain', 'uncertainty_flag', 'end_date', 'vendor_null_hids', 'white_hids']
    )
    write_data_frame('//home/market/testing/analytics_platform/refactoring/analyst/dicts/white_black_list/latest',
                     bwlist)

    beru_added_path = '//home/market/testing/analytics_platform/refactoring/analyst/beru_added/2021-03-16'
    beru_added = pd.DataFrame([
        ["wildberries.ru", 7812065, None, 9146., None],
        ['apteka.ru', 90521, 658440086, 1057., 8512905],
        ['apteka.ru', 90521, 658440086, 1057., 1111111],
        ["sibkedry.ru", 16089018, None, 700., 16337464],
        ["lenta.com", 12943705, None, 94.38, None],
        ['rbt.ru', 16686700, 1111, None, 15104727],
        ['rbt.ru', 13096190, 2222, None, 8338378]
    ],
        columns=['domain', 'hid', 'model_id', 'price', 'vendor_id']
    )
    write_data_frame(beru_added_path, beru_added)

    cli_call = [
        yatest.common.binary_path('market/assortment/ecom_log/ecom_log'),
        'CustomOperations',
        '--date', '2021-03-16',
        '--environment', 'testing'
    ]
    yatest.common.execute(cli_call)


def test_log_ga_extract(prepare_data):
    final = pd.DataFrame(
        yt.read_table('//tmp/market-analyst/ecom_log/custom_operations_2021-03-16',
                      format=yt.YsonFormat(encoding='utf8'))
    )
    # проверяем, сколько записей не ушло в чёрный список
    assert len(final) == 5
    # проверяем, что каналы подставились правильно
    assert len(final[(final['domain'] == 'apteka.ru') & (final['channel'] == 'specialist_pharmacy')]) == 2
    assert len(final[(final['domain'] == 'lenta.com') & (final['channel'] == 'generalist')]) == 1
    assert len(final[(final['domain'] == 'rbt.ru') & (final['channel'] == 'techChain')]) == 1
    assert len(final[(final['domain'] == 'wildberries.ru') & (final['channel'] == 'marketplace')]) == 1
    # проверяем, что для фармы подставился фейковый vendor_id
    assert len(final[(final['domain'] == 'apteka.ru') & (final['vendor_id'] == 1017943799)]) == 2
    # проверяем, что для нефармы из списка vendor_id остался тем же
    assert len(final[(final['domain'] == 'rbt.ru') & (final['vendor_id'] == 15104727)]) == 1

    blacklisted = pd.DataFrame(
        yt.read_table('//home/market/testing/analytics_platform/refactoring/analyst/transactions/blacklisted/2021-03-16',
                      format=yt.YsonFormat(encoding='utf8'))
    )
    # проверяем, сколько записей отфильтровалось по чёрному списку
    assert len(blacklisted) == 2
    # проверяем, что отфильтровалась запись по листовому hid
    assert len(blacklisted[(blacklisted['domain'] == 'rbt.ru') & (blacklisted['hid'] == 13096190)]) == 1
    # проверяем, что отфильтровалась запись по нелистовому hid
    assert len(blacklisted[(blacklisted['domain'] == 'sibkedry.ru') & (blacklisted['hid'] == 16089018)]) == 1
