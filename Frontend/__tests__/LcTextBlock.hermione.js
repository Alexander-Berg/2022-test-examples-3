specs({
    feature: 'LcTextBlock',
}, () => {
    describe('SEO тэги', () => {
        const expectedStyles = {
            fontFamily: 'YS Text',
            fontSize: '13px',
            fontWeight: '400',
            margin: '0px',
            lineHeight: '22px',
        };

        function testTag(browser, stub) {
            return browser
                .url(`/turbo?stub=lctextblock/${stub}.json`)
                .yaWaitForVisible(PO.lcTextBlock(), 'LcTextBlock не появился')
                .assertView('plain', PO.lcTextBlock())
                .execute(function(selector) {
                    var el = document.querySelector(selector);
                    var styles = window.getComputedStyle(el);

                    return {
                        // Правим различия в браузерах
                        // YS Text и "YS Text"
                        // Arial,sans-serif и Arial, sans-serif
                        fontFamily: styles.fontFamily.replace(/"/g, '').replace(/, /g, ','),
                        fontSize: styles.fontSize,
                        fontWeight: styles.fontWeight,
                        // в firefox margin отдается пустой строкой
                        margin: styles.margin || '0px',
                        lineHeight: styles.lineHeight,
                    };
                }, PO.lcStyledText.text())
                .then(styles => {
                    assert.deepEqual(styles.value, expectedStyles, 'Применились неправильные стили');
                });
        }

        hermione.only.notIn('safari13');
        it('Блок без тэга', function() {
            return testTag(this.browser, 'default');
        });

        hermione.only.notIn('safari13');
        it('Блок с SEO тэгом none', function() {
            return testTag(this.browser, 'none');
        });

        hermione.only.notIn('safari13');
        it('Блок с SEO тэгом h1', function() {
            return testTag(this.browser, 'h1');
        });

        hermione.only.notIn('safari13');
        it('Блок с SEO тэгом h2', function() {
            return testTag(this.browser, 'h2');
        });

        hermione.only.notIn('safari13');
        it('Блок с SEO тэгом h3', function() {
            return testTag(this.browser, 'h3');
        });

        hermione.only.notIn('safari13');
        it('Блок с SEO тэгом h4', function() {
            return testTag(this.browser, 'h4');
        });

        hermione.only.notIn('safari13');
        it('Блок с SEO тэгом h5', function() {
            return testTag(this.browser, 'h5');
        });

        hermione.only.notIn('safari13');
        it('Блок с SEO тэгом h6', function() {
            return testTag(this.browser, 'h6');
        });
    });
});
