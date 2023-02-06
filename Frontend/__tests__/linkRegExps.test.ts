import { LINK_REGEXP_STR, MD_LINK_REGEXP_STR } from '../linkRegExps';

function match(regexStr: string, str: string, md: boolean = false) {
    const _match = (new RegExp(regexStr, 'ig')).exec(str);
    // match[0] - total match
    // match[1] - preceeding character
    // match[2] - url / ip
    return _match ? (md ? _match[0] : _match[2]) : null;
}

function combine(...lists: string[][]) {
    const combinations: number = lists.reduce((res, list) => res * (list.length || 1), 1);
    const result: string[][] = Array.from({ length: combinations }, () => []);

    let frame = combinations;
    lists.forEach((list) => {
        const frameItem = frame / (list.length || 1);
        result.forEach((sublist, idx) => sublist.push(list[(idx % frame / frameItem) | 0]));
        frame = frameItem;
    });

    return result;
}

describe('Link RegExps', () => {
    const URLS = combine(
        ['', 'www.', 'http://', 'https://'],
        ['yandex.ru', 'яндекс.рф'],
        [
            '', '/', '/?query=()',
            '/path?query=()()', '/path?query=(fo(bar)o)',
            '/?query=()#hash/path', '/path///?query=()',
            '/query=купить', '/query=\u00a9', '/\u00a9/\u00a9?\u00a9',
            '/query=a–b–c', '/a–b/c–d?e–f',
        ],
    )
        /** @todo Убрать фильтрацию после ухода с node-8.6.0. */
        .filter((params) => !(params[0] === '' && params[1] === 'яндекс.рф'))
        .map((params) => params.join(''));

    describe('#link', () => {
        URLS.map((url) =>
            it(url, () => {
                expect(match(LINK_REGEXP_STR, `Lorem ${url} ipsum`)).toBe(url);
            }));

        URLS.map((url) =>
            it(`"${url}"`, () => {
                expect(match(LINK_REGEXP_STR, `Lorem "${url}" ipsum`)).toBe(url);
            }));

        URLS.map((url) =>
            it(`\`${url}\``, () => {
                expect(match(LINK_REGEXP_STR, `Lorem \`${url}\` ipsum`)).toBe(url);
            }));

        URLS.map((url) =>
            it(`[link](${url})`, () => {
                expect(match(MD_LINK_REGEXP_STR, `Lorem [link](${url}) ipsum`, true)).toBe(`[link](${url})`);
            }));

        it('Should match valid links (particular cases)', () => {
            expect(match(LINK_REGEXP_STR, 'https://ru.wikipedia.org/wiki/Ссылка')).toBe('https://ru.wikipedia.org/wiki/Ссылка');
            expect(match(LINK_REGEXP_STR, 'onliner.by')).toBe('onliner.by');
            expect(match(LINK_REGEXP_STR, 'россия.рф')).toBe('россия.рф');
            expect(match(LINK_REGEXP_STR, 'https://xn--80aaccidch5emabxe2o.xn--p1ai/shop/item/2839')).toBe('https://xn--80aaccidch5emabxe2o.xn--p1ai/shop/item/2839');
            expect(match(LINK_REGEXP_STR, 'https://youtu.be/4ky207pW_e4')).toBe('https://youtu.be/4ky207pW_e4');
            expect(match(LINK_REGEXP_STR, 'youtu.be/4ky207pW_e4')).toBe('youtu.be/4ky207pW_e4');
            expect(match(LINK_REGEXP_STR, 'bit.ly/2sTUzuD')).toBe('bit.ly/2sTUzuD');
            expect(match(LINK_REGEXP_STR, 'www.kinopoi.sk/iXAF')).toBe('www.kinopoi.sk/iXAF');
            expect(match(LINK_REGEXP_STR, 'kinopoi.sk/iXAF')).toBe('kinopoi.sk/iXAF');
            expect(match(LINK_REGEXP_STR, 'http://www.kinopoi.sk/iXAF')).toBe('http://www.kinopoi.sk/iXAF');
            expect(match(LINK_REGEXP_STR, 'amp.gs/dsRO')).toBe('amp.gs/dsRO');
            expect(match(LINK_REGEXP_STR, 'goo.gl/7BqBqE')).toBe('goo.gl/7BqBqE');
            expect(match(LINK_REGEXP_STR, 'telegra.ph/Newochyom-ishchet-redaktora-12-17')).toBe('telegra.ph/Newochyom-ishchet-redaktora-12-17');
            expect(match(LINK_REGEXP_STR, 't.co/zMUmy6EiE1')).toBe('t.co/zMUmy6EiE1');
            expect(match(LINK_REGEXP_STR, 'vk.cc/8zvhob')).toBe('vk.cc/8zvhob');
            expect(match(LINK_REGEXP_STR, 'ali.ski/5Qee1')).toBe('ali.ski/5Qee1');
            expect(match(LINK_REGEXP_STR, 'fas.st/qc8GM')).toBe('fas.st/qc8GM');
            expect(match(LINK_REGEXP_STR, 'teletype.in/@hatemyself4life/rJAqbDUCB')).toBe('teletype.in/@hatemyself4life/rJAqbDUCB');
            expect(match(LINK_REGEXP_STR, 'twitch.tv/pochezach')).toBe('twitch.tv/pochezach');
            expect(match(LINK_REGEXP_STR, 'lentach.media/ad9b')).toBe('lentach.media/ad9b');
            expect(match(LINK_REGEXP_STR, 'news.lenta.ch/iLe8')).toBe('news.lenta.ch/iLe8');
            expect(match(LINK_REGEXP_STR, 'tlg.name')).toBe('tlg.name');
        });

        it('in partial valid link should be detected valid part', () => {
            expect(match(LINK_REGEXP_STR, 'http:/ya.ru')).toBe('ya.ru');
            expect(match(LINK_REGEXP_STR, '/ya.ru/')).toBe('ya.ru/');
            expect(match(LINK_REGEXP_STR, 'ya.ru/((()))')).toBe('ya.ru/');
            expect(match(LINK_REGEXP_STR, 'ya.ru/()())')).toBe('ya.ru/()()');
            expect(match(LINK_REGEXP_STR, '(ya.ru)')).toBe('ya.ru');
            expect(match(LINK_REGEXP_STR, '(ya.ru?())')).toBe('ya.ru?()');
            expect(match(LINK_REGEXP_STR, '=>ya.ru=>')).toBe('ya.ru');
            expect(match(LINK_REGEXP_STR, 'string-with-ya.ru-in-middle')).toBe('string-with-ya.ru');
        });

        it('Should match links with parentheses', () => {
            const url = 'https://error.yandex-team.ru/projects/morda' +
                '/projectDashboard?filter=environment%20==%20production' +
                '%20AND%20platform%20==%20touch%20AND%20block%20==%20i-' +
                'messenger&period=hour&componentSettings=[%22row_1_col_0' +
                '_con_0_charts%22:{%22field%22:%22block%22,%22columns%22:[%22countErrors%22]}}';
            expect(match(LINK_REGEXP_STR, url)).toBe(url);
        });

        it('invalid link should be skipped', () => {
            expect(match(LINK_REGEXP_STR, 'ya.bazbazbaz')).toBeNull();
            expect(match(LINK_REGEXP_STR, 'http://ya .ru')).toBeNull();
            expect(match(LINK_REGEXP_STR, 'stringwithya.rusinmiddle')).toBeNull();
        });

        it('Should parse only valid domains', () => {
            expect(match(LINK_REGEXP_STR, 'президент.рф')).toBe('президент.рф');
            expect(match(LINK_REGEXP_STR, 'сайт.москва')).toBe('сайт.москва');
            expect(match(LINK_REGEXP_STR, 'сайт.бел')).toBe('сайт.бел');
            expect(match(LINK_REGEXP_STR, 'сайт.укр')).toBe('сайт.укр');
            expect(match(LINK_REGEXP_STR, '2ch.hk')).toBe('2ch.hk');
            expect(match(LINK_REGEXP_STR, 'web.dev')).toBe('web.dev');
            expect(match(LINK_REGEXP_STR, 'dev.google')).toBe('dev.google');
            expect(match(LINK_REGEXP_STR, 'cloud.yandex')).toBe('cloud.yandex');
            expect(match(LINK_REGEXP_STR, 'Привет.Хорошо')).toBeNull();
            expect(match(LINK_REGEXP_STR, 'text.slice')).toBeNull();
        });

        it('Should match ip only with protocol', () => {
            expect(match(LINK_REGEXP_STR, 'https://1.2.3.4')).toBe('https://1.2.3.4');
            expect(match(LINK_REGEXP_STR, '1.2.3.4')).toBeNull();
        });

        it('Should not match local network ip even with protocol', () => {
            expect(match(LINK_REGEXP_STR, 'https://127.0.0.1')).toBeNull();
        });
    });
});
