async function assertCount(value, sum) {
    await this.setValue('.CartHead-SelectInput[data-index="0"]', value);

    // На всякий случай нажимаем энтер, чтобы значение применилось.
    await this.keys('Enter');
    // Чтобы chrome-phone и searchapp увидели изменение текущего инпута переводим фокус в другой инпут.
    await this.yaScrollPage('.CartHead-SelectInput[data-index="1"]');
    await this.click('.CartHead-SelectInput[data-index="1"]');
    // Немного ждём, когда браузер среагирует на установку фокуса.
    await this.pause(300);

    // Скролим страницу в самый верх, иначе браузер иногда подскроливает к сфокусированному полю,
    // а вебдрайвер не видит, что текст в бейджике изменился.
    await this.yaScrollPage(0);

    await this.yaWaitUntil(
        `Не появилось количество товаров «${sum}» на бейджике иконки`,
        async() => await this.getText('.CartIcon-Icon .Badge-Dot') === sum,
        2000,
    );
    await this.assertView(`length-${sum.length}`, '.Cover-Wrapper', {
        ignoreElements: ['.Cover-NavLink'],
    });
}

describe('CartIcon', function() {
    it('Внешний вид', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'cart',
            expFlags: {
                turboforms_endpoint: '/multiple/',
            },
        });

        await browser.yaWaitForVisible('.CartHead-CartItems', 2000, 'Не появились товары в корзине');
        await browser.yaIndexify('.CartHead-SelectInput');

        await assertCount.call(browser, 20, '26');
        await assertCount.call(browser, 333, '339');
        await assertCount.call(browser, 999, '999+');
    });
});
