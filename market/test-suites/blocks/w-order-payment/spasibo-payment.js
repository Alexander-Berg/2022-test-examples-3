import {makeCase, makeSuite} from 'ginny';

import OrderPayment from '@self/root/src/widgets/parts/Payment/components/View/__pageObject';

const paymentStatusTimeout = OrderPayment.paymentStatusTimeout;

const checkFormElementsVisibility = async ctx => {
    const [
        isSpasiboInfoVisible,
        isPayWithMoneyButtonVisible,
        isPayWithSpasiboButtonVisible,
    ] = await Promise.all([
        ctx.trustFrame.isSpasiboInfoVisible(),
        ctx.trustFrame.isPayWithMoneyButtonVisible(),
        ctx.trustFrame.isPayWithSpasiboButtonVisible(),
    ]);

    await ctx.expect(isSpasiboInfoVisible)
        .to.equal(true, 'Плашка с информацией об оплате СберСпасибо должна быть видна');
    await ctx.expect(isPayWithMoneyButtonVisible)
        .to.equal(true, 'Кнопка "Оплатить деньгами" должна быть видна');
    await ctx.expect(isPayWithSpasiboButtonVisible)
        .to.equal(true, 'Кнопка "Оплатить СберСпасибо" должна быть видна');
};

const fillCardFormAndWaitForSberCheck = async (ctx, {card}) => {
    // Форма не успевает проинититься и вводится только кусок номера карты
    // eslint-disable-next-line market/ginny/no-pause
    await ctx.browser.pause(1000);

    await ctx.browser.yaScenario(ctx, 'trust.fillForm', {card});

    // Ждем, пока форма pcidss проверит бин на сбербанковость; обычно происходит очень быстро
    // Не используй pause без необходимости! Используй вейтеры (waitFor..., waitUntil)!
    // Массовая правка с добавлением игнора
    // eslint-disable-next-line market/ginny/no-pause
    await ctx.browser.pause(1000);
};

const checkButtonsState = async (ctx, {payWithMoney, payWithSpasibo}) => {
    const [
        isPayWithMoneyButtonDisabled,
        isPayWithSpasiboButtonDisabled,
    ] = await Promise.all([
        ctx.trustFrame.isPayWithMoneyButtonDisabled(),
        ctx.trustFrame.isPayWithSpasiboButtonDisabled(),
    ]);

    await ctx.expect(isPayWithMoneyButtonDisabled)
        .to.equal(!payWithMoney, `Кнопка "Оплатить деньгами" должна быть ${payWithMoney ? '' : 'не'}активна`);
    await ctx.expect(isPayWithSpasiboButtonDisabled)
        .to.equal(!payWithSpasibo, `Кнопка "Оплатить СберСпасибо" должна быть ${payWithSpasibo ? '' : 'не'}активна`);
};

const checkWarningText = async (ctx, {text}) => {
    const [
        isSpasiboWarningOrErrorVisible,
        warningOrErrorText,
    ] = await Promise.all([
        ctx.trustFrame.isSpasiboWarningOrErrorVisible(),
        ctx.trustFrame.getSpasiboWarningOrErrorText(),
    ]);

    await ctx.expect(isSpasiboWarningOrErrorVisible)
        .to.equal(true, 'Предупреждение должно быть показано');
    await ctx.expect(warningOrErrorText)
        .to.equal(text, `Текст предупреждения должен быть "${text}"`);
};

const thankYouHeader = 'Заказ оформлен!';

const checkThankYou = async ctx => {
    const isThankYouVisible =
        await ctx.orderConfirmation.waitForCheckoutThankyouIsVisible(paymentStatusTimeout);

    await ctx.expect(isThankYouVisible)
        .to.equal(true, 'Страница подтверждения заказа должна быть видна');

    const header = await ctx.orderConfirmation.getTitle();
    await ctx.expect(header).to.equal(thankYouHeader, `Заголовок должен быть ${thankYouHeader}`);
};

const suppressBeforeUnload = async ctx => {
    await ctx.browser.execute(() => {
        const originalAEL = window.addEventListener;

        window.addEventListener = function (...args) {
            originalAEL.apply(this, args);

            if (args[0] === 'beforeunload') {
                window.removeEventListener(...args);
            }
        };
    });
};

