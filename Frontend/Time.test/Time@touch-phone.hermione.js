'use strict';

specs('Колдунщик времени', function() {
    [
        { type: 'city', message: 'Время в конкретном городе', text: 'сколько времени в новосибирске' },
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

            await this.browser.yaOpenSerp({ text: item.text, data_filter: false }, PO.times());
            await this.browser.assertView(item.type, PO.times());
        });
    });
});
