import pytest

from asyncio import run


@pytest.mark.usefixtures('fill_db')
def test_user_subscription_list(user_subscription_list_actor):
    result = run(user_subscription_list_actor.list(
        source='WIZARD',
        language='en',
        credentials=(('cookie', 'cookie_val_1'),),
    ))

    assert_list_equal(result, {'mikhail@lomonosov.ru': [
        _promo_subscription('other_promo'),
        _promo_subscription('yet_other_promo'),
        _price_subscription(
            qkey='c213_c2_2017-12-21_2018-12-05_business_1_0_1_com',
            name='213-2, 21.12',
            url=('https://flights.yandex.com/search?fromBlock=emailAlert&fromId=c213'
                 '&toId=c2&when=2017-12-21&return_date=2018-12-05&adult_seats=1'
                 '&children_seats=0&infant_seats=1&klass=business&lang=en')
        ),
        _price_subscription(
            qkey='c213_c22_2018-11-21_2018-12-05_economy_3_3_1_com',
            name='213-22, 21.11',
            url=('https://flights.yandex.com/search?fromBlock=emailAlert&fromId=c213'
                 '&toId=c22&when=2018-11-21&return_date=2018-12-05&adult_seats=3'
                 '&children_seats=3&infant_seats=1&klass=economy&lang=en')
        ),
    ]})


@pytest.mark.usefixtures('fill_db')
def test_user_subscription_list_deduplication(user_subscription_list_actor):
    result = run(user_subscription_list_actor.list(
        source='WIZARD',
        language='ru',
        credentials=(('session', '123456789abc'),),
    ))

    assert_list_equal(result, {'mikhail@lomonosov.ru': [
        _promo_subscription('travel_news'),
        _price_subscription(
            qkey='c213_c2_2017-12-21_2018-01-05_economy_1_0_1_ru',
            name='213-2, 21.12',
            url=('https://avia.yandex.ru/search?fromBlock=emailAlert&fromId=c213'
                 '&toId=c2&when=2017-12-21&return_date=2018-01-05&adult_seats=1'
                 '&children_seats=0&infant_seats=1&klass=economy&lang=ru')
        ),
    ]})


@pytest.mark.usefixtures('fill_db')
def test_user_subscription_list_many_credentials(user_subscription_list_actor):
    result = run(user_subscription_list_actor.list(
        source='WIZARD',
        language='ru',
        credentials=(
            ('session', '123456789abc'),
            ('cookie', 'cookie_val_1'),
        ),
    ))

    assert_list_equal(result, {'mikhail@lomonosov.ru': [
        _promo_subscription('travel_news'),
        _promo_subscription('other_promo'),
        _price_subscription(
            qkey='c213_c2_2017-12-21_2018-01-05_economy_1_0_1_ru',
            name='213-2, 21.12',
            url=('https://avia.yandex.ru/search?fromBlock=emailAlert&fromId=c213'
                 '&toId=c2&when=2017-12-21&return_date=2018-01-05&adult_seats=1'
                 '&children_seats=0&infant_seats=1&klass=economy&lang=ru')
        ),
        _price_subscription(
            qkey='c213_c2_2017-12-21_2018-01-05_economy_1_0_1_com',
            name='213-2, 21.12',
            url=('https://flights.yandex.com/search?fromBlock=emailAlert&fromId=c213'
                 '&toId=c2&when=2017-12-21&return_date=2018-01-05&adult_seats=1'
                 '&children_seats=0&infant_seats=1&klass=economy&lang=ru')
        ),
        _price_subscription(
            qkey='c213_c2_2017-12-21_2018-12-05_business_1_0_1_com',
            name='213-2, 21.12',
            url=('https://flights.yandex.com/search?fromBlock=emailAlert&fromId=c213'
                 '&toId=c2&when=2017-12-21&return_date=2018-12-05&adult_seats=1'
                 '&children_seats=0&infant_seats=1&klass=business&lang=ru')
        ),
    ]})


@pytest.mark.usefixtures('fill_db')
def test_user_subscription_list_two_user_with_same_credential(
    user_subscription_actor, user_subscription_list_actor, approve_all_users
):
    run(user_subscription_actor.put(
        email='another@lomonosov.ru',
        credentials=(('cookie', 'cookie_val_1'),),
        subscriptions=[
            {
                'subscription_type': 'promo',
                'subscription_code': 'other_promo',
            },
            {
                'subscription_type': 'price',
                'subscription_code': 'c213_c2_2017-12-21_2018-01-05_economy_1_0_1_ru',
            },
        ],
        source='WIZARD',
        travel_vertical_name='train',
        national_version='com',
        language='ru',
        timezone='Asia/Yekaterinburg',
        name='Anatoly',
    ))
    approve_all_users()

    result = run(user_subscription_list_actor.list(
        source='WIZARD',
        language='ru',
        credentials=(('cookie', 'cookie_val_1'),),
    ))

    assert_list_equal(result, {
        'mikhail@lomonosov.ru': [
            _promo_subscription('travel_news'),
            _promo_subscription('other_promo'),
            _price_subscription(
                qkey='c213_c2_2017-12-21_2018-12-05_business_1_0_1_com',
                name='213-2, 21.12',
                url=('https://flights.yandex.com/search?fromBlock=emailAlert&fromId=c213'
                     '&toId=c2&when=2017-12-21&return_date=2018-12-05&adult_seats=1'
                     '&children_seats=0&infant_seats=1&klass=business&lang=ru')
            ),
            _price_subscription(
                qkey='c213_c2_2017-12-21_2018-01-05_economy_1_0_1_com',
                name='213-2, 21.12',
                url=('https://flights.yandex.com/search?fromBlock=emailAlert&fromId=c213'
                     '&toId=c2&when=2017-12-21&return_date=2018-01-05&adult_seats=1'
                     '&children_seats=0&infant_seats=1&klass=economy&lang=ru')
            )
        ],
        'another@lomonosov.ru': [
            _promo_subscription('other_promo'),
            _price_subscription(
                qkey='c213_c2_2017-12-21_2018-01-05_economy_1_0_1_ru',
                name='213-2, 21.12',
                url=('https://avia.yandex.ru/search?fromBlock=emailAlert&fromId=c213'
                     '&toId=c2&when=2017-12-21&return_date=2018-01-05&adult_seats=1'
                     '&children_seats=0&infant_seats=1&klass=economy&lang=ru')
            ),
        ]
    })


def assert_list_equal(actual, expected):
    def sort_result(result):
        for key in result:
            result[key] = sorted(result[key], key=lambda x: x['subscription_code'])

    sort_result(actual)
    sort_result(expected)
    assert actual == expected


def _promo_subscription(code):
    return {
        'subscription_type': 'promo',
        'subscription_code': code,
        'name': '',
        'url': '',
    }


def _price_subscription(qkey, name, url):
    return {
        'subscription_type': 'price',
        'subscription_code': qkey,
        'name': name,
        'url': url,
    }
