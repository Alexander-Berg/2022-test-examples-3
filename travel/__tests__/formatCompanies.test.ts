import formatCompanies from '../formatCompanies';

const companiesById = {
    674: {
        shortTitle: '',
        title: 'Air France',
        url: '',
        hidden: false,
        id: 674,
        icon: 'data/company/icon/airfrance-fav.png',
    },
    963: {
        shortTitle: '',
        title: 'Icelandair',
        url: 'http://www.icelandair.com/',
        hidden: false,
        id: 963,
        icon: '',
    },
    196: {
        shortTitle: '',
        title: 'Pegas Fly',
        url: 'http://pegasfly.com/',
        hidden: false,
        id: 196,
        icon: '',
    },
};

const codeshares = [
    {
        number: 'KL 3240',
        companyId: 1418,
    },
    {
        number: 'AF 4856',
        companyId: 963,
    },
];

const expectedCompanies = [
    {
        companyId: 196,
        number: 'UF 1400',
        company: {
            shortTitle: '',
            title: 'Pegas Fly',
            url: 'http://pegasfly.com/',
            hidden: false,
            id: 196,
            icon: '',
        },
        numberLetters: 'UF',
        numberNumeral: '1400',
    },
    {
        companyId: 1418,
        number: 'KL 3240',
        company: undefined,
        numberLetters: 'KL',
        numberNumeral: '3240',
    },
    {
        companyId: 963,
        number: 'AF 4856',
        company: {
            shortTitle: '',
            title: 'Icelandair',
            url: 'http://www.icelandair.com/',
            hidden: false,
            id: 963,
            icon: '',
        },
        numberLetters: 'AF',
        numberNumeral: '4856',
    },
];

describe('formatCompanies', () => {
    it('Вернет отформатированный массив codeshares', () => {
        expect(
            formatCompanies(196, 'UF 1400', companiesById, codeshares),
        ).toStrictEqual(expectedCompanies);
    });
});
