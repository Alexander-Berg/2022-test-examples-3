import {makeCase, makeSuite} from 'ginny';

import {getBonusString} from '@self/root/src/utils/string';
import PaymentSystemCashbackInfo from '@self/root/src/components/CashbackInfos/PaymentSystemCashbackInfo/__pageObject';

function createStory(shouldShow) {
    const common = {
        async beforeEach() {
            this.setPageObjects({
                paymentSystemCashbackInfo: () => this.createPageObject(PaymentSystemCashbackInfo),
            });


            await this.params.prepareState.call(this);
        },
    };

    if (shouldShow) {
        return {
            ...common,
            'По умолчанию Отображается': makeCase({
                async test() {
                    await this.paymentSystemCashbackInfo.isVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Кешбэк должен отображаться'
                        );
                },
            }),
            'Содержит корректный текст': makeCase({
                async test() {
                    const {cashbackAmount, paymentSystemCashbackAmount} = this.params;

                    const expectedText =
                        `${
                            cashbackAmount
                                ? `${cashbackAmount} ${getBonusString(cashbackAmount)}\n и еще\n`
                                : ''
                        }${paymentSystemCashbackAmount} ${getBonusString(paymentSystemCashbackAmount)}\n с Mastercard`;


                    await this.paymentSystemCashbackInfo.getText()
                        .should.eventually.to.be.include(
                            expectedText,
                            `Текст должен содержать ${expectedText}`
                        );
                },
            }),
        };
    }

    return {
        ...common,
        'Не отображается': makeCase({
            async test() {
                await this.paymentSystemCashbackInfo.isVisible()
                    .should.eventually.to.be.equal(
                        false,
                        'Кешбэк должен отображаться'
                    );
            },
        }),
    };
}

// /**
//  * Конструктор тестов промо кешбэка при оплате определенной платежной системой.
//  * Является общим генератором набора тестов, для различных кейсов.
//  *
//  * @param {boolean} shouldShow указывает должен ли отображаться акционный кешбэк
//  **/
export default ({shouldShow} = {shouldShow: true}) => makeSuite('Кешбэк при оплате акционной платежной системой.', {
    issue: 'MARKETFRONT-60935',
    environment: 'kadavr',
    params: {
        prepareState: 'Функция определяющая состояние приложения под конкретный кейс',
        cashbackAmount: 'Размер кешбэка у оффера',
        paymentSystemCashbackAmount: 'Размер кешбэка у оффера',
        isExtraCashback: 'Повышенный кешбэк у оффера',
    },
    defaultParams: {
        isAuthWithPlugin: true,
    },
    story: createStory.call(this, shouldShow),
});
