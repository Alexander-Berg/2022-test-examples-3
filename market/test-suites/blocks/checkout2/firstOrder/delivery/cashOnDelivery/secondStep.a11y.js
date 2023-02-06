import {makeCase, makeSuite} from 'ginny';

import {
    fillFirstStepOfFirstOrder,
} from '@self/root/src/spec/hermione/scenarios/checkout';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';

export default makeSuite('Оформление первого заказа. Шаг 2. Доступность', {
    id: 'marketfront-5323',
    issue: 'MARKETFRONT-58840',
    feature: 'Оформление первого заказа. Шаг 2. Доступность',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            const carts = [
                buildCheckouterBucket({
                    items: [{
                        skuMock: kettle.skuMock,
                        offerMock: kettle.offerMock,
                        count: 1,
                    }],
                }),
            ];

            await this.browser.yaScenario(
                this,
                fillFirstStepOfFirstOrder,
                carts
            );
        },
        'Проверить навигацию по форме получателя для незрячих.': {
            'Должен быть главный заголовок': makeCase({
                async test() {
                    const EXPECTED_TITLE_TEXT = 'Получатель';
                    const EXPECTED_TITLE_TAG = 'h1';

                    const title = await this.recipientForm.title;
                    const titleText = this.recipientForm.getTitleText();
                    const titleTag = this.browser.getTagName(title.selector);

                    await titleText.should.eventually.to.be.equal(
                        EXPECTED_TITLE_TEXT,
                        `Текст главного заголовка страницы должен быть "${EXPECTED_TITLE_TEXT}".`
                    );
                    await titleTag.should.eventually.to.be.equal(
                        EXPECTED_TITLE_TAG,
                        `Тег главного заголовка страницы должен быть "${EXPECTED_TITLE_TEXT}".`
                    );
                },
            }),
            'Должен быть заголовок у предупреждения о реальных данных': makeCase({
                async test() {
                    const EXPECTED_TITLE_TEXT = 'Указывайте\nреальные данные';
                    const EXPECTED_TITLE_TAG = 'h3';

                    const title = await this.recipientInfo.informationPanelTitle;
                    const titleText = this.recipientInfo.getInformationPanelTitleText();
                    const titleTag = this.browser.getTagName(title.selector);

                    await titleText.should.eventually.to.be.equal(
                        EXPECTED_TITLE_TEXT,
                        `Текст заголовка предупреждения должен быть "${EXPECTED_TITLE_TEXT}".`
                    );
                    await titleTag.should.eventually.to.be.equal(
                        EXPECTED_TITLE_TAG,
                        `Тег заголовка предупреждениядолжен быть "${EXPECTED_TITLE_TAG}".`
                    );
                },
            }),
        },
    },
});
