import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-alphabets.
 *
 * @param {PageObject.Alphabets} alphabets
 */
export default makeSuite('Блок с фильтром по алфавиту. По умолчанию', {
    feature: 'Фильтры',
    story: {
        'отображает английский алфавит': makeCase({
            id: 'marketfront-827',
            issue: 'MARKETVERSTKA-24640',
            test() {
                return this.browser.allure.runStep(
                    'Смотрим, что отображается английский алфавит',
                    () => this.alphabets.itemLangEn.isVisible()
                        .should.eventually.equal(true, 'Отображается английский алфавит')

                        .then(() => this.alphabets.isItemLangRuExisting())
                        .should.eventually.equal(false, 'Русский алфавит скрыт')
                );
            },
        }),
    },
});
