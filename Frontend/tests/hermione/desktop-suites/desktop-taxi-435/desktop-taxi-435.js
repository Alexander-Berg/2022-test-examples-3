const indexPage = require('../../page-objects/index');
const { payment, costCenter } = require('../../page-objects');

describe('desktop-taxi-435: Центр затрат. Отображение нового центра затрат', function() {
    it('Полный флоу', async function() {
        const bro = this.browser;
        await bro.auth('cost-center');
        await indexPage.openWithoutComment(bro);

        await bro.pause(5000);

        await bro.waitForVisible(payment.paymentButtonDesktop, 30000);
        await bro.click(payment.paymentButtonDesktop);

        await bro.waitForVisible(payment.modalDesktop, 10000);
        await bro.pause(1000);

        await bro.click(payment.paymentMethod);

        await bro.pause(1000);

        await bro.click(indexPage.costCenterButton);

        await bro.waitForVisible(costCenter.costCenterModal, 10000);
        await bro.click(costCenter.costCenterFirstItemList);

        await bro.waitForVisible(costCenter.costCenterModalInput, 10000);
        await bro.addValue(costCenter.costCenterModalInput, 'ride purpose');
        await bro.waitForEnabled(costCenter.costCenterModalInputDoneButton, 5000);
        await bro.click(costCenter.costCenterModalInputDoneButton);

        const actualItemSubtext = await bro.getText(costCenter.costCenterFirstItemSubtext);
        await bro.assertTexts(actualItemSubtext, 'ride purpose', 'Должен быть указан текст под названием поля = ride purpose');
        await bro.click(costCenter.costCenterDoneButton);

        const actualButtonSubtext = await bro.getText(indexPage.costCenterButtonSubtext);
        await bro.assertTexts(actualButtonSubtext, 'Цель поездки: ride purpose', 'Должен быть указан текст под названием кнопки = Цель поездки: ride purpose');
    });
});
