const event = 'input-value-changed';

hermione.only.in('chromeMobile');
describe('events', function() {
    describe(event, function() {
        beforeEach(async function() {
            await this.browser
                .yaOpenExample('serp', 'touch')
                .yaMockSuggest('', require('./mocks-personal/full.json'))
                .click('.mini-suggest__input')
                .waitForVisible('.mini-suggest__popup-content')
                .yaListenMBEMEvent(event);
        });

        it('Не вызывается при открытии саджеста', async function() {
            await this.browser
                .yaAssertMBEMEvent(event, records => assert.strictEqual(records.length, 0));
        });

        it('Вызывается при вводе с клавиатуры', async function() {
            await this.browser
                .keys('Test Test')
                .yaAssertMBEMEvent(event, records => assert.strictEqual(records.length, 9));
        });

        it('Вызывается при клике на нав', async function() {
            const query = 'ya';

            await this.browser
                .keys(query)
                .execute(() => Ya.Search.Suggest.instance._freezeClicks = 0)
                .waitForVisible('.mini-suggest__item:first-child')
                .click('.mini-suggest__item:first-child')
                .waitUntil(async() => (await this.browser.getValue('.mini-suggest__input') !== query))
                .yaAssertMBEMEvent(event, records => assert.strictEqual(records.length, query.length + 1));
        });

        it('Вызывается при клике в крестик', async function() {
            const query = 'ya';

            await this.browser
                .keys(query)
                .execute(() => Ya.Search.Suggest.instance._freezeClicks = 0)
                .click('.mini-suggest__input-clear')
                .waitUntil(async() => await this.browser.getValue('.mini-suggest__input') === '')
                .yaAssertMBEMEvent(event, records => assert.strictEqual(records.length, query.length + 1));
        });
    });
});
