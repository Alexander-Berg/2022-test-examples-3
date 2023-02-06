# -*- coding: utf-8 -*-

from travel.avia.library.python.common.models.schedule import Company, TranslatedTitle, make_slug


def test_make_slug():
    test_items = [
        {
            'iata': 'SU',
            'en_nominative': u'Aeroflot',
            'ru_nominative': u'Аэрофллот',
            'expected_slug': 'su_aeroflot'
        },
        {
            'iata': 'SU',
            'ru_nominative': u'Аэрофллот',
            'expected_slug': 'su_ayerofllot'
        },
        {
            'en_nominative': u'Aeroflot',
            'expected_slug': None,
        },
        {
            'iata': 'SU',
            'expected_slug': None,
        },
    ]

    for test_item in test_items:
        company = Company(
            iata=test_item.get('iata', ''),
            new_L_title=TranslatedTitle(
                en_nominative=test_item.get('en_nominative', ''),
                ru_nominative=test_item.get('ru_nominative', ''),
            )
        )

        assert test_item['expected_slug'] == make_slug(company)
