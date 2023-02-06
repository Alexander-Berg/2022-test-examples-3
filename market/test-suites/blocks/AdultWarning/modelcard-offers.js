import {makeSuite, makeCase} from 'ginny';

import {getSnippetsCount} from './helpers';

/**
 * @param {PageObject.AdultWarning} adultConfirmationPopup
 */
export default makeSuite('Подтверждение возраста на вкладке «Цены» КМ', {
    feature: 'Подтверждение возраста',
    story: {
        async beforeEach() {
            await this.browser.deleteCookie('adult');
            await this.browser.refresh();
        },
        'При нажатии на кнопку «Да»': {
            'открывается выдача': makeCase({
                id: 'marketfront-875',
                issue: 'MARKETVERSTKA-32595',
                async test() {
                    await this.adultConfirmationPopup.clickAccept();
                    await this.browser.yaWaitForPageReady();

                    const snippetsCount = await getSnippetsCount(this);
                    const {expectedSnippetsCount} = this.params;

                    return this.expect(snippetsCount).to.be.equal(
                        expectedSnippetsCount,
                        'Количество сниппетов соответствует ожидаемому'
                    );
                },
            }),
        },
    },
});

