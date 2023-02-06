import Banner from '../../../../../src/components/banner';

import React from 'react';
import { mount, render } from 'enzyme';
import getStore from '../../store';

import { Provider } from 'react-redux';

jest.mock('../../../../../src/lib/metrika');
import { countMainBranch } from '../../../../../src/lib/metrika';

const runTest = (state, isDeepLink, useWindowLocation = false) => {
    const store = getStore(state);
    const openLinkFn = useWindowLocation ? window.location.assign : window.open;
    expect(popFnCalls(openLinkFn).length).toEqual(0);
    expect(popFnCalls(countMainBranch).length).toEqual(0);
    const component = mount(
        <Provider store={store}>
            <Banner/>
        </Provider>
    );
    expect(component.render()).toMatchSnapshot();
    component.simulate('click');

    const windowOpenCalls = popFnCalls(openLinkFn);
    expect(windowOpenCalls.length).toEqual(1);
    expect(windowOpenCalls[0]).toMatchSnapshot();

    const countMainBranchCalls = popFnCalls(countMainBranch);
    expect(countMainBranchCalls.length).toEqual(2);
    expect(countMainBranchCalls).toEqual([
        ['smart banner', isDeepLink ? 'open' : 'install', 'show'],
        ['smart banner', isDeepLink ? 'open' : 'install', 'click']
    ]);
    component.unmount();
};

const originalWindowOpen = window.open;
let mockedWindowOpen;
const originalWindowLocationAssign = window.location.assign;
let mockedWindowLocationAssign;
describe('banner =>', () => {
    beforeEach(() => {
        mockedWindowOpen = jest.fn(() => window);
        window.open = mockedWindowOpen;
        mockedWindowLocationAssign = jest.fn(() => window);
        window.location.assign = mockedWindowLocationAssign;
    });
    afterEach(() => {
        window.open = originalWindowOpen;
        window.location.assign = originalWindowLocationAssign;
    });

    it('Android >= 4.4 (deep link)', () => {
        runTest({
            ua: {
                OSFamily: 'Android',
                isSupportedAndroid: true
            },
            url: {
                pathname: '/i/hash',
                query: {}
            }
        }, true);
    });

    it('Android >= 4.4 для ссылок /mail/ (должен быть НЕ deep link)', () => {
        runTest({
            ua: {
                OSFamily: 'Android',
                isSupportedAndroid: true
            },
            url: {
                pathname: '/mail/',
                query: {}
            }
        });
    });

    it('Android >= 4.4 для ссылок /public/nda/ (должен быть НЕ deep link)', () => {
        runTest({
            ua: {
                OSFamily: 'Android',
                isSupportedAndroid: true
            },
            url: {
                pathname: '/public/nda/',
                query: {}
            }
        });
    });

    it('Android < 4.4', () => {
        runTest({
            ua: {
                OSFamily: 'Android',
                isSupportedAndroid: false
            },
            url: { query: {} }
        });
    });

    it('iOS', () => {
        runTest({
            ua: {
                OSFamily: 'iOS'
            },
            url: { query: {} }
        });
    });

    it('iOS(ChromeMobile)', () => {
        runTest({
            ua: {
                OSFamily: 'iOS',
                BrowserName: 'ChromeMobile'
            },
            url: { query: {} }
        }, false, true);
    });

    it('Windows Phone (не должно быть баннера)', () => {
        const store = getStore({
            ua: {
                OSFamily: 'WindowsPhone'
            },
            url: { query: {} }
        });
        expect(
            render(
                <Provider store={store}>
                    <Banner/>
                </Provider>
            )
        ).toMatchSnapshot();
    });
});
