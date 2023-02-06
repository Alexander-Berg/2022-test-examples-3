/* Behold! This file is auto-generated! */
describe('serp-header.patents.base', () => {
    hermione.only.notIn(['firefox'], 'https://st.yandex-team.ru/ISL-7495');
    it('simple', function() {
        return this.browser
            .url('dist/examples/desktop/serp-header/serp-header.patents.base.html')
            .assertView('plain', 'body');
    });
});
