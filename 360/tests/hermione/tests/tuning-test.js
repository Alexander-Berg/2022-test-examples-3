const { NAVIGATION } = require('../config').consts;
const navigation = require('../page-objects/client-navigation');
const tuning = require('../page-objects/client-tuning-page');

const assert = require('chai').assert;
const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');

const TEST_ID_BUY_BUTTON_MAIL360 = '?test-id=369581';

// TODO: убрать скип после того, как обновим хром CHEMODAN-57678
hermione.skip.in('chrome-phone-6.0', 'Проблемы со снятие скринов, убрем скип после CHEMODAN-57678');
describe('Страница оплаты -> ', () => {
    describe('Блоки тарифов', () => {
        /**
         * @param {string} user
         * @returns {Promise<void>}
         */
        const assertViewTuningPageTest = async function(user) {
            const bro = this.browser;

            await bro.yaClientLoginFast(user);
            await bro.url('/tuning');
            await bro.yaWaitForVisible(tuning.common.tuningPage.body());
            await bro.yaResetPointerPosition(); // unhover "More" icon
            await bro.pause(200); // unhover animation
            await bro.assertView(this.testpalmId, 'body');
        };

        it('diskclient-4633, 4874: Блоки тарифов (бесплатный юзер без плюса)', async function() {
            this.testpalmId = await this.browser.yaIsMobile() ? 'diskclient-4633' : 'diskclient-4874';
            await assertViewTuningPageTest.call(this, 'yndx-ufo-test-01');
        });

        it('diskclient-1131, 4424: Смоук: assertView: отображение страницы оплаты (платный юзер)', async function() {
            this.testpalmId = await this.browser.yaIsMobile() ? 'diskclient-1131' : 'diskclient-4424';
            await assertViewTuningPageTest.call(this, 'yndx-ufo-test-oligarh');
        });

        it('diskclient-4880, 4879: Блоки тарифов (бесплатный юзер из другой локали)', async function() {
            this.testpalmId = await this.browser.yaIsMobile() ? 'diskclient-4880' : 'diskclient-4879';
            await assertViewTuningPageTest.call(this, 'fromAnotherLocale');
        });

        it('diskclient-1957, 1952: assertView: Блоки тарифов (юзер со скидкой 10%)', async function() {
            this.testpalmId = await this.browser.yaIsMobile() ? 'diskclient-1957' : 'diskclient-1952';
            await assertViewTuningPageTest.call(this, 'yndx-ufo-test-47');
        });

        it('diskclient-1958, 1955: assertView: Блоки тарифов (юзер со скидкой 20%)', async function() {
            this.testpalmId = await this.browser.yaIsMobile() ? 'diskclient-1958' : 'diskclient-1955';
            await assertViewTuningPageTest.call(this, 'yndx-ufo-test-48');
        });

        it('diskclient-1959, 1956: assertView: Блоки тарифов (юзер со скидкой 30%)', async function() {
            this.testpalmId = await this.browser.yaIsMobile() ? 'diskclient-1959' : 'diskclient-1956';
            await assertViewTuningPageTest.call(this, 'yndx-ufo-test-49');
        });

        // hermione.only.in(clientDesktopBrowsersList);
        hermione.skip.in(['chrome-desktop', 'chrome-phone'], 'https://st.yandex-team.ru/CHEMODAN-84276'); // мигает
        it('diskclient-5449: Выделение блока тарифа при наведении мыши', async function() {
            const bro = this.browser;
            await bro.yaClientLoginFast('yndx-ufo-test-267');

            await bro.url('/tuning');

            await bro.yaWaitForVisible(tuning.common.tuningPage.tariffContainer.tariffWrapper());
            await bro.moveToObject(tuning.common.tuningPage.tariffSubmitButton());

            await bro.pause(1000);
            await bro.assertView('diskclient-5449', tuning.common.tuningPage.tariffContainer.tariffWrapper());
        });

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-5619: Страница оплаты (диск для бизнеса)', async function() {
            const bro = this.browser;
            this.testpalmId = 'diskclient-5619';

            const testdata = {
                user: 'yndx-ufo-test-547',
                tuningUrl: 'tuning',
                businessUrl: 'business.yandex.ru/disk'
            };

            await bro.yaClientLoginFast(testdata.user);

            await bro.url(testdata.tuningUrl);

            await bro.yaClick(tuning.common.tuningPageDiskForBusinessButton());
        });

        hermione.only.in(clientDesktopBrowsersList);
        it('diskclient-6315: Страница оплаты (нет перехода в диск для бизнеса для не RU)', async function() {
            const bro = this.browser;
            this.testpalmId = 'diskclient-5619';

            const testdata = {
                user: 'yndx-ufo-test-548',
                tuningUrl: 'tuning'
            };

            await bro.yaClientLoginFast(testdata.user);

            await bro.url(testdata.tuningUrl);

            const currentLaungage = (await bro.execute(() => {
                return ns.Model.get('userCurrent').getData().locale;
            }));

            assert.notEqual(currentLaungage, 'ru', 'В тесте должен быть выбран любой язык кроме русского');
            await bro.yaWaitForHidden(tuning.common.tuningPageDiskForBusinessButton());
        });
    });

    describe('Отображение элементов', () => {
        it('diskclient-1137, 1077: assertView: Кнопка активации промокода', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            const testpalmId = isMobile ? 'diskclient-1137' : 'diskclient-1077';
            this.testpalmId = testpalmId;

            await bro.getReady('yndx-ufo-test-133', '/tuning', tuning.common.tuningPage.body());
            await bro.click(tuning.common.tuningPage.promoCodeActivationButton());
            await bro.yaWaitForVisible(tuning.common.modalContent());
            await bro.pause(500); // ждём завершения анимации инпута
            await bro.assertView(testpalmId, tuning.common.modalContent());
        });
    });

    hermione.skip.in(clientDesktopBrowsersList, 'Скролл к тарифу актуален только на тачах');
    describe('diskclient-scroll-to-tariff: скролл к тарифу по позиции', () => {
        /**
         * @param {string} id
         * @param {number} position
         * @returns {Promise<void>}
         */
        const tuningScrollTest = async function(id, position) {
            const bro = this.browser;
            await bro.yaClientLoginFast('yndx-ufo-test-70');
            await bro.url('/tuning' + (position ? `?position=${position}` : ''));
            await bro.yaWaitForVisible(tuning.common.tuningPage.tariffContainer());
            await bro.yaAssertView(id, tuning.common.tuningPage.tariffContainer());
        };

        it('diskclient-5117: assertView: Скролл к первому тарифу', async function() {
            await tuningScrollTest.call(this, 'diskclient-5117', 1);
        });
        it('diskclient-5118: assertView: Скролл ко второму тарифу (по умолчанию)', async function() {
            await tuningScrollTest.call(this, 'diskclient-5118');
        });
        it('diskclient-5119: assertView: Скролл к последнему тарифу', async function() {
            await tuningScrollTest.call(this, 'diskclient-5119', -1);
        });
    });

    describe('Переходы', () => {
        it('diskclient-3587, 3572: assertView: Переход к активации промокода по прямой ссылке', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            const testpalmId = isMobile ? 'diskclient-3587' : 'diskclient-3572';
            this.testpalmId = testpalmId;

            await bro.yaClientLoginFast('yndx-ufo-test-152');
            await bro.url('/tuning/gift');
            await bro.yaWaitForVisible(tuning.common.modalContent());
            await bro.pause(500); // ждём завершения анимации инпута
            await bro.assertView(testpalmId, tuning.common.modalContent());
        });

        it('diskclient-1130, 1508: Переход на страницу оплаты по кнопке "Купить место"', async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? 'diskclient-1130' : 'diskclient-1508';

            await bro.yaClientLoginFast('yndx-ufo-test-133');
            const from = isMobile ? 'disk_touch_buybtn' : 'disk_buybtn';
            await bro.yaClickAndAssertNewTabUrl(
                isMobile ? navigation.touch.navigationItemMail360() : navigation.desktop.navigationItemTuning(),
                { linkShouldContain: `https://mail360.yandex.ru/premium-plans?from=${from}` }
            );
        });
    });

    describe('Повторный ввод использованного промокода', () => {
        it('diskclient-3596, 3582: assertView: Повторный ввод промокода на выдачу места', async function() {
            const bro = this.browser;
            this.testpalmId = await bro.yaIsMobile() ? 'diskclient-3596' : 'diskclient-3582';

            await bro.yaClientLoginFast('yndx-ufo-test-133');
            await bro.url('/tuning/gift');
            await bro.yaWaitForVisible(tuning.common.modalContent());
            await bro.yaSetValue(tuning.common.modalContent.input(), 'RGRMX5PC3W');
            await bro.click(tuning.common.modalContent.activateButton());
            await bro.yaWaitForVisible(tuning.common.modalContent.promoError());
            await bro.assertView(this.testpalmId, tuning.common.modalContent());
        });
    });

    /**
     * @returns {Promise<void>}
     */
    async function openFrame() {
        const bro = this.browser;
        await bro.click(tuning.common.tuningPage.tariffSubmitButton());
        await bro.yaWaitForVisible(tuning.common.paymentDialog());
        await bro.yaWaitForHidden(tuning.common.spin());
        await bro.yaWaitForVisible(tuning.common.iframe());

        const iframe = await bro.element(tuning.common.iframe());
        await bro.frame(iframe);
        await bro.yaWaitForVisible(tuning.common.cardFormTariff());
    }

    describe('Попапы', () => {
        hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-83870');
        it('diskclient-6130, 703: Открытие попапа покупки места', async function() {
            const bro = this.browser;

            await bro.getReady('yndx-ufo-test-133', '/tuning', tuning.common.tuningPage.body());
            await openFrame.call(this);
            return await bro.frameParent();
        });

        hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-83870');
        it('diskclient-6166, 5630: Открытие фрейма оплаты юзером с другой локали', async function() {
            const bro = this.browser;
            const expectedText = 'Подписка 100 ГБ на год';

            await bro.yaClientLoginFast('fromAnotherLocale', 'com');
            await bro.yaOpenSection('tuning');
            await openFrame.call(this);
            const text = await bro.getText(tuning.common.cardFormTariff());
            await assert.include(text, expectedText);
            return await bro.frameParent();
        });
    });
});

