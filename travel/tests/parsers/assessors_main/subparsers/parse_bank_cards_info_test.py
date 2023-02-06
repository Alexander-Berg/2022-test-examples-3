from travel.avia.country_restrictions.lib.types.metric_type import BANK_CARD_PAYMENTS, BankCardPaymentsMetricType
from travel.avia.country_restrictions.lib.types.rich_string import new_rich_text, new_rich_url
from travel.avia.country_restrictions.parsers.assessors_main.subparsers.parse_bank_cards_info import parser


def test_only_union_pay():
    row = {
        'card_payment': {
            'no_Visa_MasterCard': True,
            'no_MIR': True,
        }
    }

    actual = parser(context={}, row=row).get(BANK_CARD_PAYMENTS.name, None)
    expected = BANK_CARD_PAYMENTS.generate_metric(
        value=BankCardPaymentsMetricType.InternalData(
            russian_visa_mastercard=False,
            mir=False,
            union_pay=True,
        ),
        additions=[
            new_rich_url(
                'Подробнее об использовании российских карт за границей',
                'https://travel.yandex.ru/journal/flight-restrictions#bankovskie_karty',
            ),
        ],
    )
    assert actual == expected
    assert expected.text == new_rich_text('Принимается UnionPay')


def test_only_russian_visa_mastercard():
    row = {
        'card_payment': {
            'no_MIR': True,
            'no_UnionPay': True,
        }
    }

    actual = parser(context={}, row=row).get(BANK_CARD_PAYMENTS.name, None)
    expected = BANK_CARD_PAYMENTS.generate_metric(
        value=BankCardPaymentsMetricType.InternalData(
            russian_visa_mastercard=True,
            mir=False,
            union_pay=False,
        ),
        additions=[
            new_rich_url(
                'Подробнее об использовании российских карт за границей',
                'https://travel.yandex.ru/journal/flight-restrictions#bankovskie_karty',
            ),
        ],
    )
    assert actual == expected
    assert expected.text == new_rich_text('Принимаются выпущенные в России Visa и MasterCard')


def test_mir_union_pay():
    row = {
        'card_payment': {
            'no_Visa_MasterCard': True,
        }
    }

    actual = parser(context={}, row=row).get(BANK_CARD_PAYMENTS.name, None)
    expected = BANK_CARD_PAYMENTS.generate_metric(
        value=BankCardPaymentsMetricType.InternalData(
            russian_visa_mastercard=False,
            mir=True,
            union_pay=True,
        ),
        additions=[
            new_rich_url(
                'Подробнее об использовании российских карт за границей',
                'https://travel.yandex.ru/journal/flight-restrictions#bankovskie_karty',
            ),
        ],
    )
    assert actual == expected
    assert expected.text == new_rich_text('Принимаются Мир, UnionPay')


def no_cards_test():
    row = {
        'card_payment': {
            'no_MIR': True,
            'no_UnionPay': True,
            'no_Visa_MasterCard': True,
        }
    }

    actual = parser(context={}, row=row).get(BANK_CARD_PAYMENTS.name, None)
    assert actual is None
