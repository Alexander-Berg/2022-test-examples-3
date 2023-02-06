import { mount } from 'enzyme';
import Root from '../../../../../src/components/root';
import getStore from '../../../../../src/store';
import Header from '../../../../../src/components/header';
import Footer from '../../../../../src/components/footer';

jest.mock('../../../../../src/components/user', () => () => null);
jest.mock('@ps-int/ufo-rocks/lib/components/lang-select', () => () => null);
jest.mock('../../../../../src/components/notes', () => () => null);

import Notes from '../../../../../src/components/notes';

const initialState = {
    environment: {
        nonce: '123',
        tld: 'ru',
        currentLang: 'ru',
        availableLangs: ['en', 'uk', 'tr'],
        noSaltSk: 'noSaltSk',
        currentUrl: 'currentUrl'
    },
    services: {
        www: 'www',
        support: 'support'
    }
};

describe('components/root =>', () => {
    it('should render all main components on desktop device', () => {
        const wrapper = mount(Root({
            store: getStore(initialState)
        }));

        expect(wrapper.find(Header).exists()).toBe(true);
        expect(wrapper.find(Notes).exists()).toBe(true);
        expect(wrapper.find(Footer).exists()).toBe(true);
    });

    it('should render all main components except footer on touch devices', () => {
        global.IS_TOUCH = true;

        const wrapper = mount(Root({
            store: getStore(initialState)
        }));

        expect(wrapper.find(Header).exists()).toBe(true);
        expect(wrapper.find(Notes).exists()).toBe(true);
        expect(wrapper.find(Footer).exists()).toBe(false);
        global.IS_TOUCH = false;
    });
});
