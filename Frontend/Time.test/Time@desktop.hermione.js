'use strict';

specs('Колдунщик времени', function() {
    hermione.also.in('chrome-desktop-dark');
    it('Время в конкретном городе', async function() {
        const PO = this.PO;

        await this.browser.yaOpenSerp(
            { text: 'сколько времени в новосибирске', data_filter: 'time' },
            PO.times(),
        );

        await this.browser.assertView('city', PO.times.factLayout());

        await this.browser.yaCheckBaobabServerCounter({
            path: '/$page/$main/$result[@wizard_name="time"]',
        });

        await this.browser.yaCheckLink2({
            selector: PO.times.link(),
            url: 'https://yandex.ru/time?city1=65&city2=213',
            message: 'Сломана ссылка на сервис времени',
            baobab: {
                path: '/$page/$main/$result/title',
            },
        });
    });

    [
        {
            type: 'negative-diff',
            message: 'Разница во времени - N часов',
            text: 'разница во времени владивосток милан',
        },
        { type: 'greenwich-time', message: 'Время по гринвичу', text: 'время по гринвичу москва' },
        { type: '24hrs-diff', message: 'Разница во времени 24 часа', text: 'разница во времени гонолулу самоа' },
        { type: 'no-diff', message: 'Разницы во времени нет', text: 'разница во времени москва санкт-петербург' },
        {
            type: 'positive-diff',
            message: 'Разница во времени + N часов и M минут',
            text: 'разница во времени между катманду и самоа',
        },
    ].forEach(function(item) {
        it(item.message, async function() {
            const PO = this.PO;

            await this.browser.yaOpenSerp({ text: item.text, data_filter: 'time' }, PO.times());
            await this.browser.assertView(item.type, PO.times.factLayout());
        });
    });
});
