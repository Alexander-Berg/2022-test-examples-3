const event = 'input-value-changed';

describe('events', function() {
    describe(event, function() {
        beforeEach(async function() {
            await this.browser
                .yaOpenExample('serp', 'desktop')
                .yaMockSuggest('', require('./mocks-personal/full.json'))
                .click('.mini-suggest__input')
                .keys('я')
                .keys('Backspace')
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

        it('Вызывается при селекте с клавиатуры', async function() {
            await this.browser
                .keys('ArrowDown')
                .yaAssertMBEMEvent(event, records => assert.strictEqual(records.length, 1));
        });

        it('Вызывается при клике мышкой в айтем', async function() {
            await this.browser
                // Дизейблим переход по ссылке
                .execute(() => Ya.Search.Suggest.instance._doSubmit = () => undefined)
                .click('.mini-suggest__item:first-child')
                .yaAssertMBEMEvent(event, records => assert.strictEqual(records.length, 1));
        });

        it('Вызывается при клике в стрелку айтема', async function() {
            const query = 'ya';

            await this.browser
                .keys(query)
                .waitForExist('.mini-suggest__item_arrow_yes .mini-suggest__arrow')
                .click('.mini-suggest__item_arrow_yes .mini-suggest__arrow')
                .yaAssertMBEMEvent(event, records => assert.strictEqual(records.length, query.length + 1));
        });

        it('Вызывается при клике в крестик', async function() {
            const query = 'ya';

            await this.browser
                .keys(query)
                .execute(() => Ya.Search.Suggest.instance._onClearClick())
                .yaAssertMBEMEvent(event, records => assert.strictEqual(records.length, query.length + 1));
        });
    });
});
