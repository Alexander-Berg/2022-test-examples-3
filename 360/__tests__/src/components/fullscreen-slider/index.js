import FullscreenSlider from 'components/fullscreen-slider';
import React from 'react';
import { Provider } from 'react-redux';
import { render } from 'enzyme';
import init from 'store';

const runTest = (state) => {
    global.LANG = 'ru';
    const store = init(Object.assign({
        doc: {
            pages: [
                {
                    index: 1,
                    state: 'READY',
                    imgUrl: 'first-page-url'
                },
                {
                    index: 2,
                    state: 'READY',
                    imgUrl: 'second-page-url'
                },
                {
                    index: 3,
                    state: 'WAIT'
                }
            ]
        }
    }, state));
    const component = render(
        <Provider store={store}>
            <FullscreenSlider/>
        </Provider>
    );
    expect(component).toMatchSnapshot();
};

describe('fullscreen-slider', () => {
    it('active page loaded', () => {
        runTest();
    });

    it('active page loading', () => {
        runTest({
            url: {
                query: {
                    page: 3
                }
            }
        });
    });
});
