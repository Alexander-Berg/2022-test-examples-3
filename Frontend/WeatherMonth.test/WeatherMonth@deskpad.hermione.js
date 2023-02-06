'use strict';

const openSerp = require('./WeatherMonth@common.hermione-helper').openSerp;
const commonWeatherMonthNewTests = require('./WeatherMonth@common.hermione-helper').commonSpecs;

hermione.also.in(['firefox', 'ipad']);
commonWeatherMonthNewTests();

specs({
    feature: 'Колдунщик погоды по месяцам',
}, function() {
    describe('Скролл в карусели', function() {
        beforeEach(async function() {
            await openSerp.call(this);
        });

        it('Вправо', async function() {
            await this.browser.click('.scroller__arrow_direction_right');
            await this.browser.moveToObject('.serp-header');
            await this.browser.assertView('plain', ['.scroller']);
        });

        it('Влево', async function() {
            await openSerp.call(this);
            await this.browser.click('.scroller__arrow_direction_right');

            await this.browser.execute(function() {
                let scrollerWrap = window.$('.scroller__wrap');
                scrollerWrap.scrollLeft(700);
            });

            await this.browser.click('.scroller__arrow_direction_left');
            await this.browser.moveToObject('.serp-header');
            await this.browser.assertView('plain', ['.scroller']);
        });
    });
});
