'use strict';

// Неактуальные test-id отсеиваются в ответе сервера при запросе.
// Поэтому при переснятии дампов необходимо выбрать test-id из актуальных.
const TEST_ID = '588289';

specs({
    feature: 'Серверные счетчики',
    type: 'Test-ids'
}, function() {
    it('Записываются при открытии страницы', async function() {
        await this.browser.yaOpenSerp({
            text: 'test',
            'test-id': TEST_ID,
            'no-tests': '',
            data_filter: 'no_results'
        }, '.b-page');

        await this.browser.yaCheckServerCounter({ path: '/test-ids', vars: { value: TEST_ID } });
    });
});
