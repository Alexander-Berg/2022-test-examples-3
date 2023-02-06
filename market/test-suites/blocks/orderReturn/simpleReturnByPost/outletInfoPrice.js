import {
    makeSuite,
    makeCase,
} from 'ginny';
import assert from 'assert';

import {getOutletName} from '@self/root/src/entities/outlet/getters';
import {currency} from '@self/root/src/entities/price';
import {selectOutlet} from '@self/root/src/spec/hermione/scenarios/returns';

export default makeSuite('Стоимость возврата через ПВЗ', {
    params: {
        outlet: 'Выбранный ПВЗ',
        returnOptions: 'Опции возврата',
        isFreeReturn: 'Является ли возврат в ПВЗ бесплатным',
    },
    story: {
        async beforeEach() {
            assert(this.params.outlet, 'Param outlet must be defined');
            assert(this.params.returnOptions, 'Param returnOptions must be defined');
        },

        'После выбора пункта возврата на карте': {
            'отображается корректная информация о стоимости возврата': makeCase({
                async test() {
                    const {outlet, returnOptions} = this.params;

                    await this.browser.yaScenario(this, selectOutlet, {outlet});

                    await this.returnMapSuggest.waitForHidden();

                    const outletId = Number(outlet.id);

                    const outletDeliveryService = returnOptions.deliveryOptions.find(option =>
                        option.outletIds.includes(outletId)
                    );

                    const returnPrice = outletDeliveryService.price;
                    const isFreeReturn = this.params.isFreeReturn || returnPrice.price === 0;
                    const priceText = isFreeReturn
                        ? 'бесплатно'
                        : `от ${returnPrice.price} ${currency.RUB}`;

                    await this.returnMapOutletInfo.getTitle()
                        .should.eventually.be.equal(
                            `Возврат через ${getOutletName(outlet)}, ${priceText}`,
                            'Заголовок информации о ПВЗ должен быть корректным'
                        );

                    await this.returnMapOutletInfo.isCargoPriceDisclaimerVisible()
                        .should.eventually.be.equal(
                            !isFreeReturn,
                            `Уведомление о стоимости возврата
                            ${isFreeReturn ? 'не' : ''} должно быть отображено`
                        );
                },
            }),
        },
    },
});
