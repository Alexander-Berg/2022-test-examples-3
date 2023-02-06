'use strict';

const PO = require('./OrgSkiLinks.page-object');

specs({
    feature: 'Одна организация',
    type: 'Горнолыжный курорт сценарий туриста',
}, function() {
    describe('Ссылка на телеграм', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                foreverdata: '2557900837',
            }, PO.telegramLink());
        });

        hermione.also.in('iphone-dark');
        it('Внешний вид', async function() {
            await this.browser.assertView('telegram-link', PO.telegramLink());
        });

        it('Имеет правильный href', async function() {
            await this.browser.yaCheckLink2({
                selector: PO.telegramLink(),
                url: {
                    href: 'https://t.me/RosaOnlineBot',
                },
            });
        });
    });

    describe('Ссылка на телеграм с наименованием канала', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                foreverdata: '4163947475',
            }, PO.telegramLink());
        });

        it('Внешний вид', async function() {
            await this.browser.assertView('with-channel-name', PO.telegramLink());
        });
    });
});
