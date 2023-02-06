const item1 = {
    title: 'Наушники Apple AirPods',
    url: '/product--naushniki-apple-airpods/14206836',
    price: {
        currency: 'RUR',
        value: '11580',
    },
    picture: {
        src: 'https://avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/4hq',
        srcSet: 'https://avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/4hq 1x,' +
            ' https://avatars.mds.yandex.net/get-mpic/175985/img_id6526000481435545741/9hq 2x',
    },
};

const item2 = {
    title: 'Карта памяти Samsung MB-MC128GA',
    url: '/product--karta-pamiati-samsung-mb-mc128ga/1725463737',
    price: {
        currency: 'RUR',
        value: '1450',
    },
    picture: {
        src: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6161510249784651938/4hq',
        srcSet: 'https://avatars.mds.yandex.net/get-mpic/195452/img_id6161510249784651938/4hq 1x,' +
            ' https://avatars.mds.yandex.net/get-mpic/195452/img_id6161510249784651938/9hq 2x',
    },
};

export const defaultData = {
    snippetMaxTitleLines: 2,
    isRetinaScale: true,
    products: [
        item1,
        item2,
        item1,
        item2,
        item1,
        item2,
        item1,
        item2,
    ],
};
