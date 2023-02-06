import { makeLink } from './zen';

describe('helpers', () => {
    describe('zen', () => {
        it('should add utm', () => {
            expect(makeLink({ baseUrl: 'https://ya.ru', utmContent: 'foo' }))
                .toMatch('https://ya.ru/?utm_source=yandex-weather&utm_campaign=spa_zen&utm_content=foo');
        });

        it('should replace utm', () => {
            expect(makeLink({ baseUrl: 'https://ya.ru?utm_source=google_weather&utm_campaign=desktop_zen&utm_content=none&foo=bar', utmContent: 'test' }))
                .toMatch('https://ya.ru/?utm_source=yandex-weather&utm_campaign=spa_zen&utm_content=test&foo=bar');
        });
    });
});