const tuningData = [
    {
        url: '/payment/1tb_1m',
        expectedText: 'Подписка 1 ТБ на месяц',
        touchId: '4437',
        desktopId: '4435'
    },
    {
        url: '/payment/1tb_1y',
        expectedText: 'Подписка 1 ТБ на год',
        touchId: '4438',
        desktopId: '4434'
    },
    {
        url: '/payment/100gb_1m',
        expectedText: 'Подписка 100 ГБ на месяц',
        touchId: '4381',
        desktopId: '4433'
    },
    {
        url: '/payment/100gb_1y',
        expectedText: 'Подписка 100 ГБ на год',
        touchId: '4436',
        desktopId: '4375'
    }
];
const tuningWithDiscountData = module.exports = [
    {
        url: '/payment/1tb_1m',
        discount: '10%',
        user: 'yndx-ufo-test-47',
        touchId: '4376',
        desktopId: '1326'
    },
    {
        url: '/payment/1tb_1y',
        discount: '20%',
        user: 'yndx-ufo-test-48',
        touchId: '4377',
        desktopId: '4373'
    },
    {
        url: '/payment/100gb_1m',
        discount: '30%',
        user: 'yndx-ufo-test-49',
        touchId: '4378',
        desktopId: '4371'
    },
    {
        url: '/payment/100gb_1y',
        discount: '20%',
        user: 'yndx-ufo-test-48',
        touchId: '4379',
        desktopId: '4372'
    },
    {
        url: '/payment/sadsadad143-4!@#',
        discount: '30%',
        user: 'yndx-ufo-test-49',
        touchId: '4380',
        desktopId: '4374'
    }
];
const promoCodes = [
    {
        testName: 'Акция закончилась',
        promoCode: '22C824DAPS',
        touchId: '3594',
        desktopId: '3581'
    },
    {
        testName: 'Акция ещё не началась',
        promoCode: 'ZEWG2RUMX2',
        touchId: '3595',
        desktopId: '3586',
        isError: true
    },
    {
        testName: 'Неверный промокод',
        promoCode: 'AUTOTEST_PROMOCODE',
        touchId: '3593',
        desktopId: '3580',
        isError: true
    }
];

