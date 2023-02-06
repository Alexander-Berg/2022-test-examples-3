import {makeCase, makeSuite} from 'ginny';
import {EXPRESS_ADDRESS_ID_COOKIE, EXPRESS_LOCATION_COOKIE} from '@self/root/src/constants/expressService';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {
    setExpressAddress,
    setKurganAddressToList,
    unsetExpressAddress,
    clearAddressList,
} from '@self/root/src/spec/hermione/scenarios/express';
import {moscowAddress} from '@self/root/src/spec/hermione/kadavr-mock/persAddress/address';
import COOKIES from '@self/root/src/constants/cookie';

/**
 * Тесты на блок header2-address
 * @param {PageObject.HeaderHyperlocationAddress} address
 * @param {PageObject.ExpressAddressPopup} expressPopup
 */
export default makeSuite('Регион пользователя в шапке', {
    environment: 'kadavr',
    id: 'marketfront-4984',
    issue: 'MARKETFRONT-51898',
    story: {
        'Стоит валидная кука addressId.': {
            async beforeEach() {
                await this.browser.yaScenario(this, setExpressAddress);
                await this.browser.yaScenario(this, setKurganAddressToList);
                await this.browser.yaOpenPage(PAGE_IDS_COMMON.INDEX, {lr: moscowAddress.regionId});
                if (this.touchRegionPopup) {
                    await this.browser.yaClosePopup(this.touchRegionPopup);
                }
            },
            'В шапке': {
                'выводится адрес': {
                    async test() {
                        await this.address.getRegionText()
                            .should.eventually.to.be.equal('улица Льва Толстого, д. 16', 'Должно быть как в куке');
                    },
                },
            },
            'При нажатии на шапку': {
                'открывается список адресов': {
                    async test() {
                        await this.browser.yaWaitForPageReady();

                        const initAddressIdCookie = await this.browser.getCookie(EXPRESS_ADDRESS_ID_COOKIE)
                            .then(({value}) => value);

                        const initAddressCookie = await this.browser.getCookie(EXPRESS_LOCATION_COOKIE)
                            .then(({value}) => value);

                        await this.address.regionClick();
                        await this.expressPopup.waitForAddressesListVisible();
                        const items = await this.expressPopup.getAddressItems();

                        items.length
                            .should.equal(1, 'В списке адресов ровно один адрес');

                        await this.expressPopup.isChoiceButtonDisabled()
                            .should.eventually.to.be.equal(true, 'Кнопка не активна');

                        await this.expressPopup.selectFirstAddress();

                        await this.expressPopup.isChoiceButtonDisabled()
                            .should.eventually.to.be.equal(false, 'Кнопка активна');

                        await this.browser.yaWaitForPageReloadedExtended(
                            () => this.expressPopup.clickChoiceButton()
                        );

                        await this.browser.getCookie(EXPRESS_ADDRESS_ID_COOKIE).then(({value}) => value)
                            .should.eventually.to.not.be.equal(initAddressIdCookie, 'Кука addressId изменилась');

                        await this.browser.getCookie(EXPRESS_LOCATION_COOKIE).then(({value}) => value)
                            .should.eventually.to.not.be.equal(
                                initAddressCookie, 'Кука express-address изменилась'
                            );

                        await this.browser.getCookie('lr').then(({value}) => value)
                            .should.eventually.to.be.equal('53', 'Кука lr (regionId) изменилась');
                    },
                },
            },
            'При изменении региона': {
                'У пользователя сбрасывается гиперлокальный адрес': {
                    id: 'marketfront-4985',
                    issue: 'MARKETFRONT-51900',
                    async test() {
                        await this.browser.yaWaitForPageReady();

                        await this.browser.yaOpenPage(PAGE_IDS_COMMON.INDEX, {lr: 53});
                        if (this.touchRegionPopup) {
                            await this.browser.yaClosePopup(this.touchRegionPopup);
                        }
                        await this.browser.getCookie('lr').then(({value}) => value)
                            .should.eventually.to.be.equal('53', 'Кука lr (regionId) изменилась');

                        await this.address.getRegionText()
                            .should.eventually.to.not.be.equal('улица Льва Толстого, д. 16', 'Только регион');
                    },
                },
            },
        },
        'Нет кук addressId, express_address': {
            'У пользователя нет доступных адресов в persAddress': {
                async beforeEach() {
                    await this.browser.yaScenario(this, unsetExpressAddress);
                    await this.browser.yaScenario(this, clearAddressList);
                    if (this.touchRegionPopup) {
                        await this.browser.yaClosePopup(this.touchRegionPopup);
                    }
                },
                'На морде кликнуть на виджет адреса.': {
                    async beforeEach() {
                        await this.address.regionClick();
                        await this.addressMapEdit.isVisible()
                            .should.eventually.be.equal(true, 'Открывается попап-карта выбора адреса');
                    },
                    'Указать любой адрес в гео-саджесте.': makeCase({
                        id: 'marketfront-4983',
                        issue: 'MARKETFRONT-51897',
                        async test() {
                            const newAddress = 'Рочдельская улица, 20';

                            await this.addressSuggest.setTextAndSelect(newAddress, false);

                            await this.browser.waitUntil(
                                async () => {
                                    const suggestFullySetText = await this.addressSuggest.getText();
                                    return newAddress !== suggestFullySetText;
                                },
                                5000,
                                'Саджест подтянул полное значение адреса.'
                            );
                            await this.browser.yaWaitForPageReloadedExtended(
                                () => this.addressMapEdit.accept()
                            );
                            const currentCoookies = await this.browser.getCookie().then(
                                cookies => cookies.map(({name}) => name)
                            );

                            await this.expect(currentCoookies).to
                                .include(COOKIES.SETTINGS, 'Выставлена кука пользовательских настроек');

                            return this.expect(currentCoookies).to.include.members(
                                [EXPRESS_ADDRESS_ID_COOKIE, EXPRESS_LOCATION_COOKIE], 'Express куки установлены.'
                            );
                        },
                    }),
                    'Указать любой адрес на карте.': makeCase({
                        id: 'marketfront-4983',
                        issue: 'MARKETFRONT-51897',
                        async test() {
                            const suggestInitial = await this.addressSuggest.getText();
                            await this.addressPinMap.waitForVisible();
                            await this.addressPinMap.waitForReady();
                            await this.addressPinMap.setCenter([55.75161179263512, 37.585118104061046], 17);
                            await this.browser.waitUntil(
                                async () => {
                                    const suggestFullySetText = await this.addressSuggest.getText();
                                    return suggestInitial !== suggestFullySetText;
                                },
                                5000,
                                'Саджест подтянул полное значение адреса.'
                            );
                            await this.browser.yaWaitForPageReloadedExtended(
                                () => this.addressMapEdit.accept()
                            );
                            const currentCoookies = await this.browser.getCookie().then(
                                cookies => cookies.map(({name}) => name)
                            );
                            return this.expect(currentCoookies).to.include.members(
                                [EXPRESS_ADDRESS_ID_COOKIE, EXPRESS_LOCATION_COOKIE], 'Express куки установлены.'
                            );
                        },
                    }),
                },
            },
        },
    },
});

