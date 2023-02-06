import React from 'react';
import { Provider } from 'react-redux';
import { mount } from 'enzyme';
import getStore from '../../../../../src/store';
import Footer, { OPERATING_SYSTEMS } from '../../../../../src/components/footer';

jest.mock('@ps-int/ufo-rocks/lib/components/lang-select', () => () => null);
jest.mock('../../../../../src/helpers/metrika', () => ({
    countNote: jest.fn()
}));

import { countNote } from '../../../../../src/helpers/metrika';

const defaultState = {
    environment: {
        currentLang: 'ru',
        availableLangs: ['ru', 'en', 'uk', 'tr'],
        noSaltSk: 'noSaltSk',
        currentUrl: 'currentUrl'
    },
    services: {
        www: 'www',
        support: 'support'
    }
};
const getComponent = () => (
    <Provider store={getStore(defaultState)}>
        <Footer />
    </Provider>
);
const getLinksByName = (wrapper, linkName) =>
    wrapper.find('lego-components_link').findWhere((item) => item.html() && item.html().includes(`notes-footer__app-badge_${linkName}`));

describe('components/footer =>', () => {
    it('should call metrika on app-badges clicks', () => {
        const wrapper = mount(getComponent());

        for (const os of OPERATING_SYSTEMS) {
            getLinksByName(wrapper, os).first().simulate('click');
            expect(popFnCalls(countNote)[0]).toEqual(['footer', `badge ${os}`]);
        }
        expect(wrapper.render()).toMatchSnapshot();
    });
});
