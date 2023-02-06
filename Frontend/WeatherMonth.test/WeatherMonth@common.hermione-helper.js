'use strict';

const PO = require('../WeatherMonth.page-object');

const openSerp = function(foreverdata = 2556522887, text = 'foreverdata') {
    return this.browser.yaOpenSerp({
        text: text,
        foreverdata: foreverdata,
        exp_flags: 'weather_month_new=1',
        data_filter: 'weather',
    }, PO.weatherMonth());
};

module.exports.commonSpecs = function commonSpecs() {
    specs('Колдунщик погоды по месяцам', () => {
        [
            {
                name: 'Погода в екатеринбурге в мае',
                text: 'foreverdata',
                screenshot: 'weather-month-ekb-may',
                foreverdata: 2556522887,
            },
            {
                name: 'Погода в афинах в мае',
                text: 'foreverdata',
                screenshot: 'weather-month-athens-may',
                foreverdata: 4061624924,
            },
        ].forEach(item => {
            hermione.also.in('iphone-dark');
            it(item.name, async function() {
                await openSerp.call(this, item.foreverdata, item.text);
                await this.browser.assertView(item.screenshot, PO.weatherMonth());
            });
        });
    });
};

module.exports.openSerp = openSerp;
