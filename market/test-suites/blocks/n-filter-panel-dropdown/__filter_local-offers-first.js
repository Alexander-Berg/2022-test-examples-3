import _ from 'lodash';
import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Сортировка "Сначала предложения в моём регионе"', {
    feature: 'Галка Сначала предложения в моем регионе',
    story: {
        'В выключенном состоянии': {
            beforeEach() {
                return this.browser.yaOpenPage(this.params.path, _.assign(this.params.query, {
                    'local-offers-first': '0',
                }));
            },

            'должна корректно отображаться': makeCase({
                issue: 'MARKETVERSTKA-26246',
                id: 'marketfront-1643',
                test() {
                    return this.checkbox
                        .isChecked()
                        .should.eventually.to.be.equal(false, 'Чекбокс должен быть выключен');
                },
            }),

            'при клике': {
                'должна добавить параметр local-offers-first=1 в адрес': makeCase({
                    issue: 'MARKETVERSTKA-26244',
                    id: 'marketfront-1638',
                    async test() {
                        await this.checkbox.clickCheckbox('Нажимаем на чекбокс "Сначала предложения в моём регионе"');
                        await this.browser.yaWaitForChangeUrl(null, 10000);
                        const currentUrl = await this.browser.getUrl();

                        return this.expect(currentUrl, 'Проверяем параметры в URL')
                            .to.be.link({
                                query: {
                                    'local-offers-first': '1',
                                },
                            }, {
                                mode: 'match',
                                skipProtocol: true,
                                skipHostname: true,
                            });
                    },
                }),
            },
        },

        'Во включенном состоянии': {
            beforeEach() {
                return this.browser.yaOpenPage(this.params.path, _.assign(this.params.query, {
                    'local-offers-first': '1',
                }));
            },

            'должна корректно отображаться': makeCase({
                issue: 'MARKETVERSTKA-26843',
                id: 'marketfront-2026',
                test() {
                    return this.checkbox
                        .isChecked()
                        .should.eventually.to.be.equal(true, 'Чекбокс должен быть включен');
                },
            }),

            'при клике': {
                'должна добавить параметр local-offers-first=0 в адрес': makeCase({
                    issue: 'MARKETVERSTKA-26245',
                    id: 'marketfront-1639',
                    async test() {
                        await this.checkbox.clickCheckbox('Нажимаем на чекбокс "Сначала предложения в моём регионе"');
                        await this.browser.yaWaitForChangeUrl(null, 10000);
                        const currentUrl = await this.browser.getUrl();

                        return this.expect(currentUrl, 'Проверяем параметры в URL')
                            .to.be.link({
                                query: {
                                    'local-offers-first': '0',
                                },
                            }, {
                                mode: 'match',
                                skipProtocol: true,
                                skipHostname: true,
                            });
                    },
                }),
            },
        },
    },
});
