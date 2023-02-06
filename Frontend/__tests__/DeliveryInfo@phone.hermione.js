describe('Информация о доставке', function() {
    it('Разворачивание и скрытие текста', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            url: '/turbo/spideradio.github.io/s/',
            query: {
                product_id: 292,
                pcgi: 'rnd=fnu7e56jbff',
            },
        });

        await browser.yaWaitForVisible('.DeliveryInfo');
        await browser.yaScrollPage('.DeliveryInfo', 0);
        await browser.assertView('collapsed', '.DeliveryInfo');

        await browser.click('.DeliveryInfo .ReadMore-Link');
        await browser.yaWaitForVisible('.ReadMore_expanded');
        await browser.assertView('expanded', '.DeliveryInfo');
    });

    it('Два варианта доставки', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            url: '/turbo/spideradio.github.io/s/',
            query: {
                product_id: 292,
                pcgi: 'rnd=fnu7e56jbff',
            },
        });

        await browser.yaWaitForVisible('.DeliveryInfo');
        const { value } = await browser.execute(function() {
            const list = document.querySelector('.DeliveryInfo .ReadMore:first-child');
            const optionsCount = list.querySelectorAll('.DeliveryInfo-ListItem').length;
            const link = list.querySelector('.ReadMore-Link');
            return optionsCount === 2 && link.innerText === 'Ещё одна опция';
        });

        assert(value, 'Не корректный текст кнопки раскрытия списка');
    });

    it('Один вариант доставки', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            url: '/turbo/spideradio.github.io/s/',
            query: {
                product_id: 292,
                pcgi: 'rnd=fnu7e56jbff',
            },
        });

        await browser.yaWaitForVisible('.DeliveryInfo');
        const { value } = await browser.execute(function() {
            const list = document.querySelector('.DeliveryInfo .ReadMore:last-child');
            const optionsCount = list.querySelectorAll('.DeliveryInfo-ListItem').length;
            const link = list.querySelector('.ReadMore-Link');
            return optionsCount === 1 && !link;
        });

        assert(value, 'Не должно быть кнопки раскрытия списка если есть всего 1 элемент');
    });

    it('Отображение текста кнопки раскрытия списка', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            url: '/turbo/gipfel.ru/s/catalog/0288/',
            query: { product_id: 40249 },
        });

        await browser.yaWaitForVisible('.DeliveryInfo');

        const { value: firstListValue } = await browser.execute(function() {
            const list = document.querySelector('.DeliveryInfo .ReadMore:first-child');
            const optionsCount = list.querySelectorAll('.DeliveryInfo-ListItem').length;
            const link = list.querySelector('.ReadMore-Link');
            return optionsCount === 3 && link.innerText === 'Ещё 2 опции';
        });

        assert(firstListValue, 'Не корректный текст кнопки раскрытия списка доставки курьером');

        const { value: secondListValue } = await browser.execute(function() {
            const list = document.querySelector('.DeliveryInfo .ReadMore:last-child');
            const optionsCount = list.querySelectorAll('.DeliveryInfo-ListItem').length;
            const link = list.querySelector('.ReadMore-Link');
            return optionsCount === 2 && link.innerText === 'Ещё одна опция';
        });

        assert(secondListValue, 'Не корректный текст кнопки раскрытия списка самовывоза');
    });

    it('Отображение длинного текста', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            url: '/turbo/spideradio.github.io/s/',
            query: {
                product_id: 292,
                pcgi: 'rnd=fnu7e56jbff',
                patch: 'setLongDeliveryTitle',
            },
        });

        await browser.yaWaitForVisible('.DeliveryInfo');
        await browser.yaScrollPage('.DeliveryInfo .ReadMore:first-child', .5);
        await browser.assertView('collapsed', '.DeliveryInfo .ReadMore:first-child');

        await browser.click('.DeliveryInfo .ReadMore:first-child .ReadMore-Link');
        await browser.yaWaitForVisible('.ReadMore_expanded');
        await browser.yaScrollPage('.DeliveryInfo .ReadMore:first-child', .1);
        await browser.assertView('expanded', '.DeliveryInfo .ReadMore:first-child');
    });
});