export default makeSuite('Оплата бонусами СберСпасибо.', {
    feature: 'Оплата бонусами СберСпасибо',
    environment: 'testing',
    story: {
        'Сберокарта со СберСпасибо.': {
            'При оплате заказа деньгами': {
                'оплата проходит успешно и пользователь видит экран "Спасибо! Заказ оформлен"': makeCase({
                    id: 'bluemarket-2570',
                    issue: 'BLUEMARKET-5075',
                    async test() {
                        await this.trustFrameComponent.waitForVisibleWithinViewport(30 * 1000);
                        await this.trustFrameComponent.switchToContentFrame();
                        await checkFormElementsVisibility(this);
                        await fillCardFormAndWaitForSberCheck(this, {card: 'sber-spasibo'});
                        await this.trustFrame.payWithMoney();
                        await this.trustFrameComponent.switchToParentFrame();
                        await checkThankYou(this);
                    },
                }),
            },
        },

        'Сберокарта с нулевым балансом СберСпасибо.': {
            'При вводе карты Сбера с нулевым балансом СберСпасибо': {
                'кнопка "Оплатить СберСпасибо" становится недоступна, выводится предупреждение': makeCase({
                    id: 'bluemarket-2568',
                    issue: 'BLUEMARKET-5075',
                    async test() {
                        const warningText = 'Ой, на этой карте недостаточно бонусов СберСпасибо. '
                            + 'Может быть, у вас есть другая карта Сбербанка? Или вы можете оплатить заказ деньгами.';

                        await suppressBeforeUnload(this);
                        await this.trustFrameComponent.waitForVisibleWithinViewport(30 * 1000);
                        await this.trustFrameComponent.switchToContentFrame();
                        await fillCardFormAndWaitForSberCheck(this, {card: 'sber-zero-spasibo'});
                        await checkButtonsState(this, {payWithMoney: true, payWithSpasibo: true});
                        await this.trustFrame.payWithSpasibo();
                        await this.trustFrame.waitForSpinnerOverlayIsVisible();
                        await this.trustFrame.waitForSpinnerOverlayIsInvisible(30 * 1000);
                        await checkButtonsState(this, {payWithMoney: true, payWithSpasibo: false});
                        await checkWarningText(this, {text: warningText});
                    },
                }),
            },
        },

        'Сберокарта, не участвующая в программе СберСпасибо': {
            'При вводе такой карты': {
                'кнопка "Оплатить СберСпасибо" становится недоступна, выводится предупреждение': makeCase({
                    id: 'bluemarket-2569',
                    issue: 'BLUEMARKET-5075',
                    async test() {
                        const warningText = 'Ой, а эта карта не участвует в СберСпасибо. '
                            + 'Может быть, у вас есть другая карта Сбербанка? '
                            + 'Или вы можете оплатить заказ деньгами.';

                        await suppressBeforeUnload(this);
                        await this.trustFrameComponent.waitForVisibleWithinViewport(30 * 1000);
                        await this.trustFrameComponent.switchToContentFrame();
                        await fillCardFormAndWaitForSberCheck(this, {card: 'sber-not-spasibo'});
                        await checkButtonsState(this, {payWithMoney: true, payWithSpasibo: true});
                        await this.trustFrame.payWithSpasibo();
                        await this.trustFrame.waitForSpinnerOverlayIsVisible();
                        await this.trustFrame.waitForSpinnerOverlayIsInvisible(30 * 1000);
                        await checkButtonsState(this, {payWithMoney: true, payWithSpasibo: false});
                        await checkWarningText(this, {text: warningText});
                    },
                }),
            },
        },

        'Не-сберокарта.': {
            'При вводе карты не Сбера': {
                'кнопка "Оплатить СберСпасибо" недоступна, выводится предупреждение': makeCase({
                    id: 'bluemarket-2567',
                    issue: 'BLUEMARKET-5075',
                    async test() {
                        const warningText = 'Чтобы оплатить заказ бонусами СберСпасибо, используйте карту Сбербанка. '
                            + 'Или оплатите деньгами с этой карты.';

                        await suppressBeforeUnload(this);
                        await this.trustFrameComponent.waitForVisibleWithinViewport(30 * 1000);
                        await this.trustFrameComponent.switchToContentFrame();
                        await fillCardFormAndWaitForSberCheck(this, {card: 'valid'});
                        await checkButtonsState(this, {payWithMoney: true, payWithSpasibo: false});
                        await checkWarningText(this, {text: warningText});
                    },
                }),
            },
        },
    },
});
