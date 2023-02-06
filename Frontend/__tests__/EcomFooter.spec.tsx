import 'jest';
import * as React from 'react';
import createMockStore from 'redux-mock-store';
import { Provider } from 'react-redux';
import { mount } from 'enzyme';

import { IAppState } from '~/types';
import { getInitialState } from '~/redux/initialState';
import { SupportLinks } from '~/components/SupportLinks';

import { EcomFooter } from '../EcomFooter';

jest.mock('~/hooks/usePageKey', () => ({
    usePageKey: () => 'pageKey',
}));

const storeCreator = createMockStore<IAppState>();

describe('EcomFooter', () => {
    it('Рендерится без ошибок', () => {
        const initialState = getInitialState();
        const store = storeCreator(initialState);
        expect(() => mount(
            <Provider store={store}>
                <EcomFooter />
            </Provider>
        )).not.toThrowError();
    });

    describe('Ссылка на оригинальный магазин', () => {
        it('Подставляется ссылка на глобальный урл магазина, если нет совпадения в редаксе по урлу страницы', () => {
            const state = getInitialState();
            state.meta.originalUrl = 'https://anyurl0.ru/q/w?e=r';
            const store = storeCreator(state);
            const wrapper = mount(
                <Provider store={store}>
                    <EcomFooter />
                </Provider>
            );
            const supportLinks = wrapper.find(SupportLinks);
            expect(supportLinks.prop('url')).toEqual('https://anyurl0.ru/q/w?e=r');
        });

        it('Подставляется ссылка на конкретную страницу магазина', () => {
            const state = getInitialState();
            state.meta.originalUrl = 'https://anyurl0.ru/q/w?e=r';
            /** тот же самый ключ, что и в застабленном модуле получения ключа страницы */
            state.pagesMeta.pageKey = { originalUrl: 'https://random-shop/product?2x2=4' };
            /** и другая страница, что уже закеширована в сторе */
            state.pagesMeta.pageKey2 = { originalUrl: 'https://another-shop/product?2x2=4' };
            const store = storeCreator(state);
            const wrapper = mount(
                <Provider store={store}>
                    <EcomFooter />
                </Provider>
            );
            const supportLinks = wrapper.find(SupportLinks);
            expect(supportLinks.prop('url')).toEqual('https://random-shop/product?2x2=4');
        });
    });
});
