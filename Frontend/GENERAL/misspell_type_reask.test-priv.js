describeBlock('misspell_type_reask__make-query', function(block) {
    let data;

    beforeEach(function() {
        data = stubData();
    });

    // тест на проверку выделения ошибки при длинном запросе https://st.yandex-team.ru/SERP-64941
    it('should mark words with corrected symbols in long query SERP-64941', function() {
        const reask = {
            raw_source_text: 'скольк\u0007[л\u0007] оста\u0007[д\u0007]ось ди\u0007[р\u0007]ломатов',
            rule: 'Misspell'
        };
        assert.equal(
            block(data, reask),
            '<span class="misspell__error misspell__error_type_bold">скольк<span class="misspell__error">л</span>' +
            '</span> <span class="misspell__error misspell__error_type_bold">оста<span class="misspell__error">' +
            'д</span>ось</span> <span class="misspell__error misspell__error_type_bold">ди' +
            '<span class="misspell__error">р</span>ломатов</span>',
            'should mark words, HTML markup should be correct'
        );
    });
});
