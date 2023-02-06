import { BASE_URL } from '@constants';
import { appUrl } from '@utils/url/urlBuilder';

describe('urlBuilder', () => {
    it('применяет дефолтны путь', () => {
        const url = appUrl();
        expect(url.url()).toEqual(BASE_URL);
    });

    it('применяет базовый путь', () => {
        const url = appUrl('ya.ru/agency-cab');
        expect(url.url()).toEqual('ya.ru/agency-cab');
    });

    it('работа с накоплением состояния', () => {
        const url = appUrl().agency(123).contract(null);
        const url1 = url.changeContract(123);
        const url2 = url.changeAgency(312).rewards();
        const url3 = url.rewards().params().add('s', '123');
        expect(url1.url()).toEqual(`${BASE_URL}/agency/123/contract/123`);
        expect(url2.url()).toEqual(`${BASE_URL}/agency/312/rewards`);
        expect(url3.url()).toEqual(`${BASE_URL}/agency/123/rewards`);
        expect(url3.path()).toEqual(`${BASE_URL}/agency/123/rewards?s=123`);
    });

    describe('применяет путь agency', () => {
        const url = appUrl();

        const agencyUrl = url.agency(123).contract(null);
        it('сразу', () => {
            expect(
                agencyUrl.url(),
            ).toEqual(`${BASE_URL}/agency/123`);
        });

        it('отложенно', () => {
            expect(
                agencyUrl.changeAgency(321).url(),
            ).toEqual(`${BASE_URL}/agency/321`);
        });

        it('отложенно, через вызов', () => {
            expect(
                agencyUrl.rewards().changeAgency(321).url(),
            ).toEqual(`${BASE_URL}/agency/321/rewards`);
        });
    });

    const url = appUrl().agency(123);

    describe('применяет путь contract', () => {
        it('сразу', () => {
            expect(
                url.contract(3).url(),
            ).toEqual(`${BASE_URL}/agency/123/contract/3`);
        });

        it('отложенно', () => {
            expect(
                url.contract(123).changeContract(321).url(),
            ).toEqual(`${BASE_URL}/agency/123/contract/321`);
        });

        it('отложенно, через вызов', () => {
            expect(
                url.contract(123).rewards().changeContract(321).url(),
            ).toEqual(`${BASE_URL}/agency/123/contract/321/rewards`);
        });

        it('очистка контракта из путя', () => {
            expect(
                url.contract(123).rewards().changeContract(null).url(),
            ).toEqual(`${BASE_URL}/agency/123/rewards`);
        });
    });

    describe('работа с параметрами', () => {
        it('добавление параметра', () => {
            expect(
                url.params().add('search', '123').path(),
            ).toEqual(`${BASE_URL}/agency/123?search=123`);
        });
        it('добавление параметра array', () => {
            expect(
                url.params().add('search', ['123', '321']).path(),
            ).toEqual(`${BASE_URL}/agency/123?search=123,321`);
        });
    });

    describe('рфбота с кастомным конфигом', () => {
        const url = appUrl('/someUrl', {
            test1: {
                children: {
                    test11: true,
                    test12: {
                        path: 'test-overload-test',
                        param: 'test123',
                    },
                },
            },
            test2: true,
            test3: {
                param: 'test3',
                children: {
                    ss: true,
                },
            },
            test4: {
                param: 'test4',
                optional: true,
            },
        } as const);

        it('проверка переорпеделенных path', () => {
            expect(
                url.test1().test12(123).url(),
            ).toEqual('/someUrl/test1/test-overload-test/123');
        });

        it('проверка если параметр совпадает с именем', () => {
            expect(
                url.test3(321).changeTest3(123).url(),
            ).toEqual('/someUrl/test3/123');
        });

        it('проверка если параметр совпадает с именем и опшинал', () => {
            const test4 = url.test4(null);
            expect(
                test4.url(),
            ).toEqual('/someUrl');
            expect(
                test4.changeTest4(123).url(),
            ).toEqual('/someUrl/test4/123');
        });
    });
});
