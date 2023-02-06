from calculate_optimal_promo_assortment import get_new_price
import yatest.common


class __YaTestEnv:  # for "ya make -t ..."
    def __init__(self):
        self.python_bin = yatest.common.python_path()
        self.work_dir = yatest.common.work_path()
        self.binary_path = yatest.common.binary_path
        self.source_path = yatest.common.source_path

_env = __YaTestEnv()


def test_max_discount_etr():
    new_price = get_new_price(
        current_price=1000,
        target_price=500,
        discount_max_absolute=600,
        discount_min_absolute=0,
        discount_max_percent=99,
        discount_min_percent=0,
        max_subsidy_by_etr_absolute=200,
        threshold_discount=0
    )

    assert new_price == -1


def test_max_discount_abs():
    new_price = get_new_price(
        current_price=1000,
        target_price=500,
        discount_max_absolute=300,
        discount_min_absolute=0,
        discount_max_percent=99,
        discount_min_percent=0,
        max_subsidy_by_etr_absolute=600,
        threshold_discount=0
    )

    assert new_price == -1


def test_max_discount_percent():
    new_price = get_new_price(
        current_price=1000,
        target_price=500,
        discount_max_absolute=600,
        discount_min_absolute=0,
        discount_max_percent=10,
        discount_min_percent=0,
        max_subsidy_by_etr_absolute=700,
        threshold_discount=0
    )

    assert new_price == -1


def test_min_discount_abs():
    new_price = get_new_price(
        current_price=1000,
        target_price=999,
        discount_max_absolute=300,
        discount_min_absolute=10,
        discount_max_percent=99,
        discount_min_percent=5,
        max_subsidy_by_etr_absolute=600,
        threshold_discount=0
    )

    assert new_price == 990


def test_min_discount_percent():
    new_price = get_new_price(
        current_price=1000,
        target_price=999,
        discount_max_absolute=600,
        discount_min_absolute=100,
        discount_max_percent=10,
        discount_min_percent=5,
        max_subsidy_by_etr_absolute=600,
        threshold_discount=0
    )

    assert new_price == 950


def test_discount_ok():
    new_price = get_new_price(
        current_price=1000,
        target_price=700,
        discount_max_absolute=600,
        discount_min_absolute=100,
        discount_max_percent=40,
        discount_min_percent=5,
        max_subsidy_by_etr_absolute=600,
        threshold_discount=5000
    )

    assert new_price == 700


def test_min_discount_threshold():
    new_price = get_new_price(
        current_price=1000,
        target_price=999,
        discount_max_absolute=300,
        discount_min_absolute=10,
        discount_max_percent=99,
        discount_min_percent=5,
        max_subsidy_by_etr_absolute=600,
        threshold_discount=2
    )

    assert new_price == -1
