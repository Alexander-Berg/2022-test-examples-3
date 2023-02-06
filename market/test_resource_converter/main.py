import os
import sys
from itertools import groupby


# выбирает первый не null элемент
def first_not_null(a: str, b: str):
    return a if a.upper() != 'NULL' else b


CONVERT_MAP = {
    'MARKET_BILLING.CPA_ORDER': {
        'RENAME': {
            'IS_FULFILMENT': 'IS_FULFILLMENT'
        },
        'DELETE': [
            'ALL_PAYMENTS_CREATED', 'AUTO_PROCESSED', 'BILLING_STATUS', 'CAMPAIGN_ID', 'CHARGEABLE_WEIGHT', 'CPA20',
            'CROSSBORDER', 'DECLARED_VALUE_AMOUNT', 'DECLARED_VALUE_PERCENT', 'DELIVERY', 'DELIVERY_BALANCE_ORDER_ID',
            'DELIVERY_BUYER_PRICE', 'DELIVERY_SERVICE_ID', 'DELIVERY_SUBSIDY', 'DELIVERY_TRANTIME', 'DEPTH',
            'FEE_CORRECT', 'FEE_SUM', 'FREE', 'HEIGHT', 'INLET_ID', 'IS_FF_LIGHT', 'IS_FULFILMENT', 'ITEMS_TOTAL_UE',
            'LIFT_PRICE', 'LIFT_TYPE', 'ORDER_NUM', 'ORIGINAL_ORDER_ID', 'OUTLET_ID', 'PAYMENT_TYPE',
            'REAL_ITEMS_TOTAL', 'REGION_FROM', 'SHIPMENT_ID', 'SHOP_CURRENCY', 'SHOP_DELIVERY_BILLING_TYPE',
            'SHOP_DELIVERY_COST', 'SHOP_DELIVERY_FINAL_COST', 'SHOP_DELIVERY_INSURANCE_COST',
            'SHOP_DELIVERY_RETURN_COST', 'SUBSIDY_TOTAL', 'TRANTIME', 'WEIGHT', 'WIDTH'
        ],
        'APPEND': [
            # ('NEW_FIELD', '1'),
        ]
    },
    'MARKET_BILLING.CPA_ORDER_ITEM': {
        'DELETE': [
            'BUYER_PRICE', 'CIS', 'EXTENDED_STATUS', 'EXTENDED_SUPPLIER_ORDER_STATUS', 'FEED_CAT_ID', 'FEED_PRICE',
            'FEE_NET_UE', 'FEE_NORM', 'FF_SHOP_ID', 'FF_SUPPLIER_ID', 'MARKUP_RULES_UPDATE_TIME', 'MODEL_ID', 'MSKU',
            'ORDER_CREATION_DATE', 'PARTNER_PRICE', 'PARTNER_PRICE_CURRENCY', 'SHOW_UID', 'UNREDEEMED_COUNT',
            'VENDOR', 'WARE_MD5'
        ],
        'GENERATE': {
            ('PARTNER_ID', ('FF_SHOP_ID', 'FF_SUPPLIER_ID'), first_not_null)
        },
        'RENAME': {
            'FF_SUPPLIER_ID': 'PARTNER_ID'
        }
    },
    'MARKET_BILLING.CPA_ORDER_TRANSACTION': {
        'DELETE': [
            'BANK_ORDER_ID', 'BANK_ORDER_TIME', 'CAMPAIGN_ID', 'CLIENT_TYPE', 'DESCRIPTION', 'ORDER_ID_TO_DROP',
            'PAYMENT_ID', 'REFUND_ID', 'SERVICE_ORDER_ID', 'SHOP_CURRENCY', 'SHOP_ID', 'TRAN_SUM',
            'TRUST_PAYMENT_ID', 'TRUST_REFUND_ID', 'USING_CASH_REFUND_SERVICE'
        ],
        'GENERATE': {
            ('TRANSACTION_TYPE', ('PAYMENT_ID', 'REFUND_ID'), lambda p, r: 'payment' if p != 'null' else 'refund'),
            ('TRANSACTION_ID', ('PAYMENT_ID', 'REFUND_ID'), first_not_null),
            ('TRUST_TRANSACTION_ID', ('TRUST_PAYMENT_ID', 'TRUST_REFUND_ID'), first_not_null)
        }
    },
    'MARKET_BILLING.RECEIPT_ITEM': {
        'DELETE': [
            'PAYMENT_ID', 'REFUND_ID'
        ],
        'GENERATE': {
            ('TRANSACTION_TYPE', ('PAYMENT_ID', 'REFUND_ID'), lambda p, r: 'payment' if p != 'null' else 'refund'),
            ('TRANSACTION_ID', ('PAYMENT_ID', 'REFUND_ID'), first_not_null)
        }
    }
}


