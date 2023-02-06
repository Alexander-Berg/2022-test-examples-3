from market.dynamic_pricing.pricing.deadstock_sales.rules_uploader.uploader import (
    prepare_rules
)
from market.dynamic_pricing.pricing.library.constants import (
    DEADSTOCK_BASE_PRICE_RULES
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


def validate_status_rules(status_rules):
    assert ((status_rules["days_on_stock"] > 0).all())
    assert ((status_rules["days_available"] > 0).all())
    assert ((status_rules["first_stage_until"] < status_rules["second_stage_until"]).all())
    assert ((status_rules["second_stage_until"] < status_rules["third_stage_until"]).all())
    assert ((status_rules["third_stage_until"] < status_rules["fourth_stage_until"]).all())


def validate_price_rules(price_rules):
    assert (price_rules["base_price_rule"].isin(DEADSTOCK_BASE_PRICE_RULES).all())

    assert ((price_rules["first_stage_markup"] >= price_rules["second_stage_markup"]).all())
    assert ((price_rules["second_stage_markup"] >= price_rules["third_stage_markup"]).all())
    assert ((price_rules["third_stage_markup"] >= price_rules["fourth_stage_markup"]).all())


def test_real_rules():
    '''
    Проверяем, что правила катменов соответствуют здравому смыслу
    '''
    base_path = yatest.common.source_path('market/dynamic_pricing/pricing/deadstock_sales/rules_uploader')

    status_rules_df, price_rules_df = prepare_rules(base_path + '/deadstock_rules.csv')
    validate_status_rules(status_rules_df)
    validate_price_rules(price_rules_df)
