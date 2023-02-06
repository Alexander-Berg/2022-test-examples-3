import {makeCase, makeSuite} from '@yandex-market/ginny';

export default makeSuite('Бейдж рейтинга оффера', {
    environment: 'kadavr',
    feature: 'searchSnippetCard',
    story: {
        'По-умолчанию': {
            'Отображается корректно': makeCase({
                async test() {
                    await this.searchSnippet.waitForVisible();
                    const ratingHintSelector = await this.operationalRatingHintContent.getSelector();
                    const operationalRatingBadgeSelector = await this.operationalRatingBadge.getSelector();
                    await this.browser.scroll(operationalRatingBadgeSelector, 0, -200);
                    await this.browser.moveToObject(operationalRatingBadgeSelector);
                    await this.operationalRatingHintContent.waitForVisible();
                    return this.browser.assertView('OperationalRatingHintSelector', ratingHintSelector, {
                        compositeImage: true,
                    });
                },
            }),
        },
    },
});
