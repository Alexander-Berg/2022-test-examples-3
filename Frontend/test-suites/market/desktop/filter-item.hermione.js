const runOnProject = require('../../../commands/util/runOnProject');

runOnProject(['market'], 'desktop', function() {
    const mock = require('../desktop/mocks/filter.json');
    const text = 'телевиз';

    describe('Фильтр-подсказки', function() {
        beforeEach(function() {
            return this.browser
                .yaMockSuggest(text, mock)
                .click('.mini-suggest__input')
                .keys(text)
                .waitForVisible('.mini-suggest__popup-content');
        });

        it('Внешний вид', function() {
            return this.browser.assertView('plain', '.mini-suggest__item_type_filter');
        });

        it('Навигация стрелками', function() {
            return this.browser
                .keys('ArrowDown')
                .assertView('after-down-selected', ['.mini-suggest', '.mini-suggest__popup-content'])
                .keys('ArrowUp')
                .assertView('zero-selected', ['.mini-suggest', '.mini-suggest__popup-content'])
                .keys('ArrowUp')
                .assertView('last-item-selected', ['.mini-suggest', '.mini-suggest__popup-content']);
        });

        it('Проверка счетчика', async function() {
            const secondItemSelector = '.mini-suggest__filter-item:nth-child(2)';

            // Отключаем переход по ссылке
            await this.browser.execute(() => MBEM.decl('mini-suggest', {
                _followLink: function() {},
            }));

            // Клик вне ссылки не должен отправлять счетчик
            const counterAfterTitleClick = await this.browser.yaGetLastCounter(
                () => this.browser.click('.mini-suggest__filter-title'),
            );

            assert.isNull(counterAfterTitleClick, 'При клике в заголовок фильтра отправился счетчик');

            const text = await this.browser.getText(secondItemSelector);
            const href = await this.browser.getAttribute(`${secondItemSelector} > .mini-suggest__filter-link`, 'href');

            // Клик в ссылку должен отправлять счетчик с определенными параметрами
            await this.browser.yaAssertCounterParams(
                () => this.browser.click(secondItemSelector),
                params => {
                    assert.strictEqual(params.text, text);
                    assert.strictEqual(params.link, href);
                    assert.strictEqual(params.index, '2');
                    assert.match(params.tpah_log, /^\[.*\[phrase,p1,\d+,filter\].*\]$/);
                },
            );

            // Добавленные для фильтров параметры должны быть обнулены при следующей отправке счетчика
            await this.browser.yaAssertCounterParams(
                () => this.browser.click('.mini-suggest__item_type_nav[data-index="7"]'),
                params => {
                    assert.doesNotHaveAnyKeys(params, ['index', 'link']);
                    assert.notStrictEqual(params.text, text);
                },
            );
        });
    });
});
