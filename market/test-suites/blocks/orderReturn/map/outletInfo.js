import {
    makeSuite,
    makeCase,
} from 'ginny';
import assert from 'assert';

import {getOutletName} from '@self/root/src/entities/outlet/getters';
import {currency} from '@self/root/src/entities/price';
import {selectOutlet} from '@self/root/src/spec/hermione/scenarios/returns';

export default makeSuite('Информация о ПВЗ', {
    issue: 'MARKETFRONT-47969',
    id: 'marketfront-4671',
    params: {
        outlet: 'Выбранный ПВЗ',
        returnOptions: 'Опции возврата',
    },
    story: {
        async beforeEach() {
            assert(this.params.outlet, 'Param outlet must be defined');
            assert(this.params.returnOptions, 'Param returnOptions must be defined');
        },

        'После выбора пункта возврата на карте': {
            'отображается корректная информация о данном пункте': makeCase({
                async test() {
                    const {outlet, returnOptions} = this.params;

                    await this.browser.yaScenario(this, selectOutlet, {outlet});

                    await this.returnMapSuggest.waitForHidden()
                        .should.eventually.be.equal(
                            true,
                            'Инпут поиска по улице должен быть скрыт'
                        );

                    await this.returnMapOutletInfo.waitForVisible()
                        .should.eventually.be.equal(
                            true,
                            'Информация о выбранном ПВЗ должна быть отображена'
                        );

                    const outletId = Number(outlet.id);

                    const outletDeliveryService = returnOptions.deliveryOptions.find(option =>
                        option.outletIds.includes(outletId)
                    );

                    const returnPrice = outletDeliveryService.price;
                    const isFreeReturn = returnPrice.price === 0;
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

                    await this.returnMapOutletInfo.isAddressVisible()
                        .should.eventually.be.equal(
                            true,
                            'Блок с адресом ПВЗ должен быть отображён'
                        );

                    await this.returnMapOutletInfo.isScheduleVisible()
                        .should.eventually.be.equal(
                            true,
                            'Блок с графиком работы ПВЗ должен быть отображён'
                        );

                    await this.returnMapOutletInfo.isAddressNoteVisible()
                        .should.eventually.be.equal(
                            true,
                            'Блок "Как добраться" должен быть отображён'
                        );

                    await this.returnMapOutletInfo.areContactsVisible()
                        .should.eventually.be.equal(
                            true,
                            'Блок с контактами ПВЗ должен быть отображён'
                        );

                    return this.returnMapOutletInfo.isNextStepButtonVisible()
                        .should.eventually.be.equal(
                            true,
                            'Кнопка перехода на следующий шаг должна быть видна'
                        );
                },
            }),
        },
    },
});
