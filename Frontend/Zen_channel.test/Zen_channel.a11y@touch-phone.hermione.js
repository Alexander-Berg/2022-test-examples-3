'use strict';

const PO = require('./Zen_channel.page-object');

hermione.only.in('chrome-phone', 'Достаточно проверить атрибуты только на одной платформе');
specs({
    feature: 'Колдунщик Дзена',
    type: 'Доступность',
}, function() {
    it('Доступность', async function() {
        const link = PO.zenChannel.zenHeader.link();
        const thumb = PO.zenChannel.zenHeader.thumb();

        await this.browser
            .yaOpenSerp({
                foreverdata: '2053611378',
                text: 'foreverdata',
                data_filter: 'games',
            }, PO.zenChannel());

        assert.equal(
            await this.browser.getAttribute(thumb, 'aria-hidden'),
            'true',
            `Элемент с картинкой ${thumb} должен быть скрыт от a11y`,
        );

        assert.equal(
            await this.browser.getAttribute(link, 'aria-label'),
            'KP.RU:Комсомольская правда в Дзене 635,6 тыс. подписчиков',
            `Элемент с селектором ${link} должен иметь aria-label: KP.RU:Комсомольская правда в Дзене 635,6 тыс. подписчиков'`,
        );
    });
});
