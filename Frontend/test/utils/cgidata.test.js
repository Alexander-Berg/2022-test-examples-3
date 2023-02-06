const {
    getExpFlags,
    getDeajaxedArgs,
    buildDeajaxedUrl,
    buildUserFriendlyUrl,
    getUTMArgs,
    safeRemoveQueryArgs,
} = require('../../core/utils/cgidata');

jest.mock('../../.flags_allowed.json', () => ({
    'flag-name-1': {
        date: '20.01.2018',
        description: 'like google',
        manager: 'sbmaxx',
        expire: '-',
    },
    'flag-name-2': {
        date: '20.01.2018',
        description: 'like bing',
        manager: 'sbmaxx',
        expire: '-',
    },
    'flag-internal': {
        date: '20.01.2018',
        description: 'like yachan',
        manager: 'sbmaxx',
        expire: '-',
        internal: true,
    },
}), { virtual: true });

describe('utils cgidata', () => {
    describe('getExpFlags', () => {
        it('Фильтрует флаги из expflags', () => {
            expect(getExpFlags({ 'flag-name-1': true, 'unknown-flags': true }))
                .toEqual({ 'flag-name-1': true });
        });

        it('Фильтрует внутренние флаги во внешней сети', () => {
            expect(getExpFlags({ 'flag-internal': true }))
                .toEqual({});
        });

        it('Не фильтрует внутренние флаги во внутренней сети', () => {
            expect(getExpFlags({ 'flag-internal': true }, true))
                .toEqual({ 'flag-internal': true });
        });
    });

    describe('getDeajaxedArgs', () => {
        it('Не фильтрует обычные параметры', () => {
            expect([...getDeajaxedArgs('hype=1&exp_flags=some_flag%3D1&exp_flags=some_other_flag%3D2')])
                .toEqual([
                    ['hype', '1'],
                    ['exp_flags', 'some_flag=1'],
                    ['exp_flags', 'some_other_flag=2'],
                ]);
        });

        it('Фильтрует text', () => {
            expect([...getDeajaxedArgs('text=bbbbbbb&hype=1&exp_flags=some_flag%3D1&exp_flags=some_other_flag%3D2')])
                .toEqual([
                    ['hype', '1'],
                    ['exp_flags', 'some_flag=1'],
                    ['exp_flags', 'some_other_flag=2'],
                ]);
        });

        it('Фильтрует аяксовые параметры в формате массивов', () => {
            expect([...getDeajaxedArgs([
                ['text', 'bbbbbbb'],
                ['ajax_type', 'related'],
                ['hype', '1'],
                ['exp_flags', 'some_flag=1'],
                ['exp_flags', 'some_other_flag=2'],
                ['bundles', '["bundles/cover","bundles/link","bundles/divider","bundles/title","bundles/markup","bundles/paragraph","bundles/source","bundles/button","bundles/footer","bundles/related","bundles/autoload","bundles/api-request"]'],
                ['icons', '["turbo"]'],
            ])]).toEqual([
                ['hype', '1'],
                ['exp_flags', 'some_flag=1'],
                ['exp_flags', 'some_other_flag=2'],
            ]);
        });

        it('Фильтрует аяксовые параметры в формате объектов', () => {
            expect([...getDeajaxedArgs({
                text: 'bbbbbbb',
                ajax_type: 'related',
                hype: '1',
                exp_flags: ['some_flag=1', 'some_other_flag=2'],
                bundles: '["bundles/cover","bundles/link","bundles/divider","bundles/title","bundles/markup","bundles/paragraph","bundles/source","bundles/button","bundles/footer","bundles/related","bundles/autoload","bundles/api-request"]',
                icons: '["turbo"]]',
            })]).toEqual([
                ['hype', '1'],
                ['exp_flags', 'some_flag=1'],
                ['exp_flags', 'some_other_flag=2'],
            ]);
        });

        it('Фильтрует аяксовые параметры', () => {
            expect([...getDeajaxedArgs('text=bbbbbbb&ajax_type=related&hype=1&exp_flags=some_flag%3D1&exp_flags=some_other_flag%3D2&bundles=[%22bundles/cover%22,%22bundles/link%22,%22bundles/divider%22,%22bundles/title%22,%22bundles/markup%22,%22bundles/paragraph%22,%22bundles/source%22,%22bundles/button%22,%22bundles/footer%22,%22bundles/related%22,%22bundles/autoload%22,%22bundles/api-request%22]&icons=[%22turbo%22]')])
                .toEqual([
                    ['hype', '1'],
                    ['exp_flags', 'some_flag=1'],
                    ['exp_flags', 'some_other_flag=2'],
                ]);
        });
    });

    describe('buildDeajaxedUrl', () => {
        it('Не фильтрует обычные параметры и не добавляет лишний "&" после "?"', () => {
            expect(buildDeajaxedUrl('/turbo?', { text: 'hype=1&exp_flags=some_flag%3D1&exp_flags=some_other_flag%3D2' }))
                .toEqual('/turbo?hype=1&exp_flags=some_flag%3D1&exp_flags=some_other_flag%3D2');
        });

        it('Фильтрует text', () => {
            expect(buildDeajaxedUrl('/turbo?', { text: 'text=bbbbbbb&hype=1&exp_flags=some_flag%3D1&exp_flags=some_other_flag%3D2' }))
                .toEqual('/turbo?hype=1&exp_flags=some_flag%3D1&exp_flags=some_other_flag%3D2');
        });

        it('Фильтрует аяксовые параметры', () => {
            expect(buildDeajaxedUrl('/turbo?', { text: 'text=bbbbbbb&ajax_type=related&hype=1&exp_flags=some_flag%3D1&exp_flags=some_other_flag%3D2&bundles=[%22bundles/cover%22,%22bundles/link%22,%22bundles/divider%22,%22bundles/title%22,%22bundles/markup%22,%22bundles/paragraph%22,%22bundles/source%22,%22bundles/button%22,%22bundles/footer%22,%22bundles/related%22,%22bundles/autoload%22,%22bundles/api-request%22]&icons=[%22turbo%22]' }))
                .toEqual('/turbo?hype=1&exp_flags=some_flag%3D1&exp_flags=some_other_flag%3D2');
        });

        it('Добавляет "?" к строке без cgi-параметров', () => {
            expect(buildDeajaxedUrl('/turbo', { text: 'text=bbbbbbb&hype=1&exp_flags=some_flag%3D1&exp_flags=some_other_flag%3D2' }))
                .toEqual('/turbo?hype=1&exp_flags=some_flag%3D1&exp_flags=some_other_flag%3D2');
        });

        it('Добавляет "&" после cgi-параметров', () => {
            expect(buildDeajaxedUrl('/turbo?flag1=1', { text: 'text=bbbbbbb&hype=1&exp_flags=some_flag%3D1&exp_flags=some_other_flag%3D2' }))
                .toEqual('/turbo?hype=1&exp_flags=some_flag%3D1&exp_flags=some_other_flag%3D2&flag1=1');
        });

        it('Не дублирует cgi параметры', () => {
            expect(buildDeajaxedUrl('/turbo?step=1', { text: 'step=bbbbbbb&data=22222' }))
                .toEqual('/turbo?data=22222&step=1');
        });

        it('Вставляет дублирующиеся url параметры без изменений', () => {
            expect(buildDeajaxedUrl('/turbo?step=1&step=2', { text: 'step=bbbbbbb&step=ccccc&data=22222' }))
                .toEqual('/turbo?data=22222&step=1&step=2');
        });
    });

    describe('buildUserFriendlyUrl', () => {
        it('Не фильтрует обычные параметры', () => {
            expect(buildUserFriendlyUrl('/turbo?hype=1&exp_flags=some_flag%3D1&exp_flags=some_other_flag%3D2#123'))
                .toEqual('/turbo?hype=1&exp_flags=some_flag%3D1&exp_flags=some_other_flag%3D2#123');
        });

        it('Фильтрует технические параметры', () => {
            expect(buildUserFriendlyUrl('/turbo?text=bbbbbbb&hype=1&exp_flags=some_flag%3D1&exp_flags=some_other_flag%3D2&from=web&parent-reqid=123&fallback=1&null='))
                .toEqual('/turbo?text=bbbbbbb&hype=1&exp_flags=some_flag%3D1&exp_flags=some_other_flag%3D2');
        });
    });

    describe('getUTMArgs', () => {
        it('Извлекает только значения UTM-параметров', () => {
            expect(getUTMArgs({ args: {
                utm_content: ['sdas d'],
                utm_campaign: ['3', '5'],
                hype: ['1'],
                utm_source: ['1'],
                utm_term: ['4'],
                exp_flags: ['some_flag=1', 'some_other_flag=2'],
                utm_medium: ['2', '5'],
                text: ['test-landing-1'],
            } })).toEqual({
                utm_source: '1',
                utm_medium: '2',
                utm_campaign: '3',
                utm_term: '4',
                utm_content: 'sdas d',
            });
        });
    });

    describe('safeRemoveQueryArgs', () => {
        it('Корректно удаляет параметры без декодирования', () => {
            const url = new URL('https://yandex.ru/turbo?text=https%3A//en.wikipedia.org&remove=me&export=json&exp_flags&empty=&delete=');

            const result = safeRemoveQueryArgs(url, ['remove', 'delete']);

            expect(result.toString()).toStrictEqual(
                'https://yandex.ru/turbo?text=https%3A//en.wikipedia.org&export=json&exp_flags&empty='
            );
        });
    });
});
