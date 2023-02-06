import rewrite from '../js/rewriter';

function stubRequest(params = {}) {
    return {
        uri: '/foo/bar',
        headersIn: {
            Host: 'turbo.lenta.ru',
        },
        variables: {
            args: '',
        },
        log: jest.fn(),
        args: {},
        ...params,
    };
}

describe('Тесты njs rewrite', () => {
    test('Корректно обрабатывает простой валидный запрос', () => {
        const request = stubRequest();
        const rewritedUri = rewrite(request);

        expect(rewritedUri).toEqual('/turbo');
        expect(request.variables.args).toEqual(`text=${encodeURIComponent('https://lenta.ru/foo/bar')}`);
    });

    test('Корректно обрабатывает запрос с query параметрами турбо и паблишера', () => {
        const request = stubRequest({
            variables: { args: 'pcgi=ID%3D123%26foo%3Dbar&exp_flags=shiny_flag%3Dflag' },
        });
        rewrite(request);

        expect(request.variables.args).toEqual(`text=${encodeURIComponent('https://lenta.ru/foo/bar?ID=123&foo=bar')}&exp_flags=shiny_flag%3Dflag`);
    });

    test('Корректно обрабатывает запрос с query параметрами турбо', () => {
        const request = stubRequest({
            variables: { args: 'turbo_uid=turbo_user&utm_source=turbo' },
        });
        rewrite(request);

        expect(request.variables.args).toEqual(`text=${encodeURIComponent('https://lenta.ru/foo/bar')}&turbo_uid=turbo_user&utm_source=turbo`);
    });

    test('Корректно обрабатывает запрос с query параметрами паблишера', () => {
        const request = stubRequest({
            variables: { args: 'pcgi=foo%3Dbar%26baz%3Dfoo' },
        });
        rewrite(request);

        expect(request.variables.args).toEqual(`text=${encodeURIComponent('https://lenta.ru/foo/bar?foo=bar&baz=foo')}`);
    });

    test('Корректно работает на любом уровне домена', () => {
        const req1 = stubRequest({ headersIn: { Host: 'turbo.m.lenta.ru' } });
        const req2 = stubRequest({ headersIn: { Host: 'turbo.m.tt.lenta.ru' } });

        rewrite(req1);
        rewrite(req2);

        expect(req1.variables.args).toEqual(`text=${encodeURIComponent('https://m.lenta.ru/foo/bar')}`);
        expect(req2.variables.args).toEqual(`text=${encodeURIComponent('https://m.tt.lenta.ru/foo/bar')}`);
    });
});
