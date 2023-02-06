from decimal import Decimal

import pytest

from mail.payments.payments.core.entities.enums import NDS
from mail.payments.payments.core.entities.product import Product


class TestProduct:
    @pytest.fixture
    def product(self, merchant):
        return Product(
            uid=merchant.uid,
            name='product 01',
            price=Decimal('12.34'),
            nds=NDS.NDS_0,
            currency='RUB',
        )

    def test_key(self, product):
        assert product.key == (product.name, product.nds, product.currency, product.price)

    def test_key_out(self, product):
        assert product.key_out == (
            product.name,
            product.nds.value,
            product.currency,
            float(round(product.price, 2)),  # type: ignore
        )

    @pytest.mark.parametrize('nds_before,nds_after', [
        (NDS.NDS_0, NDS.NDS_0),
        (NDS.NDS_NONE, NDS.NDS_NONE),
        (NDS.NDS_10, NDS.NDS_10),
        (NDS.NDS_10_110, NDS.NDS_10_110),
        (NDS.NDS_20, NDS.NDS_20),
        (NDS.NDS_20_120, NDS.NDS_20_120),

        (NDS.NDS_18, NDS.NDS_20),
        (NDS.NDS_18_118, NDS.NDS_20_120),
    ])
    def test_adjust_nds(self, product, nds_before, nds_after):
        product.nds = nds_before
        product.adjust_nds()
        assert product.nds == nds_after
