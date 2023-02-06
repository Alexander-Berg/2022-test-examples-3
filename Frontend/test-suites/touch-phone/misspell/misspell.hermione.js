'use strict';

const url = require('url');
const PO = require('../../../page-objects/touch-phone/index').PO;

specs('Опечаточник', function() {
    [
        {
            title: 'Задан пустой поисковый запрос',
            source: ''
        },
        {
            title: 'Синтаксическая ошибка',
            source: '№:?;'
        },
        {
            title: 'Ничего не найдено',
            source: 'aaaaaaggggggpppppp'
        }
    ].forEach(test => {
        hermione.only.notIn('searchapp', 'В ПП нет шапки');
        it(test.title, function() {
            return this.browser
                .yaOpenSerp('text=' + test.source)
                .yaWaitForVisible(PO.misspell(), 'Не появился опечаточник')
                .getValue(PO.search.input()).then(text => {
                    assert.equal(text, test.source, 'Поисковая строка должна сохраниться');
                });
        });
    });

    [
        {
            title: 'Поиск с кавычками',
            source: 'foreverdata',
            foreverdata: '4284258006',
            noreask: true,
            baobab: '/$page/$main/misspell/type_reask/unquote'
        },
        {
            title: 'Исключены результаты в одной фразе',
            source: 'москва ~ париж',
            correct: 'москва париж',
            baobab: '/$page/$main/misspell/type_minuswords/all'
        },
        {
            title: 'Исключено слово',
            source: 'серфинг -виндсерфинг',
            correct: 'серфинг виндсерфинг',
            baobab: '/$page/$main/misspell/type_minuswords/all'
        },
        {
            title: 'Восстановление раскладки клавиатуры',
            source: 'ktnj',
            correct: 'лето',
            noreask: true,
            baobab: '/$page/$main/misspell/type_reask/keyboard_layout'
        },
        {
            title: 'Быть может вы искали',
            source: 'шипито',
            correct: 'шапито',
            baobab: '/$page/$main/misspell/type_misspell_source/title'
        },
        {
            title: 'Исправлена опечатка',
            source: 'клбаса',
            correct: 'колбаса',
            noreask: true,
            baobab: '/$page/$main/misspell/type_reask/"tech/correct"'
        }
    ].forEach(test => {
        hermione.only.notIn('searchapp', 'В ПП нет шапки');
        it(test.title, function() {
            return this.browser
                .yaOpenSerp('text=' + test.source + (test.foreverdata ? '&foreverdata=' + test.foreverdata : ''))
                .yaWaitForVisible(PO.misspell(), 'Не появился опечаточник')
                .getValue(PO.search.input()).then(text => {
                    // если запрос автоматически корректируется
                    if (test.noreask && test.correct) {
                        assert.equal(text, test.correct, 'Поисковая строка не содержит исправленного текста');
                    }
                })
                .yaCheckLink(PO.misspell.buttonLink(), { target: '' }).then(location => {
                    const parsed = url.parse(location, true);

                    if (test.noreask) {
                        assert.equal(parsed.query.text, test.source);
                        assert.equal(parsed.query.noreask, 1);
                    } else {
                        assert.equal(parsed.query.text, test.correct);
                    }
                })
                .yaCheckBaobabCounter(PO.misspell.buttonLink(), { path: test.baobab });
        });
    });

    it('Добавлены похожие запросы', function() {
        return this.browser
            .yaOpenSerp('text=отель пхень янь')
            .yaWaitForVisible(PO.misspell(), 'Не появился опечаточник')
            .yaCheckLink(PO.misspell.firstLink(), { target: '' }).then(location => {
                const parsed = url.parse(location, true);

                assert.equal(parsed.query.text, 'отель пхеньян');
            })
            .yaMockExternalUrl(PO.misspell.firstLink())
            .yaCheckBaobabCounter(PO.misspell.firstLink(), {
                path: '/$page/$main/misspell/type_web_misspell/correct'
            })
            .yaCheckLink(PO.misspell.secondLink(), { target: '' }).then(location => {
                const parsed = url.parse(location, true);

                assert.equal(parsed.query.text, 'отель пхень янь');
                assert.equal(parsed.query.noreask, 1);
            })
            .yaCheckBaobabCounter(PO.misspell.secondLink(), {
                path: '/$page/$main/misspell/type_web_misspell/source'
            });
    });
});
