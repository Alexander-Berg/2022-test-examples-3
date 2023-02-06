# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date

from django.test.utils import override_settings

from travel.rasp.train_api.train_purchase.core.utils import hash_birth_date, hash_doc_id, hash_string


@override_settings(SECRET_KEY='test_secret_key')
def test_hashing():
    assert hash_string('foo', 'bar') == 'a128719a629d9cad3b45e3abba65d193512512138b6a6eb7c095b9f3b19a9606'
    assert hash_birth_date(date(1995, 1, 1)) == '28105f9a14957f64a6bc472d43d946e04a6f29df94b00521c39c61a68e2b985d'
    assert hash_doc_id('III-АИ 123456') == '185951d41018a90f5be5aa4eeabfcd84d9ceb22b4fcd418c15b9d3aebd7e9888'
