import getJsonLdCrumbs from '../getJsonLdCrumbs';

const expectedObject = {
    '@context': 'http://schema.org',
    '@type': 'BreadcrumbList',
    itemListElement: [
        {
            '@type': 'ListItem',
            position: 1,
            item: {
                '@id': 'https://rasp.yandex.ru/train',
                name: 'Расписание поездов',
            },
        },
        {
            '@type': 'ListItem',
            position: 2,
            item: {
                '@id': 'https://rasp.yandex.ru/train/moscow',
                name: 'Москва',
            },
        },
    ],
};

const breadcrumbs = [
    {
        name: 'Расписание поездов',
        url: 'https://rasp.yandex.ru/train',
    },
    {
        name: 'Москва',
        url: 'https://rasp.yandex.ru/train/moscow',
    },
];

describe('getJsoLdCrumbs', () => {
    it('Return correct object', () => {
        expect(getJsonLdCrumbs(breadcrumbs)).toStrictEqual(expectedObject);
    });
});
