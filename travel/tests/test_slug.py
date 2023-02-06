# -*- coding: utf-8 -*-
import pytest

from travel.avia.library.python.slug.slug import build_slug


@pytest.mark.parametrize(
    'title,expected_slug',
    [
        (u'Москва', 'moskva'),
        (u'Екатеринбург', 'ekaterinburg'),
        (u'Санкт-Петербург', 'sankt-peterburg'),
        (u'Сочи', 'sochi'),
        (u'Фьюмичино', 'fiumichino'),
        (u'Мишкольц-Тапольца', 'mishkolts-tapoltsa'),
        (u'Йошкар-Ола', 'ioshkar-ola'),
        (u'Ростов-на-Дону', 'rostov-na-donu'),
        (u'Ярославль', 'iaroslavl'),
        (u'Орёл', 'oriol'),
    ]
)
def test_build_slug(title, expected_slug):
    assert build_slug(title) == expected_slug
