import { findFirstUrl, findFirstUrlFormatted } from '../findFirstUrl';

const Q_URL_REGEXP = /q\.yandex-team\.ru\/\#/i;
const YT_URL_REGEXP = /yandex-team\.ru/i;

export function urlTeamFilter(url: string) {
    return (
        !YT_URL_REGEXP.test(url) ||
        Q_URL_REGEXP.test(url)
    );
}

describe('TextFormatter findFirstUrl', () => {
    describe('#findFirstUrl', () => {
        it('returns first url from text', () => {
            expect(findFirstUrlFormatted('Some text \n WoW https://ya.ru zZz')).toEqual('https://ya.ru');
            expect(findFirstUrlFormatted('Some text \n WoW https://ya.ru zZz http://google.com')).toEqual('https://ya.ru');
        });

        it('returns full url when protocol-less url found', () => {
            expect(findFirstUrlFormatted('Some text www.yandex.ru')).toBe('http://www.yandex.ru');
            expect(findFirstUrlFormatted('Some text yandex.ru')).toBe('http://yandex.ru');
        });

        it('returns empty string when url not found', () => {
            expect(findFirstUrlFormatted('')).toBe('');
            expect(findFirstUrlFormatted('Some text without url')).toBe('');
        });

        it('skip url in code tag', () => {
            expect(findFirstUrlFormatted('``` https://beru.ru ``` https://ya.ru')).toBe('https://ya.ru');
        });

        it('skip url in inline-code tag', () => {
            expect(findFirstUrlFormatted('` https://beru.ru ` https://ya.ru')).toBe('https://ya.ru');
        });

        it('skip yandex-team url with skipInternalUrls option', () => {
            expect(findFirstUrlFormatted('https://staff.yandex-team.ru https://ya.ru', urlTeamFilter))
                .toBe('https://ya.ru');
        });

        it('save q.yandex-team chat url with skipInternalUrls option', () => {
            expect(findFirstUrlFormatted('https://q.yandex-team.ru https://q.yandex-team.ru/#/join https://ya.ru', urlTeamFilter))
                .toBe('https://q.yandex-team.ru/#/join');
        });

        it('punctuation', () => {
            expect(findFirstUrlFormatted('https://ya.ru,')).toBe('https://ya.ru');

            expect(findFirstUrlFormatted('https://ya.ru/test, test test')).toBe('https://ya.ru/test');
        });

        it('common checks is urls', () => {
            [
                ['qwerthttps://ya.ru', 'ya.ru'],
                ['https://ya.ru'],
                ['https://ya.ru/path?qu=e&r=y#fragment'],
                ['ya.ru'],
                ['ya.ru/path'],
                ['ya.ru?query=1'],
                ['ya.ru#fragment'],
                ['ya.ru/path/#fragment'],
                ['ya.ru/#fragment'],
                ['(https://ya.ru', 'https://ya.ru'],
                ['https://ya.ru)', 'https://ya.ru'],
                ['https://ru.wikipedia.org/wiki/Ссылка'],
                ['onliner.by'],
                ['россия.рф'],
                ['(https://ya.ru/path)', 'https://ya.ru/path'],
                ['https://st.yandex-team.ru/issues/?q=Resolution=empty()'],
                ['https://st.yandex-team.ru/issues/?q=Resolution=empty()&a=b'],
                ['(https://st.yandex-team.ru/issues/?q=Resolution=empty())', 'https://st.yandex-team.ru/issues/?q=Resolution=empty()'],
                ['https://youtu.be/4ky207pW_e4'],
                ['youtu.be/4ky207pW_e4'],
                ['bit.ly/2sTUzuD'],
                ['www.kinopoi.sk/iXAF'],
                ['kinopoi.sk/iXAF'],
                ['http://www.kinopoi.sk/iXAF'],
                ['amp.gs/dsRO'],
                ['goo.gl/7BqBqE'],
                ['telegra.ph/Newochyom-ishchet-redaktora-12-17'],
                ['t.co/zMUmy6EiE1'],
                ['vk.cc/8zvhob'],
                ['ali.ski/5Qee1'],
                ['fas.st/qc8GM'],
                ['teletype.in/@hatemyself4life/rJAqbDUCB'],
                ['twitch.tv/pochezach'],
                ['lentach.media/ad9b'],
                ['news.lenta.ch/iLe8'],
                ['tlg.name'],
                ['https://www.notion.so/da0d7dfbbcbd439c84776ee426e118ec'],
            ].forEach(([str, expected]) => {
                expect(findFirstUrl(str)).toBe(expected || str);
            });
        });

        it('common checks is not urls', () => {
            [
                'https://ya.sixsymloremipsum',
                'romikabi@yandex.ru',
            ].forEach((url) => {
                expect(findFirstUrl(url)).toBe('');
            });
        });
    });
});
