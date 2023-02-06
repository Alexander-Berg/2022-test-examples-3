/* eslint-disable max-len */

const shopMock = {
    'entity': 'shop',
    'id': 431782,
    'name': 'БЕРУ',
    'logo': {
        'entity': 'picture',
        'width': 37,
        'height': 14,
        'url': '//avatars.mds.yandex.net/get-market-shop-logo/1598257/2a00000167bcc983458fb76f2a31be52b15e/orig',
        'extension': 'SVG',
    },
};

const promoCodeDeal = {
    'entity': 'promo',
    'type': 'promo-code',
    'url': '',
    'offers_num': 40,
    'requiredQuantity': 1,
    'promo-code': {
        'text': '63D12633',
        'value': 6,
    },
    'min_purchases_price': '0.0249',
    'max_purchases_price': '0.4799',
    'pictures': [
        {
            'entity': 'picture',
            'original': {
                'containerWidth': 550,
                'containerHeight': 550,
                'url': '//avatars.mds.yandex.net/get-marketpictesting/1330625/market_KJFvi48BLj2OWPOY62vXJg/orig',
                'width': 550,
                'height': 550,
            },
            'thumbnails': [
                {
                    'containerWidth': 50,
                    'containerHeight': 50,
                    'url': '//avatars.mds.yandex.net/get-marketpictesting/1330625/market_KJFvi48BLj2OWPOY62vXJg/50x50',
                    'width': 50,
                    'height': 50,
                },
            ],
        },
    ],
    'shop': shopMock,
};

const discountDeal = {
    'entity': 'promo',
    'key': '431782discount',
    'type': 'discount',
    'url': '',
    'discount_prc': 76,
    'min_purchases_price': '0',
    'max_purchases_price': '0',
    'pictures': [
        {
            'entity': 'picture',
            'original': {
                'containerWidth': 701,
                'containerHeight': 465,
                'url': '//avatars.mds.yandex.net/get-mpic/1428687/img_id338081844565661316.jpeg/orig',
                'width': 701,
                'height': 465,
            },
            'thumbnails': [
                {
                    'containerWidth': 50,
                    'containerHeight': 50,
                    'url': '//avatars.mds.yandex.net/get-mpic/1428687/img_id338081844565661316.jpeg/50x50',
                    'width': 50,
                    'height': 33,
                },
            ],
        },
    ],
    'shop': shopMock,
};

const giftWithPurchaseDeal = {
    'entity': 'promo',
    'key': 'lKRxznBk4-c2uMt3TDxC9A',
    'type': 'gift-with-purchase',
    'url': 'https://www.onlinetrade.ru/actions/igray_v_gears_5_vmeste_s_amd-a4985.html?utm_source=market.yandex.ru&utm_medium=cpc&city=19',
    'offers_num': 103,
    'gifts_num': 1,
    'requiredQuantity': 1,
    'min_purchases_price': '0.06825',
    'max_purchases_price': '0.5802',
    'pictures': [
        {
            'entity': 'picture',
            'original': {
                'containerWidth': 800,
                'containerHeight': 444,
                'url': '//avatars.mds.yandex.net/get-marketpic/1715186/market_CjM8yimP_dSAHLtp526_fg/orig',
                'width': 800,
                'height': 444,
            },
            'thumbnails': [
                {
                    'containerWidth': 50,
                    'containerHeight': 50,
                    'url': '//avatars.mds.yandex.net/get-marketpic/1715186/market_CjM8yimP_dSAHLtp526_fg/50x50',
                    'width': 50,
                    'height': 27,
                },
            ],
        },
    ],
    'shop': shopMock,
};

const nPlusMDeal = {
    'entity': 'promo',
    'key': 'BAKe2s2OJJmyTaep8F8F6Q',
    'type': 'n-plus-m',
    'url': 'https://unizoo.ru/shares/517457/',
    'offers_num': 18,
    'startDate': '2019-07-02T08:00:00Z',
    'requiredQuantity': 2,
    'freeQuantity': 1,
    'min_purchases_price': '0.00083',
    'max_purchases_price': '0.0012',
    'pictures': [
        {
            'entity': 'picture',
            'original': {
                'containerWidth': 1000,
                'containerHeight': 745,
                'url': '//avatars.mds.yandex.net/get-marketpic/932147/market_l0KSRyDqKFF4ZGQR7apDjQ/orig',
                'width': 1000,
                'height': 745,
            },
            'thumbnails': [
                {
                    'containerWidth': 50,
                    'containerHeight': 50,
                    'url': '//avatars.mds.yandex.net/get-marketpic/932147/market_l0KSRyDqKFF4ZGQR7apDjQ/50x50',
                    'width': 50,
                    'height': 37,
                },
            ],
        },
    ],
    'shop': shopMock,
};


export {
    promoCodeDeal,
    discountDeal,
    giftWithPurchaseDeal,
    nPlusMDeal,
};
