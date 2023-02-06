describeBlock('serp-bk-counter_type_plain__result-url', function(block) {
    it('should return counters for native logs by default', function() {
        assert.equal(
            block('https://yandex.ru/clck/unit-test'),
            'bk848484(https://yandex.ru/clck/unit-test)',
            'Ссылка на счетчик обернута в неправильную последовательность'
        );
    });

    it('should return counters for prefetch in exp', () => {
        RequestCtx.GlobalContext.expFlags['suggest_prefetch_html'] = 1;

        assert.deepEqual(
            block('https://yandex.ru/clck/unit-test'),
            {
                block: 'html-prefetch-counter',
                content: 'https://yandex.ru/clck/unit-test'
            },
            'Счетчик установлен неправильно'
        );
    });

    it('should return counters for native logs in exp YaBro', () => {
        RequestCtx.GlobalContext.expFlags['suggest_prefetch_html'] = 1;
        RequestCtx.GlobalContext.isYandexSearchBrowser = true;

        assert.equal(
            block('https://yandex.ru/clck/unit-test'),
            'bk848484(https://yandex.ru/clck/unit-test)',
            'Ссылка на счетчик обернута в неправильную последовательность'
        );
    });

    it('should return counters for native logs in exp Searchapp', () => {
        RequestCtx.GlobalContext.expFlags['suggest_prefetch_html'] = 1;
        RequestCtx.GlobalContext.isSearchApp = true;

        assert.equal(
            block('https://yandex.ru/clck/unit-test'),
            'bk848484(https://yandex.ru/clck/unit-test)',
            'Ссылка на счетчик обернута в неправильную последовательность'
        );
    });
});
