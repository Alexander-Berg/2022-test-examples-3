jest.mock('@ps-int/ufo-server-side-commons/helpers/bindings-node14');
jest.mock('../../../../app/version', () => '45.0.0');
import dispatchCommon from '../../../../app/helpers/dispatch-common';

describe('app/helpers/dispatch-common', () => {
    const store = {
        dispatch: jest.fn()
    };
    const actions = {
        updateUA: jest.fn(() => 'ua updated'),
        updateUser: jest.fn(() => 'user updated'),
        setUrl: jest.fn(() => 'url updated'),
        updateEnvironment: jest.fn(() => 'environment updated'),
        updateServices: jest.fn(() => 'services updated')
    };

    const runTest = (req, data) => {
        const host = req.tld === 'sk' ? 'yadi.sk' : `disk.yandex.${req.tld}`;
        const path = req.tld === 'sk' ? '/i/short-hash' : '/public';
        req = Object.assign({
            path,
            headers: {
                'x-original-uri': req.tld === 'sk' ?
                    `https://${host}${path}` :
                    `https://${host}${path}?hash=long-hash`,
                host
            },
            ua: {
                BrowserName: 'test browser name'
            },
            user: {
                id: 0,
                auth: false
            },
            lang: 'ru',
            cookies: {
                yandexuid: 'test-yandexuid'
            },
            generatedSK: 'generated sk',
            generatedNoSaltSK: 'generated no salt sk'
        }, req);
        dispatchCommon(store, actions, req, data);
        expect(popFnCalls(store.dispatch)).toEqual([
            ['ua updated'],
            ['user updated'],
            ['url updated'],
            ['environment updated'],
            ['services updated']
        ]);
        expect(popFnCalls(actions.updateUA)).toEqual([[req.ua || {}]]);
        expect(popFnCalls(actions.updateUser)).toEqual([[req.user]]);
        expect(popFnCalls(actions.updateServices)).toEqual([[req.yandexServices]]);

        return {
            setUrlCalls: popFnCalls(actions.setUrl),
            updateEnvironmentCalls: popFnCalls(actions.updateEnvironment)
        };
    };

    it('для домена com.tr', () => {
        const { setUrlCalls, updateEnvironmentCalls } = runTest({
            tld: 'tr'
        });
        expect(setUrlCalls).toMatchSnapshot();
        expect(updateEnvironmentCalls).toMatchSnapshot();
    });

    it('nda-паблик', () => {
        const { setUrlCalls, updateEnvironmentCalls } = runTest({
            tld: 'ru',
            headers: {
                host: 'disk.yandex.ru',
                'x-original-uri': 'https://disk.yandex.ru/public/nda?hash=long-hash'
            },
            path: '/public/nda'
        });
        expect(setUrlCalls).toMatchSnapshot();
        expect(updateEnvironmentCalls[0][0].nda).toEqual(true);
    });

    it('noAdv === true если у владельца файла отключена реклама', () => {
        const { updateEnvironmentCalls } = runTest({
            tld: 'sk'
        }, {
            ownerAdvertizingDisabled: true
        });
        expect(updateEnvironmentCalls[0][0].noAdv).toEqual(true);
    });

    it('noAdv === true если домен sk и прошлый заход на паблик был пользователем у которого отключена реклама', () => {
        const { updateEnvironmentCalls } = runTest({
            tld: 'sk',
            visitorFeatures: { advertizingDisabled: true }
        });
        expect(updateEnvironmentCalls[0][0].noAdv).toEqual(true);
    });

    it('noAdv === true если куковый домен и у текущего пользователя отключена реклама', () => {
        const { updateEnvironmentCalls } = runTest({
            tld: 'ua',
            visitorFeatures: { advertizingDisabled: true }
        });
        expect(updateEnvironmentCalls[0][0].noAdv).toEqual(true);
    });
});