const doDiscountTest = ({ url, discount, user, touchId, desktopId }) => {
    describe('Страница оплаты -> ', () => {
        it(`diskclient-${touchId}, ${desktopId}: Прямые ссылки на оплату (юзер со скидкой ${discount}): ${url}`, async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? touchId : desktopId;

            await bro.getReady(user, url, tuning.common.tuningPage.body());
            await bro.yaWaitForHidden(tuning.common.paymentDialog());
            await bro.yaAssertUrlInclude('/tuning');
        });
    });
};
tuningWithDiscountData.forEach(doDiscountTest);

const doTest = ({ url, touchId, desktopId, expectedText }) => {
    describe('Страница оплаты -> ', () => {
        hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-83870');
        it(`diskclient-${touchId}, ${desktopId}: Прямые ссылки на оплату (юзер без скидки): ${url}`, async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? touchId : desktopId;

            await bro.getReady('yndx-ufo-test-133', url, tuning.common.tuningPage.body());
            await bro.yaWaitForVisible(tuning.common.paymentDialog());

            const iframe = await bro.element(tuning.common.iframe());

            await bro.frame(iframe);
            await bro.yaWaitForVisible(tuning.common.cardFormTariff());
            const text = await bro.getText(tuning.common.cardFormTariff());
            await assert.include(text, expectedText);

            return await bro.frameParent();
        });
    });
};
tuningData.forEach(doTest);

