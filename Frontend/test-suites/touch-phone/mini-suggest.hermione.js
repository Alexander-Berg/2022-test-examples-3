'use strict';

const URL = require('url').URL;
const _ = require('lodash');
const PO = require('../../page-objects/touch-phone/index').PO;

hermione.only.notIn('searchapp', 'фича не актуальна в searchapp');
specs('Пословный саджест', function() {
    const query = 'аэрофлот';
    const counterPattern = '/clck/jclck/dtype=stred/pid=0/cid=2873/path=granny-touch';
    const suggestPattern = 'suggest';

    const suggest = {};

    beforeEach(function() {
        return this.browser
            .yaOpenSerp({ text: query })
            .click(PO.search.input())
            .waitForVisible(PO.miniSuggestPopup());
    });

    describe('Типы подсказок', function() {
        it('Пословные подсказки', function() {
            return this.browser
                .getText(PO.miniSuggestPopup.itemTpah()).then(text => {
                    suggest.itemText = text;

                    return this.browser
                        .click(PO.miniSuggestPopup.itemTpah());
                })
                .yaWaitUntil('Содержимое инпута не обновилось', () => this.browser
                    .getValue(PO.search.input())
                    .then(val => query + ' ' + suggest.itemText === val.trim())
                )
                .yaWaitUntil('Элементы должны обновиться', () => this.browser
                    .getText(PO.miniSuggestPopup.itemTpah())
                    .then(results => results && (results !== suggest.itemText))
                );
        });

        it('Навигационная подсказка', function() {
            return this.browser
                .yaWaitForVisible(PO.miniSuggestPopup.itemNav(), 'Навигационная подсказка отсутствует')
                .yaCheckLink(PO.miniSuggestPopup.itemNav())
                .then(url => {
                    assert.equal(url.pathname, '/clck/jsredir', 'Из данных пришла ссылка без jsredir');
                });
        });

        it('Фактовая подсказка', function() {
            return this.browser
                .setValue(PO.search.input(), 'погода в мо')
                .yaWaitForVisible(PO.miniSuggestPopup.itemFact())
                .getText(PO.miniSuggestPopup.itemFact.info())
                .then(text => {
                    suggest.itemFact = text;
                })
                .yaWaitUntilPageReloaded(() => this.browser
                    .click(PO.miniSuggestPopup.itemFact())
                )
                .yaParseUrl()
                .then(url => {
                    assert.equal(url.query.text.trim(), suggest.itemFact, 'Параметр text в url не изменился');
                });
        });

        it('Полнотекстовая подсказка', function() {
            return this.browser
                .yaWaitForVisible(PO.miniSuggestPopup.itemsFulltext(), 'Не появились подсказки с типом type_fulltext')
                .getText(PO.miniSuggestPopup.itemsFulltext())
                .then(text => {
                    suggest.itemFulltext = _.isArray(text) ? text[0] : text;
                })
                .yaWaitUntilPageReloaded(() => this.browser
                    .click(PO.miniSuggestPopup.itemsFulltext())
                )
                .yaParseUrl()
                .then(url => {
                    assert.equal(url.query.text.trim(), suggest.itemFulltext, 'Параметр text в url не изменился');
                });
        });
    });

    describe('Манипуляции', function() {
        it('При перезапросе текст элемента подставляется в url', function() {
            return this.browser
                .getText(PO.miniSuggestPopup.itemTpah())
                .then(text => {
                    suggest.itemText = text;

                    return this.browser
                        .click(PO.miniSuggestPopup.itemTpah());
                })
                .yaWaitUntilPageReloaded(() => this.browser
                    .click(PO.search.button())
                )
                .yaParseUrl()
                .then(url => {
                    assert.equal(
                        url.query.text.trim(), query + ' ' + suggest.itemText,
                        'Параметр text в url не изменился'
                    );
                });
        });

        it('Клик вне блока - саджест должен скрыться', function() {
            return this.browser
                .click(PO.search.button())
                .yaWaitForHidden(PO.miniSuggestPopup())
                .then(assert.isTrue);
        });

        it('Клик на крестик и повторный ввод 1 символа - саджест должен скрыться и появиться', function() {
            return this.browser
                .click(PO.search.clear())
                .yaWaitForHidden(PO.miniSuggestPopup())
                .then(assert.isTrue)
                .keys('а')
                .yaWaitForVisible(PO.miniSuggestPopup())
                .then(assert.isTrue);
        });

        it('Backspace - подсказки обновляются', function() {
            return this.browser
                .getText(PO.miniSuggestPopup())
                .then(text => {
                    suggest.text = text;

                    return this.browser
                        .click(PO.miniSuggestPopup.itemTpah());
                })
                .keys('Back space')
                .waitUntil(() => this.browser
                    .getText(PO.miniSuggestPopup())
                    .then(results => results && (results !== suggest.text))
                );
        });
    });

    describe('Счетчики', function() {
        const getPathKeyValues = (url, keys) => url
            .split('/')
            .map(el => el.split('='))
            .reduce((obj, pair) => {
                if (keys.indexOf(pair[0]) !== -1) {
                    obj[pair[0]] = pair[1];
                }
                return obj;
            }, {});

        const submitAndWaitForReload = function() {
            return this.yaWaitUntilPageReloaded(() =>
                this.click(PO.search.button())
            );
        };

        const changeValueAndWaitForSuggestUpdate = function(browser, newValue) {
            let itemText = '';

            return browser
                .getText(PO.miniSuggestPopup.itemTpah())
                .then(text => {
                    itemText = text;
                })
                .setValue(PO.search.input(), newValue)
                .yaWaitUntil('Ждём обновления саджеста', () => browser
                    .getText(PO.miniSuggestPopup.itemTpah())
                    .then(text => text !== itemText)
                );
        };

        it('Время total_input_time и since_first_change должны быть больше 0', function() {
            return this.browser
                .setValue(PO.search.input(), 'погода ')
                .yaGetMiniSuggestCounterData(counterPattern, { field: 'url' }, submitAndWaitForReload)
                .then(data => {
                    const values = getPathKeyValues(data.value, ['since_first_change', 'total_input_time']);

                    assert.isAtLeast(
                        parseInt(values.total_input_time),
                        0,
                        'счетчик полного времени ввода меньше 0'
                    );
                    assert.isAtLeast(
                        parseInt(values.since_first_change),
                        0,
                        'счетчик времени первого изменения меньше 0'
                    );
                });
        });

        it('При перезапросе проклеивается один и тот же suggest_reqid', function() {
            return this.browser
                .setValue(PO.search.input(), 'погода ')
                .yaGetMiniSuggestCounterData(counterPattern, { field: 'url' }, submitAndWaitForReload)
                .then(data => this.browser
                    .yaParseUrl()
                    .then(url => {
                        const valueCounter = getPathKeyValues(data.value, ['suggest_reqid']).suggest_reqid;
                        const valuesSearch = url.query.suggest_reqid;

                        assert.equal(valueCounter, valuesSearch, 'разные suggest_reqid в запросах счетчика и выдачи');
                    })
                );
        });

        it('Содержит правильный suggest_reqid в запросах к ручке саджеста и в счетчиках', function() {
            let suggestParam;

            return this.browser
                .setValue(PO.search.input(), 'rwus')
                .yaGetMiniSuggestCounterData(
                    'part=rwuss', { field: 'url' }, () => this.browser.keys('s')
                )
                .then(data => {
                    const search = new URL(data.value, 'https://yandex.ru');

                    suggestParam = search.searchParams.get('suggest_reqid');
                })
                .yaGetMiniSuggestCounterData(counterPattern, { field: 'url' }, submitAndWaitForReload)
                .then(data => {
                    const counterParam = getPathKeyValues(data.value, ['suggest_reqid']).suggest_reqid;

                    assert.equal(
                        suggestParam,
                        counterParam,
                        'разные suggest_reqid в запросах и отправленных счетчиках'
                    );
                });
        });

        it('Содержит верное количество запросов/ответов', function() {
            return this.browser
                .setValue(PO.search.input(), 'погода')
                .yaGetMiniSuggestCounterData(
                    suggestPattern,
                    { field: 'url' },
                    () => this.browser.yaKeyPress('BACKSPACE')
                )
                .yaGetMiniSuggestCounterData(
                    suggestPattern,
                    { field: 'url' },
                    () => this.browser.yaKeyPress('BACKSPACE')
                )
                .yaGetMiniSuggestCounterData(counterPattern, { field: 'url' }, submitAndWaitForReload)
                .then(data => this.browser
                    .yaParseUrl()
                    .then(url => {
                        const { rqs, rsp, rndr, cchd } = getPathKeyValues(data.value, ['cchd', 'rsp', 'rqs', 'rndr']);

                        assert.equal(parseInt(rqs), 9, 'неверное значение rqs');
                        assert.equal(parseInt(rsp), 9, 'неверное значение rsp');
                        assert.equal(parseInt(cchd), 2, 'неверное значение cchd');
                        assert.isAtLeast(parseInt(rndr), 3, 'неверное значение rndr');
                        assert.isAtMost(parseInt(rndr), 9, 'неверное значение rndr');
                    })
                );
        });

        it('Содержит верное количество пустых ответов', function() {
            return this.browser
                .setValue(PO.search.input(), 'rwusqsd')
                .yaKeyPress('BACKSPACE')
                .yaKeyPress('BACKSPACE')
                .yaGetMiniSuggestCounterData(counterPattern, { field: 'url' }, submitAndWaitForReload)
                .then(data => this.browser
                    .yaParseUrl()
                    .then(url => {
                        const { ersp } = getPathKeyValues(data.value, ['ersp']);

                        assert.equal(parseInt(ersp), 9, 'неверное значение ersp');
                    })
                );
        });

        it('Содержит количество кликов на саджест', function() {
            return this.browser
                .then(() => changeValueAndWaitForSuggestUpdate(this.browser, 'погода в мо'))
                .click(PO.miniSuggestPopup.itemTpah())
                .click(PO.miniSuggestPopup.itemTpah())
                .click(PO.miniSuggestPopup.itemTpah())
                .yaGetMiniSuggestCounterData(counterPattern, { field: 'url' }, submitAndWaitForReload)
                .then(data => this.browser
                    .yaParseUrl()
                    .then(url => {
                        const { clks } = getPathKeyValues(data.value, ['clks']);

                        assert.equal(parseInt(clks), 3, 'неверное значение clks');
                    })
                );
        });

        it('При удалении и вводе state должен быть равен edit', function() {
            return this.browser
                .then(() => changeValueAndWaitForSuggestUpdate(this.browser, 'погода '))
                .click(PO.miniSuggestPopup.itemTpah())
                .yaKeyPress('BACKSPACE')
                .yaKeyPress('BACKSPACE')
                .yaKeyPress('BACKSPACE')
                .keys(['о', 'д', 'а'])
                .yaGetMiniSuggestCounterData(counterPattern, { field: 'url' }, submitAndWaitForReload)
                .then(data => {
                    assert.match(
                        data.value,
                        /path=granny-touch\.edit\.p0\.nah_not_used\.button_by_mouse/,
                        'счетчик в неправильном состоянии'
                    );
                });
        });
    });
});
