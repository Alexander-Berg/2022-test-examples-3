import {makeCase, makeSuite} from 'ginny';

import Roll from '@self/platform/spec/page-objects/Roll';
import ContentPreview from '@self/platform/spec/page-objects/ContentPreview';

import OfferComplaintButton from '@self/platform/spec/page-objects/components/OfferComplaintButton';

const COUNT_OF_SNIPPET_ON_PAGE = 10;

/**
 * Тесты на виджет Roll
 * @param {PageObject.Roll} roll
 */
export default makeSuite('Roll', {
    feature: 'Лента рекомендаций.',
    story: {
        beforeEach() {
            this.setPageObjects({
                roll: () => this.createPageObject(Roll),
                contentPreview: () => this.createPageObject(ContentPreview),
                offerComplaintButton: () => this.createPageObject(OfferComplaintButton),
            });
        },
        'По умолчанию': {
            'лента присутствует на странице.': makeCase({
                id: 'm-touch-2359',
                issue: 'MOBMARKET-5456',
                async test() {
                    await this.browser.yaSlowlyScroll(Roll.root);
                    await this.browser.scroll(Roll.root, 0, -200);

                    return this.roll.content.isVisible()
                        .should
                        .eventually
                        .equal(true, 'Виджет отображается');
                },
            }),
        },
        'При скролле': {
            'появляется кнопка "Показать Еще".': makeCase({
                id: 'm-touch-2360',
                issue: 'MOBMARKET-5456',
                async test() {
                    await this.browser.yaSlowlyScroll(Roll.loadMoreButton);
                },
            }),
        },
        'При клике по кнопке "Показать Еще"': {
            'появляются еще сниппеты.': makeCase({
                id: 'm-touch-1991',
                issue: 'MOBMARKET-5456',
                async test() {
                    // Две страницы за раз
                    await this.browser.yaSlowlyScroll(Roll.loadMoreButton);

                    await this.roll.clickLoadMoreButton();
                    await this.allure.runStep(
                        'Ждем появления новых сниппетов',
                        // eslint-disable-next-line market/ginny/no-pause
                        this.browser.pause(1000)
                    );

                    return this.roll.getSnippetsCount()
                        .should
                        .eventually
                        .equal(COUNT_OF_SNIPPET_ON_PAGE * 3, 'Новые сниппеты загрузились');
                },
            }),
        },
        'По клику на первый сниппет в ленте': {
            'открывается контент превью этого товара': makeCase({
                id: 'm-touch-1474',
                issue: 'MOBMARKET-5455',
                async test() {
                    await this.browser.yaSlowlyScroll(Roll.root);
                    await this.browser.scroll(Roll.root, 0, -200);
                    await this.roll.clickSnippetByIndex(1);

                    return this.contentPreview.isVisible()
                        .should
                        .eventually
                        .to
                        .be
                        .equal(true, 'Контент превью открылся');
                },
            }),
        },
        'По клику на первый сниппет': {
            'открывается контент превью с ссылкой на магазин': makeCase({
                id: 'm-touch-1993',
                issue: 'MOBMARKET-5459',
                async test() {
                    const regexp = /market-click2([\w-]+)?\.yandex\.ru/gi;
                    await this.browser.yaSlowlyScroll(Roll.root);
                    await this.browser.scroll(Roll.root, 0, -200);
                    await this.roll.clickSnippetByIndex(1);

                    const {href} = await this.contentPreview.getButtonShopUrl();

                    return this.expect(href)
                        .to
                        .match(regexp, 'В ссылке содержится market-click2.yandex.ru');
                },
            }),
        },
    },
});
