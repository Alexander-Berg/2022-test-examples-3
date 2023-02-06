from typing import Any

from order_comparator import OrderComparator
from data import OrderKey
from results import Results
from travel.cpa.data_processing.lib.order_data_model import CATEGORY_CONFIGS


Order = dict[str, Any]


class TableMappingChecker:

    def __init__(self, results: Results):
        self.results = results

    @staticmethod
    def get_tables_to_read() -> list[str]:
        tables_to_read = list()
        for category in CATEGORY_CONFIGS:
            tables_to_read.append(f'{category}/orders_internal')
            tables_to_read.append(f'private/{category}/orders')
            tables_to_read.append(f'public/{category}/orders')
        return tables_to_read

    def check(self):
        for category, category_config in CATEGORY_CONFIGS.items():
            orders_internal_table = f'{category}/orders_internal'
            orders_private_table = f'private/{category}/orders'
            orders_public_table = f'public/{category}/orders'
            orders_internal = self.get_orders(orders_internal_table)
            orders_private = self.get_orders(orders_private_table)
            orders = self.get_orders(orders_public_table)
            order_cls = category_config.order_with_decoded_label_cls or category_config.order_with_encoded_label_cls
            self.compare_tables(orders_internal, orders_private, orders_private_table, set())
            self.compare_tables(orders_internal, orders, orders_public_table, order_cls().get_hidden_field_names())

    def get_orders(self, path: str) -> dict[OrderKey, Order]:
        return {ok: ol[0] for ok, ol in self.results.tables_data[path].items()}

    @staticmethod
    def compare_tables(
        expected_data: dict[OrderKey, Order],
        actual_data: dict[OrderKey, Order],
        table_name: str,
        fields_to_skip: set[str],
    ) -> None:
        expected_keys = set(expected_data.keys())
        actual_keys = set(actual_data.keys())
        extra_keys = actual_keys - expected_keys
        if extra_keys:
            raise Exception(f'more actual than expected: {sorted(extra_keys)}')
        lost_keys = expected_keys - actual_keys
        if lost_keys:
            raise Exception(f'more expected than actual: {sorted(lost_keys)}')
        for order_key in expected_keys:
            OrderComparator.compare_pair(
                table_name,
                order_key,
                {k: v for k, v in expected_data[order_key].items() if k not in fields_to_skip},
                actual_data[order_key],
                OrderComparator.__order_fields_to_ignore__,
            )
