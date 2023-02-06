export const thumbStub = {
    thumbnails: [
        {
            containerHeight: 50,
            containerWidth: 50,
            url: '//avatars.mds.yandex.net/get-goods_pic/6259751/pic7a4395b7f26386b417d595e586a689a5/50x50',
            width: 37,
            height: 50,
        },
    ],
    signatures: [],
    entity: 'picture',
    original: {
        containerHeight: 2289,
        containerWidth: 1717,
        url: '//avatars.mds.yandex.net/get-goods_pic/6259751/pic7a4395b7f26386b417d595e586a689a5/orig',
        width: 1717,
        height: 2289,
    },
};

export const offerDataStub = {
    title: 'Рубашка Stronger',
    type: 'offer',
    id: 'Ty_9WCLfSwHbDzHdgTCjjw',
    sku: '101699308935',
    urls: {
        direct: 'https://www.ozon.ru/context/detail/id/500236606/',
    },
    price: {
        discount_percent: 0,
        old: '',
        currency: 'RUR',
        type: 'exact',
        current: '1919',
    },
    pictures: [thumbStub],
    showUid: '',
    skuAwarePictures: [thumbStub],
    skuAwareTitles: 'Рубашка мужская классическая, в клетку, с длинным рукавом Stronger MG-32-25225',
    shop: {
        title: 'ozon.ru',
        id: '1864680',
    },
};

export const skuDataStub = {
    skuOffersCount: 1,
    title: 'Рубашка мужская классическая, в клетку, с длинным рукавом Stronger MG-32-25225',
    type: 'product',
    skuPrices: {
        max: '1919',
        currency: 'RUR',
        type: 'range',
        min: '1919',
    },
    id: '1742732431',
    offers: {
        data: [
            offerDataStub,
        ],
        count: 1,
    },
    urls: {
        direct: '//market.yandex.ru/product/1742732431?hid=7812145&nid=57402',
        decrypted: '/redir/stub/url',
        encrypted: '/redir/stub/url',
    },
    price: {
        max: '1919',
        currency: 'RUR',
        type: 'range',
        min: '1919',
    },
    pictures: [thumbStub],
    showUid: '16552129688736021883516003',
};
