import getCompany from '../getCompany';

const companies = {
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

describe('getCompany', () => {
    it('Вернет компанию по id, если компания с таким id есть в списке', () => {
        expect(getCompany(963, companies)).toBe(companies[963]);
    });

    it('Вернет undefined, если компании с переданным id нет в списке', () => {
        expect(getCompany(6, companies)).toBe(undefined);
    });

    it('Вернет undefined, если в качестве id передали -1', () => {
        expect(getCompany(-1, companies)).toBe(undefined);
    });

    it('Вернет undefined, если передали пустой список компаний', () => {
        expect(getCompany(1, {})).toBe(undefined);
    });
});
