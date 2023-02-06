import {
    makeCase,
    makeSuite,
} from 'ginny';

/**
 * @property {PageObject.RecommendedOffers} recommendedOffers
 */
export default makeSuite('Рекомендуемые оффера', {
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'должны быть подписаны для незрячих': makeCase({
                async test() {
                    const EXPECTED_TITLE_TAG = 'h2';
                    const EXPECTED_TITLE_TEXT = 'Где купить';

                    const title = await this.recommendedOffers.title;
                    const titleText = this.recommendedOffers.getTitleText();
                    const titleTag = this.browser.getTagName(title.selector);

                    await titleText.should.eventually.to.be.equal(
                        EXPECTED_TITLE_TEXT,
                        `Содержание заголовка должно быть "${EXPECTED_TITLE_TEXT}".`
                    );
                    await titleTag.should.eventually.to.be.equal(
                        EXPECTED_TITLE_TAG,
                        `Тег заголовка должен быть "${EXPECTED_TITLE_TAG}".`
                    );
                },
            }),
        },
    },
});
