import { getInitialState } from '~/redux/initialState';
import { IAppState } from '~/types';
import { suggestUrlSelector } from '../suggest-url';

describe('Suggest url selector', () => {
    let state: IAppState;

    beforeEach(() => {
        state = getInitialState();
    });

    it('Возвращает url, содержащий корректные параметры', () => {
        state.meta.tld = 'ua';
        state.meta.shopId = 'test';

        const url = new URL(suggestUrlSelector(state));

        expect(url.origin).toBe('https://suggest-multi.yandex.net');
        expect(url.pathname).toBe('/suggest-ecom');
        expect(url.searchParams.get('shop'), 'Некорректный параметр shop').toBe('test');
        expect(url.searchParams.get('srv'), 'Некорректный параметр srv').toBe('turbo-ecom_ua_touch');
    });

    it('Возвращает url, содержащий флаги из turbo-app-ecom-suggest-params', () => {
        state.meta.tld = 'ua';
        state.meta.shopId = 'test';
        state.meta.expFlags = {
            'turbo-app-ecom-suggest-params': 'disable_fulltext_suggestions=1&relative_url=1'
        };

        const url = new URL(suggestUrlSelector(state));

        expect(url.searchParams.get('disable_fulltext_suggestions'), 'Некорректный параметр disable_fulltext_suggestions').toBe('1');
        expect(url.searchParams.get('relative_url'), 'Некорректный параметр relative_url').toBe('1');
    });
});