# выбирает из dataset колонки, имена которых переданы в col_names
def get_input_columns(dataset: list, col_names: list) -> list:
    out = []
    for col in dataset:
        if col[0] in col_names:
            out.append(col[1:])
    return out


# преобразуем данные
def convert_table_data(table_data: list) -> list:
    convert_rules = CONVERT_MAP[table_data[0]]

    # разбиваем строки на отдельные поля
    splitted_arr = [line.split(',') for line in table_data[1:]]

    # преобразуем в список колонок
    columns = list(zip(*splitted_arr))

    # генерируем дополнительные колонки
    col_to_generate = convert_rules.get('GENERATE', [])
    for col in col_to_generate:
        input_data = list(zip(*get_input_columns(columns, col[1])))
        columns.append(
            [col[0], *[col[2](*inp) for inp in input_data]]
        )

    # удаляем колонки
    col_to_delete = convert_rules.get('DELETE', [])
    columns = [c for c in columns if c[0] not in col_to_delete]

    # переименовываем колонки
    col_to_rename = convert_rules.get('RENAME', dict())
    for column in columns:
        if column[0] in col_to_rename.keys():
            column[0] = col_to_rename[column[0]]

    # добавляем колонки
    col_to_append = convert_rules.get('APPEND', [])
    for append_column in col_to_append:
        columns.append(
            [append_column[0], *[append_column[1] for _ in range(len(columns[0]))]]
        )

    # преобразуем обратно в список строк
    return [','.join(line) for line in zip(*columns)]


def process_test_resource(single_resource: str):
    if not os.path.exists(single_resource):
        print('%s - file not found' % single_resource)
        return

    with open(single_resource, 'rt') as f:
        # читаем и обрезаем строки
        lines = [line.strip() for line in f.readlines()]

        # удаляем дубли пустых строк
        lines = [line for (row, line) in enumerate(lines) if line or row > 0 and lines[row - 1]]

        # получаем позиции (номера строк) комментариев
        comment_rows = [(row, line) for (row, line) in enumerate(lines) if line.startswith('#')]

        # отфильтровываем комментарии
        lines = filter(lambda line: not line.startswith('#'), lines)

        # разбиваем тестовые данные потаблично (разделитель - пустая строка)
        all_table_data = [list(g) for k, g in groupby(lines, lambda x: x == '') if not k]

        # итоговый результат
        result = []

        # перебираем тестовые данные для всех таблиц
        for table_data in all_table_data:
            if table_data[0] in CONVERT_MAP.keys():
                # преобразуем данные
                local_result = convert_table_data(table_data)

                # формируем результат
                result += [table_data[0]]
                result += local_result
                result.append('')
            else:
                # оставляем данные как есть
                result += table_data
                result.append('')

        # восстанавливаем комментарии
        for p in comment_rows:
            result.insert(p[0], p[1])

        # выводим результат
        print('\n'.join(result))


def process_path(resources_dir: str):
    for single_resource in os.listdir(resources_dir):
        if single_resource.endswith(".csv"):
            process_test_resource(os.path.join(resources_dir, single_resource))


if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Specify test resource path as argument")
        sys.exit(0)

    process_test_resource(sys.argv[1])
