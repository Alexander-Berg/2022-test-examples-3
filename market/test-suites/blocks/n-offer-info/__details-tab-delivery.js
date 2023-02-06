import {makeSuite, makeCase} from 'ginny';

/**
 * Сьют тестов на вкладку о доставке и самовывозе.
 * @param {PageObject.OfferInfo} offerInfo
 * @param {Array} params.prohibitedWords - Список запрещенных слов.
 */
export default makeSuite('Вкладка о доставке и самовывозе', {
    params: {
        prohibitedWords: 'список запрещенных слов',
    },
    story: {
        'По умолчанию': {
            'не содержит запрещенных слов': makeCase({
                async test() {
                    const deliveryTabText = await this.offerInfo.deliveryTabContent.getText();
                    const prohibitedWordsRegExp = new RegExp(this.params.prohibitedWords.join('|'), 'i');
                    const hasProhibitedWords = prohibitedWordsRegExp.test(deliveryTabText);

                    await this.expect(hasProhibitedWords).to.be.equal(
                        false,
                        `не содержит запрещенных слов: ${this.params.prohibitedWords.join()}`
                    );
                },
            }),
        },
    },
});
