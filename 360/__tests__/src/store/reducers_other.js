import * as actions from '../../../src/store/actions';
import * as consts from '../../../src/store/consts';
import TOKEN_ARG from '../../../src/lib/token-arg';
import init from '../../../src/store';

const { actions: actionTypes } = consts;

let store = null;
const resetStore = () => {
    store = init({
        url: {
            pathname: '/',
            query: {
                [TOKEN_ARG]: 'token'
            }
        },
        doc: {
            pages: new Array(10)
        }
    });
};

beforeEach(() => {
    resetStore();
});

describe('url_reducer', () => {
    it('updateURL', () => {
        store.dispatch(actions.updateURL({ [TOKEN_ARG]: 'some token' }, '/some/pathname/', 'my-dv.ya.ru'));
        expect(store.getState().url).toMatchSnapshot();

        store.dispatch(actions.updateURL({ [TOKEN_ARG]: 'another token', page: 5 }, '/another/pathname/', 'not-my-dv.ya.com.tr'));
        expect(store.getState().url).toMatchSnapshot();
    });

    it('goToPage', () => {
        // push state
        store.dispatch(actions.goToPage(6));
        expect(store.getState().url).toMatchSnapshot();

        // replace state
        store.dispatch(actions.goToPage(7, true));
        expect(store.getState().url).toMatchSnapshot();
    });

    it('updateDoc', () => {
        store.dispatch(actions.updateURL({ page: 4 }, ''));
        store.dispatch(actions.updateDoc({ pages: [1, 2, 3, 4] }));
        expect(store.getState().url.query).toMatchSnapshot();

        // тут страница сбрасывается на 1, так как в документе страниц меньше, чем 4
        store.dispatch(actions.updateDoc({ pages: [1, 2, 3] }));
        expect(store.getState().url.query).toMatchSnapshot();
    });
});

describe('user_reducer', () => {
    it('updateUser', () => {
        store.dispatch(actions.updateUser({
            id: 42,
            auth: true,
            accounts: [
                {
                    id: '4004594257',
                    login: 'iegit20',
                    name: 'iegit20',
                    avatar: 'https://avatars.mdst.yandex.net/get-yapic/0/0-0/islands-middle'
                },
                {
                    id: '4004594258',
                    login: 'kri0-gen',
                    name: 'kri0-gen',
                    avatar: 'https://avatars.mdst.yandex.net/get-yapic/1/0-0/islands-middle'
                }
            ]
        }));
        expect(store.getState().user).toMatchSnapshot();

        store.dispatch(actions.updateUser({ id: 0, auth: false, accounts: [] }));
        expect(store.getState().user).toMatchSnapshot();
    });
});

describe('cfg_reducer', () => {
    it('updateCfg', () => {
        store.dispatch(actions.updateCfg({
            tld: 'com',
            lang: 'en',
            osFamily: 'MacOs',
            passport: 'https://passport-test.yandex.com',
            mail: 'https://mail.yandex.com'
        }));
        expect(store.getState().cfg).toMatchSnapshot();

        store.dispatch(actions.updateCfg({
            tld: '',
            lang: '',
            osFamily: '',
            passport: '',
            mail: ''
        }));
        expect(store.getState().cfg).toMatchSnapshot();
    });
});

describe('fullscreen_reducer', () => {
    it('UPDATE_FULLSCREEN', () => {
        store.dispatch({
            type: actionTypes.UPDATE_FULLSCREEN,
            fullscreen: true
        });
        expect(store.getState().fullscreen).toEqual(true);

        store.dispatch({
            type: actionTypes.UPDATE_FULLSCREEN,
            fullscreen: false
        });
        expect(store.getState().fullscreen).toEqual(false);
    });
});

describe('context_reducer', () => {
    it('goToPage', () => {
        // initial state
        expect(store.getState().context).toEqual({});

        // change page without previous page saving
        store.dispatch(actions.goToPage(1));
        expect(store.getState().context).toEqual({});

        // change page with previous page saving
        const originalDateNow = Date.now;
        Date.now = () => 123456;
        store.dispatch(actions.goToPage(2, false, true));
        expect(store.getState().context).toEqual({ previousPage: 1, pageChangeTimestamp: 123456 });
        Date.now = originalDateNow;
    });
});