const doPromoCodesTest = ({ testName, promoCode, touchId, desktopId, isError }) => {
    describe('Страница оплаты -> ', () => {
        it(`diskclient-${touchId}, ${desktopId}: assertView: Промокоды. ${testName}`, async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            const testpalmId = isMobile ? 'diskclient-3594' : 'diskclient-3581';
            this.testpalmId = testpalmId;

            const randomId = Math.floor(Math.random() * 99 + 1);
            const userId = randomId < 10 ? '0' + randomId : randomId;
            await bro.getReady('yndx-ufo-test-' + userId, '/tuning', tuning.common.tuningPage.body());
            await bro.click(tuning.common.tuningPage.promoCodeActivationButton());
            await bro.yaWaitForVisible(tuning.common.modalContent());
            await bro.yaSetValue(tuning.common.modalContent.input(), promoCode);
            await bro.click(tuning.common.modalContent.activateButton());

            await bro.yaWaitForVisible(isError ?
                tuning.common.modalContent.promoError() : tuning.common.modalContent.finishedDescription());
            await bro.assertView(testpalmId, tuning.common.modalContent());
        });
    });
};
promoCodes.forEach(doPromoCodesTest);

describe('Блок с кнопкой покупки -> ', () => {
    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-6423, 6424: Отображение блока Почты 360, Редирект на страницу Почты 360 из блока покупки', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-6423';

        await bro.yaClientLoginFast('yndx-ufo-test-772');
        await bro.url(NAVIGATION.disk.url + TEST_ID_BUY_BUTTON_MAIL360);

        await bro.assertView(this.testpalmId, navigation.desktop.spaceInfoSection(), {
            hideElements: navigation.desktop.spaceInfoSection.infoSpaceButton.infoSpaceButtonTextWrapper()
        });

        await bro.yaClickAndAssertNewTabUrl(
            navigation.desktop.spaceInfoSection.infoSpaceButton(),
            { linkShouldContain: '/mail360.yandex' }
        );
    });
});
