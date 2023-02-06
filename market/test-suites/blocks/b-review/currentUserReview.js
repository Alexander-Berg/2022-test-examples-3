import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.Review} review
 */
export default makeSuite('Блок с отзывом на товар, автор которой текущий пользователь.', {
    params: {
        productId: 'Id продукта',
        slug: 'Slug продукта',
    },
    environment: 'kadavr',
    story: {
        'Кнопка "Изменить".': {
            'По умолчанию': {
                'отображается': makeCase({
                    feature: 'Страница мои отзывы',
                    id: 'm-touch-2320',
                    issue: 'MOBMARKET-9426',
                    async test() {
                        await this.review.hasEditLink()
                            .should.eventually.to.be.equal(true, 'Кнопка "изменить" отображается');
                    },
                }),
                'ведет на страницу редактирования отзыва на товар': makeCase({
                    feature: 'Страница мои отзывы',
                    id: 'm-touch-2325',
                    issue: 'MOBMARKET-9428',
                    async test() {
                        const actualHref = await this.review.getEditLinkHref();
                        await this.expect(actualHref).to.be.link({
                            pathname: `/product--${this.params.slug}/${this.params.productId}/reviews/add`,
                        }, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                    },
                }),
            },
        },
        'Кнопка "Удалить".': {
            'По умолчанию': {
                Отображается: makeCase({
                    feature: 'Страница мои отзывы',
                    id: 'm-touch-1628',
                    issue: 'MOBMARKET-6070',
                    async test() {
                        await this.review.hasDeleteLink()
                            .should.eventually.to.be.equal(true, 'Кнопка удалить отображается');
                    },
                }),
            },
            'При нажатии и подтверждении удаления': {
                'отзыв удаляется': makeCase({
                    feature: 'Страница мои отзывы',
                    id: 'm-touch-2322',
                    issue: 'MOBMARKET-9425',
                    async test() {
                        await this.review.clickDeleteLink();
                        await this.browser.allure.runStep(
                            'Подтверждаем удаление',
                            () => this.browser.alertAccept()
                        );
                        await this.review.isRootVisible()
                            .should.eventually.to.be.equal(false, 'Отзыв больше не отображается');
                        await this.browser.refresh();
                        await this.review.isRootVisible()
                            .should.eventually.to.be.equal(false, 'После перезагрузки страницы отзыв не отображается');
                    },
                }),
            },
            'При нажатии и отмене удаления': {
                'отзыв не удаляется': makeCase({
                    feature: 'Страница мои отзывы',
                    id: 'm-touch-1690',
                    issue: 'MOBMARKET-9424',
                    async test() {
                        await this.review.clickDeleteLink();
                        await this.browser.allure.runStep(
                            'Подтверждаем удаление',
                            () => this.browser.alertDismiss()
                        );
                        await this.review.isRootVisible()
                            .should.eventually.to.be.equal(true, 'Отзыв по прежнему отображается');
                        await this.browser.refresh();
                        await this.review.isRootVisible()
                            .should.eventually.to.be.equal(true, 'После перезагрузки страницы отзыв отображается');
                    },
                }),
            },
        },

    },
});
