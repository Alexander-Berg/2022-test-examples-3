import {makeSuite, makeCase} from 'ginny';
import ReviewsPhotoFilter from '@self/platform/spec/page-objects/components/ReviewsPhotoFilter';

/**
 * Тест виджета "Фильтр по фото"
 */
export default makeSuite('Есть отзыв с фото.', {
    feature: 'Фильтр с фото',
    issue: 'MARKETVERSTKA-23314',
    story: {
        beforeEach() {
            this.setPageObjects({
                reviewsPhotoFilter: () => this.createPageObject(ReviewsPhotoFilter),
            });
        },

        'Фильтр "С фото" в выключенном состоянии': {
            beforeEach() {
                return this.browser.yaOpenPage(this.params.path, {
                    ...this.params.query,
                    'with-photo': '0',
                });
            },

            'должен корректно отображаться': makeCase({
                id: 'marketfront-1304',
                async test() {
                    await this.reviewsPhotoFilter.isTurnOn()
                        .should.eventually.to.be.equal(false, 'Фильтрация выключена должен быть выключен');
                    return this.reviewsPhotoFilter.isDisabled()
                        .should.eventually.to.be.equal(false, 'Фильтр должен быть доступен для редактирования');
                },
            }),

            'при клике': {
                'должен перезагрузить страницу с параметром with-photo=1': makeCase({
                    id: 'marketfront-1305',
                    async test() {
                        await this.browser.yaWaitForChangeUrl(() =>
                            this.reviewsPhotoFilter.toggleFilter()
                        );
                        const [openedUrl, expectedPath] = await Promise.all([
                            this.browser.getUrl(),
                            this.browser.yaBuildURL(this.params.path, {
                                ...this.params.query,
                                'with-photo': '1',
                            }),
                        ]);
                        return this
                            .expect(openedUrl, 'Проверяем что URL изменился')
                            .to.be.link(expectedPath, {
                                skipProtocol: true,
                                skipHostname: true,
                            });
                    },
                }),
            },
        },

        'Фильтр "С фото" в включенном состоянии': {
            beforeEach() {
                return this.browser.yaOpenPage(this.params.path, {
                    ...this.params.query,
                    'with-photo': '1',
                });
            },

            'должен корректно отображаться': makeCase({
                id: 'marketfront-1301',
                async test() {
                    await this.reviewsPhotoFilter.isTurnOn()
                        .should.eventually.to.be.equal(true, 'Фильтрация выключена должен быть выключен');
                    return this.reviewsPhotoFilter.isDisabled()
                        .should.eventually.to.be.equal(false, 'Фильтр должен быть доступен для редактирования');
                },
            }),

            'при клике': {
                'должен перезагрузить страницу без параметра with-photo': makeCase({
                    id: 'marketfront-1302',
                    async test() {
                        await this.browser.yaWaitForChangeUrl(() =>
                            this.reviewsPhotoFilter.toggleFilter()
                        );

                        const [openedUrl, expectedPath] = await Promise.all([
                            this.browser.getUrl(),
                            this.browser.yaBuildURL(this.params.path, {
                                ...this.params.query,
                                'with-photo': undefined,
                            }),
                        ]);
                        return this
                            .expect(openedUrl, 'Проверяем что URL изменился')
                            .to.be.link(expectedPath, {
                                skipProtocol: true,
                                skipHostname: true,
                            });
                    },
                }),
            },
        },
    },
});
