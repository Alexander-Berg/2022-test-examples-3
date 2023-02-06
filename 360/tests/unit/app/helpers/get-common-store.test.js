import getCommonStore from '../../../../app/helpers/get-common-store';

jest.mock('@ps-int/ufo-server-side-commons/helpers/bindings-node14');

describe('app/helpers/get-common-store =>', () => {
    const mockedReq = {
        protocol: 'https:',
        get: (param) => param === 'host' ? 'disk.yandex.ru' : undefined,
        originalUrl: '/notes',
        tld: 'ru',
        fullTld: 'ru',
        cookies: {
            yandexuid: '56789000'
        },
        experiments: {
            metrika: 'exp-metrika',
            diskFlags: {
                disk_notes_prod_experiment: true
            }
        },

        ua: {
            BrowserName: 'YandexBrowser',
            OSFamily: 'Ubuntu'
        },
        user: {
            id: '12345',
            auth: true,
            name: '%username%'
        },
        yandexServices: {
            disk: 'https://disk.yandex.ru',
            passport: 'https://passport.yandex.ru'
        },
        lang: 'ru',
        generatedSK: 'generated-sk',
        generatedNoSaltSK: 'generated-no-salt-sk',
        nonce: 'nonce-123'
    };

    it('should map request fields to store', () => {
        expect(getCommonStore(mockedReq)).toMatchSnapshot();
    });

    it('should set errorCode', () => {
        const store = getCommonStore(mockedReq, { errorCode: 500 });
        expect(store.errorCode).toEqual(500);
    });

    it('should fallback if req.experiments does not exist', () => {
        const reqWithoutExperiments = Object.assign(mockedReq);
        delete reqWithoutExperiments.experiments;
        const store = getCommonStore(reqWithoutExperiments);
        expect(store.environment.experiments).toEqual({
            metrika: '',
            flags: {}
        });
    });

    it('should set balloonMode', () => {
        const store = getCommonStore(mockedReq, { balloonMode: true });
        expect(store.environment.balloonMode).toBe(true);
    });

    it('should set broMode', () => {
        const store = getCommonStore(mockedReq, { broMode: true });
        expect(store.environment.broMode).toBe(true);
    });
});
