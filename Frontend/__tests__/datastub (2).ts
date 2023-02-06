const snippetDefault = {
    title: 'Мультимедиа-платформа Яндекс.Станция, фиолетовая',
    price: 7000,
    oldPrice: 10000,
    picture: {
        src: '//avatars.mds.yandex.net/get-mpic/1382936/img_id3557252912121609779.png/5hq',
        srcSet: '//avatars.mds.yandex.net/get-mpic/1382936/img_id3557252912121609779.png/9hq 2.5x',
    },
    discountPercent: 30,
    opinions: 42,
    rating: 3.7,
    url: '/path/to',
};

export const defaultRecomendations = {
    title: 'С этим товаром покупают',
    products: [
        snippetDefault,
        snippetDefault,
        snippetDefault,
        snippetDefault,
        snippetDefault,
        snippetDefault,
        snippetDefault,
        snippetDefault,
        snippetDefault,
        snippetDefault,
        snippetDefault,
    ],
    snippetPerPage: 3,
    snippetMaxWidth: 200,
    snippetMaxTitleLines: 2,
    suite: 'small',
};
