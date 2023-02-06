import _ from 'lodash';
import {makeSuite, makeCase} from 'ginny';
import Checkbox from '@self/platform/spec/page-objects/checkbox';

/**
 * Тесты на элемент n-reviews-toolbar__filter_type_photo
 *
 * @param {PageObject.ReviewsToolbar} reviewsToolbar
 */
export default makeSuite('Фильтр "С фото"', {
    feature: 'Фильтр с фото',
    issue: 'MARKETVERSTKA-23314',
    story: {
        beforeEach() {
            this.setPageObjects({
                checkbox: () => this.createPageObject(Checkbox, {
                    parent: this.reviewsToolbar,
                    root: '[data-filter="with-photo"]',
                }),
            });
        },

        'в выключенном состоянии': {
            beforeEach() {
                return this.browser.yaOpenPage(this.params.path, _.assign(this.params.query, {
                    'with-photo': '0',
                }));
            },

            'должен корректно отображаться': makeCase({
                id: 'marketfront-1304',
                test() {
                    return this.reviewsToolbar
                        .hasFilterTypePhoto()
                        .should.eventually.to.be.equal(true, 'Фильтр должен быть виден')
                        .then(() => this.checkbox.isChecked())
                        .should.eventually.to.be.equal(false, 'Чекбокс должен быть выключен');
                },
            }),

            'при клике': {
                'должен перезагрузить страницу с параметром with-photo=1': makeCase({
                    id: 'marketfront-1305',
                    test() {
                        return this.browser.yaWaitForPageReloaded(() =>
                            this.checkbox.clickCheckbox('Нажимаем на чекбокс "С фото"')
                        )
                            .then(() => Promise.all([
                                this.browser.getUrl(),
                                this.browser.yaBuildURL(this.params.path, _.assign(this.params.query, {
                                    'with-photo': '1',
                                })),
                            ])
                                .then(([openedUrl, expectedPath]) => this
                                    .expect(openedUrl, 'Проверяем что URL изменился')
                                    .to.be.link(expectedPath, {
                                        skipProtocol: true,
                                        skipHostname: true,
                                    })
                                )
                            );
                    },
                }),
            },
        },

        'во включенном состоянии': {
            beforeEach() {
                return this.browser.yaOpenPage(this.params.path, _.assign(this.params.query, {
                    'with-photo': '1',
                }));
            },

            'должен корректно отображаться': makeCase({
                id: 'marketfront-1301',
                test() {
                    return this.reviewsToolbar
                        .hasFilterTypePhoto()
                        .should.eventually.to.be.equal(true, 'Фильтр должен быть виден')
                        .then(() => this.checkbox.isChecked())
                        .should.eventually.to.be.equal(true, 'Чекбокс должен быть включен');
                },
            }),

            'при клике': {
                'должен перезагрузить страницу с параметром with-photo=0': makeCase({
                    id: 'marketfront-1302',
                    test() {
                        return this.browser.yaWaitForPageReloaded(() =>
                            this.checkbox.clickCheckbox('Нажимаем на чекбокс "С фото"')
                        )
                            .then(() => Promise.all([
                                this.browser.getUrl(),
                                this.browser.yaBuildURL(this.params.path, _.assign(this.params.query, {
                                    'with-photo': '0',
                                })),
                            ])
                                .then(([openedUrl, expectedPath]) => this
                                    .expect(openedUrl, 'Проверяем что URL изменился')
                                    .to.be.link(expectedPath, {
                                        skipProtocol: true,
                                        skipHostname: true,
                                    })
                                )
                            );
                    },
                }),
            },
        },
    },
});
