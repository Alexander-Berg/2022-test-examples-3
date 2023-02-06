import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-alphabets.
 *
 * @param {PageObject.Alphabets} alphabets
 */
export default makeSuite('Блок с фильтром по алфавиту.', {
    feature: 'Фильтры',
    story: {
        'Контрол с алфавитом.': {
            'Для английского фильтра': {
                'содержит ссылки на страницу с фильтром на английскую букву': makeCase({
                    id: 'marketfront-831',
                    issue: 'MARKETVERSTKA-24636',
                    test() {
                        return this.alphabets.getEnLettersUrls()
                            .then(links => this.browser.allure.runStep(
                                'Проверяем ссылки фильтров английского алфавита',
                                () => Promise.all(
                                    links.map(link => this.expect(link).to.be.link({
                                        pathname: '/brands',
                                        query: {
                                            char: '[a-z]',
                                        },
                                    }, {
                                        mode: 'match',
                                        skipProtocol: true,
                                        skipHostname: true,
                                    }))))
                            );
                    },
                }),
            },

            'Для русского фильтра': {
                'содержит ссылки на страницу с фильтром на русскую букву': makeCase({
                    id: 'marketfront-832',
                    issue: 'MARKETVERSTKA-24635',
                    test() {
                        let prevState;

                        return this.alphabets.getLangState()
                            .then(state => {
                                prevState = state;
                            })
                            .then(() => this.alphabets.clickLangControl())
                            .then(() => this.alphabets.waitForStateChanged(prevState))
                            .then(() => this.alphabets.getRuLettersUrls())
                            .then(links => this.browser.allure.runStep(
                                'Проверяем ссылки фильтров русского алфавита', () => {
                                    links.forEach(link => {
                                        link.should.be.link({
                                            pathname: '/brands',
                                            query: {
                                                char: '[а-яё]',
                                            },
                                        }, {
                                            mode: 'match',
                                            skipProtocol: true,
                                            skipHostname: true,
                                        });
                                    });
                                }));
                    },
                }),
            },
        },
    },
});
