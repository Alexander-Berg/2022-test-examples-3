import csv
from datetime import datetime
from decimal import Decimal

import pytest
from click.testing import CliRunner

from mail.payments.payments.commands.merchant_orders_stats import cli
from mail.payments.payments.core.entities.enums import NDS, OrderKind, PayStatus, ShopType
from mail.payments.payments.core.entities.item import Item
from mail.payments.payments.core.entities.order import Order
from mail.payments.payments.core.entities.product import Product
from mail.payments.payments.http_helpers.crypto import Crypto


@pytest.fixture(autouse=True)
async def generate_stats(storage, merchant, default_merchant_shops):
    order = await storage.order.create(
        Order(
            uid=merchant.uid,
            shop_id=default_merchant_shops[ShopType.PROD].shop_id,
            closed=datetime.fromisoformat('2021-03-02T00:00:00+00:00'),
            pay_status=PayStatus.PAID,
            kind=OrderKind.PAY,
        ),
    )
    product = await storage.product.create(Product(uid=merchant.uid, name='name', price=Decimal(100), nds=NDS.NDS_0))
    await storage.item.create(
        Item(uid=merchant.uid, product_id=product.product_id, order_id=order.order_id, amount=Decimal(1))
    )


@pytest.fixture(autouse=True)
def mock_crypto(mocker):
    """
    Без этой фикстуры @action_command будет падать из-за невозможности инициализировать BaseAction.context
    Самое просто решение - просто замокать Crypto, чтобы не пришлось подсовывать ему какой-то удобоваримый файл
    """
    mocker.patch.object(Crypto, 'from_file')


@pytest.fixture
def run_cli():
    def _run_cli(*args):
        runner = CliRunner(mix_stderr=False)

        result = runner.invoke(cli, args)
        assert not result.exit_code, result.stderr

        csv_file = result.stdout

        try:
            reader = csv.DictReader(
                csv_file.encode('utf-8').decode('utf-8-sig').split('\n'),
                delimiter=';',
                quotechar='"'
            )
        except Exception:
            raise RuntimeError(f'Bad cmd result^\n{result.output}')
        return list(reader)

    return _run_cli


def test_order_stats(run_cli):
    result_dict = run_cli('--date_from', '2021-03-01', '--date_to', '2021-03-31')

    assert {'parent_uid': '-', 'title': 'Test merchant', 'sum': '100,00', 'commission': '2,15'} in result_dict


def test_order_stats_without_comma_decimal_delimiter(run_cli):
    result_dict = run_cli('--date_from', '2021-03-01', '--date_to', '2021-03-31', '--decimal_delimiter_comma', 'false')

    assert {'parent_uid': '-', 'title': 'Test merchant', 'sum': '100.00', 'commission': '2.15'} in result_dict
