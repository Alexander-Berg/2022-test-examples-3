'use strict';

const PO = require('./Translate.page-object')('touch-phone');

const LANG_SELECT_ENGLISH = 3;
const LANG_SELECT_ARABIC = 4;
const LANG_SELECT_PORTUGUESE = 63;
const LANG_SELECT_RUSSIAN = 65;
const LANG_SELECT_JAPANESE = 97;

specs({ feature: 'Колдунщик переводов' }, function() {
    // Такой же тест на deskpad, отличается параметром -srv
    describe('Отправка статистики при инициализации', () => {
        [
            {
                message: 'с пустым колдунщиком',
                query: 'перевод',
                empty: '1',
            },
            {
                message: 'с предзаполненным колдунщиком',
                query: 'код перевод',
                empty: '0',
            },
        ].forEach(type => {
            it(type.message, async function() {
                await this.browser.yaOpenSerp({ text: type.query }, PO.translate());

                await this.browser.yaCheckBaobabCounter(() => {}, {
                    path: '/$page/$main/$result/translate-form',
                    event: 'tech',
                    type: 'translate-init',
                    data: { srv: 'yawzrdm-back', empty: type.empty },
                });
            });
        });
    });

    describe('Общие проверки', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({ text: 'перевод с русского на английский онлайн' }, PO.translate());
        });

        hermione.also.in('iphone-dark');
        it('Внешний вид с транскрипцией', async function() {
            await this.browser.yaOpenSerp({
                text: 'Перевод с английского на немецкий',
                data_filter: 'translate',
                srcskip: 'YABS_DISTR',
            }, PO.translate(), 'Не появился колдунщик переводов');

            await this.browser.setValue(PO.translate.textArea.control(), 'loudly');

            await this.browser.execute(function(input) {
                window.$(input).blur();
            }, PO.translate.textArea.control());

            await this.browser.yaWaitForVisible(PO.translate.resultText(), 'Не показался результат перевода');
            await this.browser.yaWaitForVisible(PO.translate.resultText());
            await this.browser.yaWaitForVisible(PO.translate.sourceTranscription());
            await this.browser.yaWaitForVisible(PO.translate.targetTranscription());
            await this.browser.assertView('transcription', PO.translate());
        });

        hermione.also.in('iphone-dark');
        it('Кнопка перезагрузки работает', async function() {
            await this.browser.yaOpenSerp({
                text: 'перевод',
                data_filter: 'translate',
            }, PO.translate(), 'Не появился колдунщик переводов');

            await this.browser.yaMockXHR({
                urlDataMap: {
                    '^https://translate.yandex.net/api/v1/tr.json/translate': {},
                },
            });

            await this.browser.setValue(PO.translate.textArea.control(), 'test');
            await this.browser.yaWaitForVisible(PO.translate.errorMessage());
            await this.browser.assertView('error', PO.translate.errorMessage());

            await this.browser.yaMockXHR({
                urlDataMap: {
                    '^https://translate.yandex.net/api/v1/tr.json/translate': {
                        code: 200,
                        lang: 'en-ru',
                        text: ['тест'],
                    },
                },
            });

            await this.browser.click(PO.translate.refreshButton());
            await this.browser.yaWaitForVisible(PO.translate.resultText(), 'Не появился перевод');
            await this.browser.yaWaitForVisible(PO.translate.copyIcon(), 'Не появилась кнопка копирования');
            await this.browser.yaScroll(PO.translate.resultText());
            await this.browser.assertView('afterError', PO.translate.resultText());
        });

        it('Саджест подставляется', async function() {
            await this.browser.yaOpenSerp({
                text: 'переводчик онлайн с русского на английский',
                data_filter: 'translate',
            }, PO.translate());

            await this.browser.setValue(PO.translate.textArea.control(), 'hel');
            await this.browser.yaWaitForVisible(PO.translate.suggest());

            await this.browser.yaWaitUntil('В первом саджесте help', async function() {
                const value = await this.getText(PO.translate.suggestItem());
                return value === 'help';
            });

            await this.browser.yaTouch(PO.translate.suggestItem());

            await this.browser.yaWaitUntil('Не появился результат нажатия саджеста', async function() {
                const value = await this.getText(PO.translate.textArea.control());
                return value === 'help';
            });

            await this.browser.yaCheckBaobabCounter(() => {}, {
                path: '$page/$main/$result/translate-form/suggest',
                data: { byEnter: false },
                behaviour: { type: 'dynamic' },
            });
        });

        describe('Проверки с контролами', function() {
            beforeEach(async function() {
                await this.browser.setValue(PO.translate.textArea.control(), 'friend');
                await this.browser.yaWaitForVisible(PO.translate.result(), 'Не появился результат с переводом');
            });

            [
                {
                    message: 'с которого',
                    selectors: PO => ({
                        switch: PO.translate.sourceLangSwitch(),
                        switchItem: PO.translate.sourceLangSwitch.firstItemNative(),
                    }),
                    counterToken: 'from',
                },
                {
                    message: 'на который',
                    selectors: PO => ({
                        switch: PO.translate.targetLangSwitch(),
                        switchItem: PO.translate.targetLangSwitch.firstItemNative(),
                    }),
                    counterToken: 'to',
                },
            ].forEach(function(testData) {
                it(`При выборе языка, ${testData.message} переводим, результат с переводом обновляется`, async function() {
                    let oldResult;
                    const selectors = testData.selectors(PO);

                    const text = await this.browser.getHTML(PO.translate.result());
                    oldResult = text;

                    await this.browser.yaCheckBaobabCounter(() => this.browser.yaTouch(selectors.switch), {
                        path: `/$page/$main/$result/translate-form/select-${testData.counterToken}`,
                        behaviour: { type: 'dynamic' },
                    }, `Не сработал динамический счётчик на переключателе языка, ${testData.message} переводим`);

                    await this.browser.yaCheckBaobabCounter(() => this.browser.yaTouch(selectors.switchItem), {
                        path: `/$page/$main/$result/translate-form/select-${testData.counterToken}`,
                        behaviour: { type: 'dynamic' },
                        data: { action: 'change' },
                    }, 'Не сработал динамический счётчик счётчик на выбранном языке');

                    await this.browser.yaWaitUntil('Результат с переводом не обновился', async function() {
                        const text = await this.getHTML(PO.translate.result());
                        return text !== oldResult;
                    });
                });
            });
        });

        describe('Проверки с контролами (без текста)', function() {
            beforeEach(async function() {
                const value1 = await this.browser.getText(PO.translate.sourceLangSwitch.text());
                assert(value1 === 'Русский');
                const value2 = await this.browser.getText(PO.translate.targetLangSwitch.text());
                assert(value2 === 'Английский');
            });

            it('Смена языка текста на язык перевода', async function() {
                await this.browser.yaTouch(PO.translate.sourceLangSwitch());
                await this.browser.selectByIndex(PO.translate.sourceLangSwitch.control(), LANG_SELECT_ENGLISH);

                await this.browser.yaWaitUntil('Язык в первом селекте не изменился', async function() {
                    const value = await this.getText(PO.translate.sourceLangSwitch.text());
                    return value === 'Английский';
                });

                const value = await this.browser.getText(PO.translate.targetLangSwitch.text());
                assert.equal(value, 'Русский', 'Язык во втором селекте не изменился');

                await this.browser.yaCheckBaobabCounter(() => {}, [
                    {
                        path: '/$page/$main/$result/translate-form/select-from',
                    },
                    {
                        event: 'tech',
                        type: 'translate-language2',
                        path: '/$page/$main/$result/translate-form',
                    },
                ]);
            });

            it('Смена языка перевода на язык текста', async function() {
                await this.browser.yaTouch(PO.translate.targetLangSwitch());
                await this.browser.selectByIndex(PO.translate.targetLangSwitch.control(), LANG_SELECT_RUSSIAN);

                await this.browser.yaWaitUntil('Язык во втором селекте не изменился', async function() {
                    const value = await this.getText(PO.translate.targetLangSwitch.text());
                    return value === 'Русский';
                });

                const value = await this.browser.getText(PO.translate.sourceLangSwitch.text());
                assert.equal(value, 'Английский', 'Язык в первом селекте не изменился');

                await this.browser.yaCheckBaobabCounter(() => {}, [
                    {
                        path: '/$page/$main/$result/translate-form/select-to',
                    },
                    {
                        event: 'tech',
                        type: 'translate-language1',
                        path: '/$page/$main/$result/translate-form',
                    },
                ]);
            });

            it('При смене языков в пустом колдунщике языки меняются местами', async function() {
                await this.browser.click(PO.translate.swapLangsButton());

                await this.browser.yaWaitUntil('Язык оригинала не обновился', async function() {
                    const value = await this.getText(PO.translate.sourceLangSwitch.text());
                    return value === 'Английский';
                });

                await this.browser.yaWaitUntil('Язык перевода не обновился', async function() {
                    const value = await this.getText(PO.translate.targetLangSwitch.text());
                    return value === 'Русский';
                });
            });
        });

        it('Фокус в поле ввода после возврата на serp', async function() {
            await this.browser.yaCheckNewTabOpen(PO.seventhSerpItem());
            await this.browser.yaWaitForVisible(PO.translate.textArea.control());
            const cls = await this.browser.yaGetClass(PO.translate.textArea.control());
            assert(cls.indexOf('focused') !== 1);
        });

        hermione.also.in('iphone-dark');
        it('Длинные названия в контролах', async function() {
            const textToDetect = 'hi tha mi ùr agad a charaid airson an eadar-theangachadh';

            await this.browser.setValue(PO.translate.textArea.control(), textToDetect);

            await this.browser.yaWaitUntil('Язык в исходном селекте не определяется', async function() {
                const value = await this.getText(PO.translate.sourceLangSwitch.text());
                return value === 'Шотландский (гэльский) (автоопределение)';
            });

            await this.browser.yaTouch(PO.translate.targetLangSwitch());
            await this.browser.yaTouch(PO.translate.targetLangSwitch.AzerbajaniLangNative());
            const app = await this.browser.getMeta('app');
            if (app === 'searchapp-phone') {
                await this.browser.deviceClickBack();
            }

            // убираем наведение с поля ввода, чтобы не скриншотить рамку
            await this.browser.moveToObject('body', 0, 0);

            await this.browser.assertView('plain', PO.translate.langControls());
        });
    });

    describe('Показ примеров', function() {
        hermione.also.in('iphone-dark');
        it('Если примеры пришли с сервера', async function() {
            await this.browser.yaOpenSerp({
                text: 'foreverdata',
                foreverdata: 3845198953,
                data_filter: 'translate',
            }, PO.translate(), 'Не появился колдунщик переводов');

            await this.browser.yaWaitForVisible(PO.translate.examples());
            await this.browser.assertView('examplesSimple', PO.translate.examples());
        });

        hermione.also.in('iphone-dark');
        it('Если примеры не пришли с сервера', async function() {
            await this.browser.yaOpenSerp({
                text: 'foreverdata',
                foreverdata: 3153665154,
                data_filter: 'translate',
            }, PO.translate(), 'Не появился колдунщик переводов');

            await this.browser.yaWaitForVisible(PO.translate.examples());
            await this.browser.yaScroll(PO.translate.examples());
            await this.browser.assertView('examplesFromServer', PO.translate.examples());
        });

        hermione.also.in('iphone-dark');
        it('Примеры показываются после нового запроса', async function() {
            await this.browser.yaOpenSerp({
                text: 'кот перевод',
                data_filter: 'translate',
            }, PO.translate(), 'Не появился колдунщик переводов');

            await this.browser.yaWaitForVisible(PO.translate.resultText());
            await this.browser.click(PO.translate.clear());
            await this.browser.setValue(PO.translate.textArea.control(), 'привет');
            await this.browser.yaWaitForVisible(PO.translate.examples());
            await this.browser.assertView('examplesAfterClear', PO.translate.examples());
        });

        hermione.also.in('iphone-dark');
        it('Кнопка расхлопа примеров работает', async function() {
            await this.browser.yaOpenSerp({
                text: 'кот перевод',
                data_filter: 'translate',
            }, PO.translate(), 'Не появился колдунщик переводов');

            await this.browser.yaWaitForVisible(PO.translate.examples());
            await this.browser.click(PO.translate.maximizeButton());

            await this.browser.yaWaitUntil('Кнопка не сменила надпись после расхлопа', async function() {
                const value = await this.getText(PO.translate.maximizeButtonText());
                return value === 'Скрыть';
            });

            await this.browser.assertView('examplesMax', PO.translate.examples());
            await this.browser.click(PO.translate.maximizeButton());

            await this.browser.yaWaitUntil('Кнопка не сменила надпись после сворачивания примеров', async function() {
                const value = await this.getText(PO.translate.maximizeButtonText());
                return value === 'Показать ещё';
            });

            await this.browser.assertView('examplesMin', PO.translate.examples());
        });

        hermione.also.in('iphone-dark');
        it('Кнопка источников работает', async function() {
            await this.browser.yaOpenSerp({
                text: 'кот перевод',
                data_filter: 'translate',
            }, PO.translate(), 'Не появился колдунщик переводов');

            await this.browser.yaWaitForVisible(PO.translate.examples());
            await this.browser.assertView('noPopup', PO.translate.examples());
            await this.browser.click(PO.translate.maximizeButton());
            await this.browser.click(PO.translate.srcIcon1());
            await this.browser.assertView('firstPopup', PO.translate.examples());
            await this.browser.click(PO.translate.srcIcon2());
            await this.browser.assertView('secondPopup', PO.translate.examples());
        });
    });

    describe('Автоопределение и смена целевого языка', function() {
        const japanese = 'Японский';

        const portuguese = 'Португальский';

        const auto = '(автоопределение)';

        beforeEach(async function() {
            await this.browser.yaOpenSerp({ text: 'перевод с русского на английский онлайн' }, PO.translate());
            await this.browser.setValue(PO.translate.textArea.control(), 'ハイ');

            await this.browser.yaWaitUntil('Язык в исходном селекте не определяется', async function() {
                const value = await this.getText(PO.translate.sourceLangSwitch.text());
                return value === japanese + ' ' + auto;
            });

            await this.browser.yaCheckBaobabCounter(() => {}, {
                event: 'tech',
                type: 'translate-language1',
                path: '/$page/$main/$result/translate-form',
            }, 'Не сработал счётчик смены языка при детекте');

            await this.browser.yaTouch(PO.translate.targetLangSwitch());
        });

        it('на любой, кроме автоопределенного', async function() {
            await this.browser.yaCheckBaobabCounter(
                () => this.browser.selectByIndex(PO.translate.targetLangSwitch.control(), LANG_SELECT_PORTUGUESE),
                {
                    path: '/$page/$main/$result/translate-form/select-to',
                    behaviour: { type: 'dynamic' },
                },
            );

            await this.browser.yaWaitUntil('Целевой язык не изменился', async function() {
                const value = await this.getText(PO.translate.targetLangSwitch.text());
                return value === portuguese;
            });

            const value = await this.browser.getText(PO.translate.sourceLangSwitch.text());
            assert.equal(
                value,
                japanese + ' ' + auto,
                'Исходный язык сменился или пропало (автоопределение)',
            );
        });

        it('на автоопределенный', async function() {
            await this.browser.selectByIndex(PO.translate.targetLangSwitch.control(), LANG_SELECT_JAPANESE);

            await this.browser.yaWaitUntil('Целевой язык не изменился', async function() {
                const value = await this.getText(PO.translate.targetLangSwitch.text());
                return value === japanese;
            });

            const value = await this.browser.getText(PO.translate.sourceLangSwitch.text());
            assert.notInclude(
                value,
                auto,
                'Исходный язык не сменился или не пропало (автоопределение)',
            );

            await this.browser.yaCheckBaobabCounter(() => {}, [
                {
                    path: '/$page/$main/$result/translate-form/select-to',
                    behaviour: { type: 'dynamic' },
                },
                {
                    event: 'tech',
                    type: 'translate-language1',
                    path: '/$page/$main/$result/translate-form',
                },
            ], 'Не сработали счётчики на кнопке смены языков местами');
        });
    });

    describe('Озвучка', function() {
        const isSpeaking = 'Translate-Speaker_speaking';

        beforeEach(async function() {
            await this.browser.yaOpenSerp(
                { text: 'переводчик с русского на английский онлайн', data_filter: 'translate' },
                PO.translate(),
            );

            await this.browser.setValue(PO.translate.textArea.control(), 'Incomprehensibilities');

            await this.browser.execute(function(input) {
                window.$(input).blur();
            }, PO.translate.textArea.control());

            await this.browser.yaWaitForVisible(PO.translate.resultText(), 'Не появился результат с переводом');
            await this.browser.yaWaitForVisible(PO.translate.speakerTarget(), 'Не появился динамик перевода');
            await this.browser.yaWaitForVisible(PO.translate.speakerSource(), 'Не появился динамик введенного текста');
            const cls1 = await this.browser.yaGetClass(PO.translate.speakerSource());
            assert(cls1.indexOf(isSpeaking) === -1);
            const cls2 = await this.browser.yaGetClass(PO.translate.speakerTarget());
            assert(cls2.indexOf(isSpeaking) === -1);
        });

        it('Динамик введённого текста выключается при нажатии на динамик перевода', async function() {
            await this.browser.click(PO.translate.speakerSource());

            await this.browser.yaWaitUntil('Динамик введённого текста не активировался', async () => {
                const cls = await this.browser.yaGetClass(PO.translate.speakerSource());
                return cls.indexOf(isSpeaking) !== -1;
            });

            await this.browser.click(PO.translate.speakerTarget());

            await this.browser.yaWaitUntil('Динамик перевода не активировался', async () => {
                const cls = await this.browser.yaGetClass(PO.translate.speakerTarget());
                return cls.indexOf(isSpeaking) !== -1;
            });

            await this.browser.yaWaitUntil('Динамик введённого текста не деактивировался', async () => {
                const cls = await this.browser.yaGetClass(PO.translate.speakerSource());
                return cls.indexOf(isSpeaking) === -1;
            });
        });

        it('Динамик перевода выключается при нажатии на динамик введённого текста', async function() {
            await this.browser.click(PO.translate.speakerTarget());

            await this.browser.yaWaitUntil('Динамик перевода не активировался', async () => {
                const cls = await this.browser.yaGetClass(PO.translate.speakerTarget());
                return cls.indexOf(isSpeaking) !== -1;
            });

            await this.browser.click(PO.translate.speakerSource());

            await this.browser.yaWaitUntil('Динамик введённого текста не активировался', async () => {
                const cls = await this.browser.yaGetClass(PO.translate.speakerSource());
                return cls.indexOf(isSpeaking) !== -1;
            });

            await this.browser.yaWaitUntil('Динамик перевода не деактивировался', async () => {
                const cls = await this.browser.yaGetClass(PO.translate.speakerTarget());
                return cls.indexOf(isSpeaking) === -1;
            });
        });
    });

    describe('Метрики', () => {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({ text: 'кот перевод' }, PO.translate());

            await this.browser.yaCheckMetrics({
                'daily.total_session_count': 1,
                'web.total_request_count': 1,
                'web.total_title_click': 0,
                'web.abandonment': 100,
                'web.tech_part_misc_click': 0,
                'web.total_all_click': 0,
                'web.total_dynamic_click_count': 0,
            });
        });

        it('Выбор исходного языка', async function() {
            await this.browser.yaCheckBaobabCounter(
                () => this.browser.selectByIndex(PO.translate.sourceLangSwitch.control(), LANG_SELECT_PORTUGUESE),
                {
                    path: '/$page/$main/$result/translate-form/select-from',
                    behaviour: { type: 'dynamic' },
                    data: {
                        from: 'ru',
                        to: 'pt',
                    },
                },
                'Неправильный счетчик выбора языка',
            );

            await this.browser.yaCheckMetrics({
                // +1 dynamic-клик в селект — snippet/translate/text/select-from
                'daily.total_session_count': 1,
                'web.total_request_count': 1,
                'web.total_title_click': 0,
                'web.total_all_click': 2,
                'web.total_dynamic_click_count': 2,
                'web.abandonment': 100,
                'web.tech_part_misc_click': 100,
            });
        });

        it('Выбор целевого языка', async function() {
            await this.browser.yaCheckBaobabCounter(
                () => this.browser.selectByIndex(PO.translate.targetLangSwitch.control(), LANG_SELECT_ARABIC),
                {
                    path: '/$page/$main/$result/translate-form/select-to',
                    behaviour: { type: 'dynamic' },
                    data: {
                        from: 'en',
                        to: 'ar',
                    },
                },
                'Неправильный счетчик выбора языка',
            );

            await this.browser.yaCheckMetrics({
                // +1 dynamic-клик в селект — snippet/translate/text/select-to
                'daily.total_session_count': 1,
                'web.total_request_count': 1,
                'web.total_title_click': 0,
                'web.total_all_click': 2,
                'web.total_dynamic_click_count': 2,
                'web.abandonment': 100,
                'web.tech_part_misc_click': 100,
            });
        });

        it('Двойной клик в Селект', async function() {
            await this.browser.click(PO.translate.targetLangSwitch());
            await this.browser.click(PO.translate.targetLangSwitch());

            await this.browser.yaCheckMetrics({
                'daily.total_session_count': 1,
                'web.abandonment': 100,
                'web.tech_part_misc_click': 100,
                'web.total_all_click': 2,
                'web.total_dynamic_click_count': 2,
                'web.total_request_count': 1,
                'web.total_title_click': 0,
            });
        });

        it('Смена направления перевода', async function() {
            await this.browser.yaCheckBaobabCounter(() => this.browser.yaTouch(PO.translate.swapLangsButton()), [
                {
                    path: '/$page/$main/$result/translate-form',
                    event: 'tech',
                    type: 'translate-language1',
                },
                {
                    path: '/$page/$main/$result/translate-form',
                    event: 'tech',
                    type: 'translate-language2',
                },
            ], 'Неправильный счетчик смены направления');

            await this.browser.yaCheckMetrics({
                // +1 dynamic-клик в кнопку — snippet/translate/text/change
                // +1 dynamic-клик перевода — snippet.translate.text.translate
                'daily.total_session_count': 1,
                'web.total_request_count': 1,
                'web.total_title_click': 0,
                'web.abandonment': 100,
                'web.tech_part_misc_click': 100,
                'web.total_all_click': 1,
                'web.total_dynamic_click_count': 1,
            });
        });

        it('Клик по примеру', async function() {
            await this.browser.yaOpenSerp({
                text: 'foreverdata',
                foreverdata: 3845198953,
                data_filter: 'translate',
            }, PO.translate(), 'Не появился колдунщик переводов');

            await this.browser.yaWaitForVisible(PO.translate.examples());

            await this.browser.yaCheckBaobabCounter(PO.translate.examples(), {
                path: '/$page/$main/$result/translate-form/examples',
                behaviour: { type: 'dynamic' },
            }, 'Не сработал счетчик на примерах');
        });
    });
});
