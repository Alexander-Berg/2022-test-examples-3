import { findFirstUrlFormatted } from '../url';

describe('helpers/url', () => {
    describe('#findFirstUrlFormatted', () => {
        describe('external', () => {
            it('skip yandex-team url with skipInternalUrls option', () => {
                expect(findFirstUrlFormatted('https://staff.yandex-team.ru https://ya.ru'))
                    .toBe('https://ya.ru');
            });

            it('save q.yandex-team chat url with skipInternalUrls option', () => {
                expect(findFirstUrlFormatted('https://q.yandex-team.ru https://q.yandex-team.ru/#/join ya.ru'))
                    .toBe('http://ya.ru');
            });
        });

        describe('team', () => {
            let prevWindowFlags = window.flags;

            beforeAll(() => {
                prevWindowFlags = window.flags;
                window.flags = { internal: '1' };
            });

            afterAll(() => {
                window.flags = prevWindowFlags;
            });

            it('skip yandex-team url with skipInternalUrls option', () => {
                expect(findFirstUrlFormatted('https://staff.yandex-team.ru https://ya.ru'))
                    .toBe('https://ya.ru');
            });

            it('save q.yandex-team chat url with skipInternalUrls option', () => {
                expect(findFirstUrlFormatted('https://q.yandex-team.ru https://q.yandex-team.ru/#/join https://ya.ru'))
                    .toBe('https://q.yandex-team.ru/#/join');
            });
        });
    });
});
