import pytest
from url_utils import clean_cgi, fix_yandex_url, get_cgi_value, ComponentTypes, WizardTypes


@pytest.mark.parametrize('input, cgis, expected', [
    ('https://ya.ru/?k=v', ['k'], 'https://ya.ru/'),
    ('https://ya.ru/?k=v&k1=v2', ['k'], 'https://ya.ru/?k1=v2'),
    ('https://ya.ru/?k=v&k=v2', ['k'], 'https://ya.ru/'),
    ('https://ya.ru/?k=v&k2=v', ['k', 'k2'], 'https://ya.ru/'),
    ('https://ya.ru/?k=%20', [], 'https://ya.ru/?k=%20'),
    ('https://ya.ru/?k=', ['k2'], 'https://ya.ru/?k')  # java compatablity
])
def test_clean_cgi(input, cgis, expected):
    assert clean_cgi(input, cgis) == expected


@pytest.mark.parametrize('input, expected', [
    ('https://yandex.ru/health/turbo/articles?id=266&text=t&ids=266&utm_source=yandex&utm_medium=search&utm_campaign=y&utm_content=a',
     'https://yandex.ru/health/turbo/articles?id=266&text=t&ids=266'),
    ('https://translate.yandex.ru/?utm_source=wizard', 'https://translate.yandex.ru/'),
    ('https://yandex.ru/video/запрос/сериал/мост/?text=крымский%20мост%20сегодня&noreask=1&path=wizard&redircnt=1584955254.1',
     'https://yandex.ru/video/запрос/сериал/мост/?text=крымский%20мост%20сегодня&redircnt=1584955254.1')
])
def test_fix_common_wizard(input, expected):
    assert fix_yandex_url(input, ComponentTypes.SEARCH_RESULT, wizard_type=None, region_id=None) == expected


@pytest.mark.parametrize('input, expected', [
    ('https://market.yandex.ru/search?lr=213&text=купить%20айфон&rs=eJwz0vTiFuI0NbUwNDc0MDOHccwsDCws4RxjQyPLCAYAoz8Hl'
     'g%2C%2C&wprid=1611502689210816-1119888408996076772500107-production-app-host-vla-web-yp-270&utm_medium=cpc'
     '&utm_referrer=wizards&clid=836',
     'https://market.yandex.ru/search?text=купить%20айфон&lr=213&clid=545'),
    ('https://m.market.yandex.ru/search?rs=eJwzSvKS4xIrLvWwqDLIDk11NIgqTTQrNnE3KIt0lOBRYNBgAMkHpvr7Z5v5u8eXVYaUeel'
     'GVQbl-2akS3DD5F0MHNNDI7yT8gM8HHMNM_MyXUOy4sslmEHyEQwATIoaOg%2C%2C&text=шторки%20для%20ванной%20купить'
     '&wprid=1611503779707721-1232513443271545765400107-production-app-host-vla-web-yp-250&utm_medium=cpc'
     '&utm_referrer=wizards&clid=708',
     'https://m.market.yandex.ru/search?text=шторки%20для%20ванной%20купить&clid=545'),
    ('https://market.yandex.ru/search?rs=eJwzSvKS4xIr8nAzdg3Kdit39Ql183X2ykn3DTAKlJBVYNBgAMkb-ocF5Vf4epUGpucblJtHGucYB2'
     'cESujB5OPj_fyLQ3NzC00dfVPik8IdzXy9SqD6IxgALlMZzw%2C%2C&text=тумбочки%20от%20135'
     '&wprid=1611504042635182-1638776693792978149600125-production-app-host-vla-web-yp-87'
     '&utm_medium=cpc&utm_referrer=wizards&clid=830',
     'https://market.yandex.ru/search?text=тумбочки%20от%20135&lr=2&clid=545')
])
def test_fix_market_wizard(input, expected):
    assert fix_yandex_url(
        input,
        ComponentTypes.WIZARD,
        wizard_type=WizardTypes.WIZARD_MARKET,
        region_id=2
    ) == expected


@pytest.mark.parametrize('url, key, expected', [
    ('https://ya.ru/?k=v', 'k', 'v'),
    ('some string', 'k', None)
])
def test_get_cgi_value(url, key, expected):
    assert get_cgi_value(url, key) == expected
