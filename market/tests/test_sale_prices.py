from market.dynamic_pricing.pricing.library.utils import (
    create_dataframe
)
import yatest.common


class __YaTestEnv:  # for "ya make -t ..."
    def __init__(self):
        import yatest.common
        self.python_bin = yatest.common.python_path()
        self.work_dir = yatest.common.work_path()
        self.binary_path = yatest.common.binary_path
        self.source_path = yatest.common.source_path


_env = __YaTestEnv()


def _test_unique_multi_col_values(df, multi_col_names):
    return df.groupby(multi_col_names).nunique().max().max() == 1


def _test_price_values(sale_prices):
    return ((sale_prices["price"] > 0).all())


def validate_prices(sale_prices):
    assert(_test_unique_multi_col_values(sale_prices, ['market_sku', 'shop_sku', 'warehouse_id']))
    assert(_test_price_values(sale_prices))


def test_real_rules():
    '''
    Проверяем, что ручные цены соответсвуют здравому смыслу
    '''
    base_path = yatest.common.source_path('market/dynamic_pricing/pricing/deadstock_sales/rules_uploader')

    sale_prices_df = create_dataframe(base_path + '/sale_prices.csv')
    if not sale_prices_df.empty:
        validate_prices(sale_prices_df)
