'use strict';

const PO = require('./Translate.page-object')('desktop');

specs({ feature: 'Колдунщик переводов' }, function() {
    describe('Общие проверки', function() {
        hermione.also.in(['chrome-desktop-dark']);
        it('Проверка тултипа копирования', async function() {
            await this.browser.yaOpenSerp({
                text: 'перевод hello',
                data_filter: 'translate',
            }, PO.translate());

            await this.browser.click(PO.translate.copyIcon());
            await this.browser.yaWaitForVisible(PO.translate.copyTooltip());
            await this.browser.assertView('transalateAfterCoping', PO.translate());
            await this.browser.assertView('copyTooltip', PO.translate.copyTooltip());
        });

        // в chrome-desktop не работает имитация Shift+Enter
        hermione.only.notIn('chrome-desktop');
        it('Перенос строки по нажатию Shift+Enter', async function() {
            await this.browser.yaOpenSerp(
                { text: 'кот перевод с русского на английский онлайн', data_filter: 'translate' },
                PO.translate(),
            );

            await this.browser.click(PO.translate.textArea.control());
            await this.browser.yaKeyPress('Shift+Enter', 'кот');
            await this.browser.click(PO.translate.result());
            await this.browser.assertView('transfer', PO.translate.textArea.control());
        });

        it('Проверка саджестов', async function() {
            await this.browser.yaOpenSerp({
                text: 'переводчик онлайн с русского на английский',
                data_filter: 'translate',
            }, PO.translate());

            await this.browser.setValue(PO.translate.textArea.control(), 'hel');
            await this.browser.yaWaitForVisible(PO.translate.suggest());

            await this.browser.yaCheckBaobabCounter(PO.translate.suggest(), {
                path: '$page/$main/$result/translate-form/suggest',
                data: { byEnter: false },
                behaviour: { type: 'dynamic' },
            });

            await this.browser.yaWaitUntil('Не появился результат нажатия саджеста', function() {
                return this.getText(PO.translate.textArea.control()).then(value => value === 'help');
            });

            await this.browser.click(PO.translate.clear());
            await this.browser.setValue(PO.translate.textArea.control(), 'hel');
            await this.browser.yaWaitForVisible(PO.translate.suggest());

            await this.browser.yaCheckBaobabCounter(() => this.browser.yaKeyPress('Enter'), {
                path: '$page/$main/$result/translate-form/suggest',
                data: { byEnter: true },
                behaviour: { type: 'dynamic' },
            });

            await this.browser.yaWaitUntil('Не появился результат нажатия саджеста', function() {
                return this.getText(PO.translate.textArea.control()).then(value => value === 'help');
            });
        });

        it('Проверка саджестов с разделителем', async function() {
            await this.browser.yaOpenSerp({
                text: 'переводчик онлайн с русского на английский',
                data_filter: 'translate',
            }, PO.translate());

            await this.browser.setValue(PO.translate.textArea.control(), 'hello helpi');
            await this.browser.yaWaitForVisible(PO.translate.suggest());
            await this.browser.click(PO.translate.suggest());

            await this.browser.yaWaitUntil('Не появился результат нажатия саджеста', function() {
                return this.getText(PO.translate.textArea.control()).then(value => value === 'hello helping');
            });

            await this.browser.click(PO.translate.clear());
            await this.browser.setValue(PO.translate.textArea.control(), 'hello.helpi');
            await this.browser.yaWaitForVisible(PO.translate.suggest());
            // 1 клик скрывает саджест
            await this.browser.click(PO.translate.textArea.control());
            // второй его опять показывает
            await this.browser.click(PO.translate.textArea.control());
            await this.browser.yaKeyPress('Enter');

            await this.browser.yaWaitUntil('Не появился результат нажатия саджеста', function() {
                return this.getText(PO.translate.textArea.control()).then(value => value === 'hello.helping');
            });
        });

        hermione.also.in(['chrome-desktop-dark']);
        it('Длинные названия в контролах', async function() {
            const textToDetect = 'hi tha mi ùr agad a charaid airson an eadar-theangachadh';

            await this.browser.yaOpenSerp(
                { text: 'перевод с русского на английский онлайн', data_filter: 'translate' },
                PO.translate(),
            );

            await this.browser.setValue(PO.translate.textArea.control(), textToDetect);

            await this.browser.yaWaitUntil('Язык в исходном селекте не определяется', function() {
                return this.getText(PO.translate.sourceLangSwitch.text()).then(value => {
                    return value === 'Шотландский (гэльский) (определено)';
                });
            });

            await this.browser.click(PO.translate.targetLangSwitch());
            await this.browser.yaWaitForVisible(PO.translatePopup(), 'Не показался селект с языками');

            // .yaScroll(PO.translatePopup.PortugueseLang())
            await this.browser.click(PO.translatePopup.PortugueseLang());

            await this.browser.assertView('plain', PO.translate.langControls());
        });

        hermione.also.in(['chrome-desktop-dark']);
        it('Проверка внешнего вида языкового попапа', async function() {
            await this.browser.yaOpenSerp({ text: 'кот перевод', data_filter: 'translate' }, PO.translate());
            await this.browser.click(PO.translate.sourceLangSwitch());
            await this.browser.yaWaitForVisible(PO.translatePopup(), 'Не показался селект с языками');
            await this.browser.assertView('sourceSelectPopup', PO.translate());
            await this.browser.click(PO.translate.targetLangSwitch());
            await this.browser.yaWaitForVisible(PO.translatePopup(), 'Не показался селект с языками');
            await this.browser.assertView('targetSelectPopup', PO.translate());
        });

        hermione.only.notIn('firefox');
        describe('Прерывание нажатием на значок микрофона', function() {
            const isSpeaking = 'Translate-Speaker_speaking';

            // Функция проверки  прерывания озвучки исходного текста или перевода
            async function checkSpeakerInterruption(speaker, screenName) {
                await this.browser.yaOpenSerp({ text: 'кот перевод с русского на английский онлайн' }, PO.translate());
                await this.browser.click(speaker);

                await this.browser.yaWaitUntil('Динамик перевода активировался', async () => {
                    const cls = await this.browser.yaGetClass(speaker);
                    return cls.indexOf(isSpeaking) !== -1;
                });

                await this.browser.assertView('speaker-' + screenName + '-active', PO.translate());

                await this.browser.click(PO.translate.inputVoice());

                await this.browser.yaWaitUntil('Динамик перевода деактивировался', async () => {
                    const cls = await this.browser.yaGetClass(speaker);
                    return cls.indexOf(isSpeaking) === -1;
                });

                await this.browser.assertView('speaker-' + screenName + '-inactive', PO.translate());
            }

            hermione.also.in(['chrome-desktop-dark']);
            it('Озвучки исходного текста', function() {
                return checkSpeakerInterruption.call(this, PO.translate.speakerSource(), 'source');
            });

            hermione.also.in(['chrome-desktop-dark']);
            it('Озвучки перевода', function() {
                return checkSpeakerInterruption.call(this, PO.translate.speakerTarget(), 'target');
            });
        });

        //TODO SERP-96173
        hermione.only.notIn(['firefox']);
        describe('Проверки с контролами', function() {
            beforeEach(async function() {
                await this.browser.yaOpenSerp({ text: 'перевод' }, PO.translate());
                await this.browser.setValue(PO.translate.textArea.control(), 'friend');
                await this.browser.yaWaitForVisible(PO.translate.result(), 'Не появился результат с переводом');
            });

            [
                {
                    message: 'с которого',
                    selectors: PO => ({
                        switch: PO.translate.sourceLangSwitch(),
                    }),
                    counterToken: 'from',
                    dynamicCounterToken: 'language1',
                },
                {
                    message: 'на который',
                    selectors: PO => ({
                        switch: PO.translate.targetLangSwitch(),
                    }),
                    counterToken: 'to',
                    dynamicCounterToken: 'language2',
                },
            ].forEach(function(testData) {
                hermione.also.in(['firefox', 'ipad']);
                it(`При выборе языка, ${testData.message} переводим, результат с переводом обновляется`, async function() {
                    let oldResult;
                    const selectors = testData.selectors(PO);

                    const text = await this.browser.getHTML(PO.translate.result());
                    oldResult = text;

                    await this.browser.yaCheckBaobabCounter(selectors.switch, {
                        path: `/$page/$main/$result/translate-form/select-${testData.counterToken}`,
                        behaviour: { type: 'dynamic' },
                    }, `Не сработал динамический счётчик на переключателе языка, ${testData.message} переводим`);

                    await this.browser.yaWaitForVisible(PO.translatePopup(), 'Не показался селект с языками');
                    await this.browser.yaScroll(PO.translatePopup.AzerbajaniLang());

                    await this.browser.yaCheckBaobabCounter(PO.translatePopup.AzerbajaniLang(), {
                        path: `/$page/$main/$result/translate-form/select-${testData.counterToken}`,
                        behaviour: { type: 'dynamic' },
                        data: { action: 'change' },
                    }, 'Не сработал динамический счётчик счётчик на выбранном языке');

                    await this.browser.yaWaitForHidden(PO.translatePopup(), 'Попап с языками не скрылся');

                    await this.browser.yaWaitUntil('Результат с переводом не обновился', function() {
                        return this.getHTML(PO.translate.result()).then(text => text !== oldResult);
                    });
                });
            });

            hermione.also.in(['chrome-desktop-dark']);
            it('Проверка ховера иконок', async function() {
                await this.browser.yaOpenSerp({
                    text: 'кот перевод',
                    data_filter: 'translate',
                }, PO.translate(), 'Не появился колдунщик переводов');

                await this.browser.yaWaitForVisible(PO.translate.resultDict());
                await this.browser.yaWaitForVisible(PO.translate.examples());
                await this.browser.moveToObject(PO.translate.speakerSource(), 0, 0);
                await this.browser.assertView('hoveredSpeakerSource', PO.translate.speakerSource());
                await this.browser.moveToObject(PO.translate.speakerTarget(), 0, 0);
                await this.browser.assertView('hoveredSpeakerTarget', PO.translate.speakerTarget());
                await this.browser.moveToObject(PO.translate.inputVoice(), 0, 0);
                await this.browser.assertView('hoveredVoice', PO.translate.inputVoice());
                await this.browser.moveToObject(PO.translate.clear(), 0, 0);
                await this.browser.assertView('hoveredClear', PO.translate.clear());
                await this.browser.moveToObject(PO.translate.swapLangsButton(), 0, 0);
                await this.browser.assertView('hoveredSwapLangs', PO.translate.swapLangsButton());
                await this.browser.moveToObject(PO.translate.srcIcon1(), 0, 0);
                await this.browser.assertView('hoveredExampleIcon1', PO.translate.srcIcon1());
                await this.browser.click(PO.translate.maximizeButton());
            });

            hermione.also.in(['chrome-desktop-dark']);
            it('Кнопка перезагрузки работает', async function() {
                await this.browser.yaOpenSerp({
                    text: 'перевод',
                    exp_flags: 'translate_desktop_redesign',
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

            it('Закрытие попапа с языками по второму нажатию на селект', async function() {
                await this.browser.yaOpenSerp({
                    text: 'перевод',
                    exp_flags: 'translate_desktop_redesign',
                    data_filter: 'translate',
                }, PO.translate(), 'Не появился колдунщик переводов');

                await this.browser.click(PO.translate.targetLangSwitch());
                await this.browser.yaWaitForVisible(PO.translatePopupLego(), 'Не показался селект с языками');
                await this.browser.click(PO.translate.targetLangSwitch());
                await this.browser.yaWaitForHidden(PO.translatePopupLego(), 'Не показался селект с языками');
            });
        });

        hermione.only.notIn('firefox');
        describe('Проверки с контролами (без текста)', function() {
            it('Смена языка текста на язык перевода', async function() {
                await this.browser.yaOpenSerp({ text: 'перевод на английский' }, PO.translate());
                const value1 = await this.browser.getText(PO.translate.sourceLangSwitch.text());

                assert(value1 === 'Русский');

                const value2 = await this.browser.getText(PO.translate.targetLangSwitch.text());

                assert(value2 === 'Английский');

                await this.browser.yaTouch(PO.translate.sourceLangSwitch());
                await this.browser.yaWaitForVisible(PO.translatePopup(), 'Не показался селект с языками у первого селекта');
                await this.browser.yaScroll(PO.translatePopup.EnglishLang());
                await this.browser.yaTouch(PO.translatePopup.EnglishLang());

                await this.browser.yaWaitUntil('Язык в первом селекте не изменился', function() {
                    return this.getText(PO.translate.sourceLangSwitch.text()).then(value => {
                        return value === 'Английский';
                    });
                });

                const value3 = await this.browser.getText(PO.translate.targetLangSwitch.text());
                assert.equal(value3, 'Русский', 'Язык во втором селекте не изменился');

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
                await this.browser.yaOpenSerp({ text: 'перевод' }, PO.translate());
                const value1 = await this.browser.getText(PO.translate.sourceLangSwitch.text());

                assert(value1 === 'Английский');

                const value2 = await this.browser.getText(PO.translate.targetLangSwitch.text());

                assert(value2 === 'Русский');

                await this.browser.yaTouch(PO.translate.targetLangSwitch());
                await this.browser.yaWaitForVisible(PO.translatePopup(), 'Не показался селект с языками у первого селекта');
                await this.browser.yaScroll(PO.translatePopup.EnglishLang());
                await this.browser.yaTouch(PO.translatePopup.EnglishLang());

                await this.browser.yaWaitUntil('Язык во втором селекте не изменился', function() {
                    return this.getText(PO.translate.targetLangSwitch.text()).then(value => {
                        return value === 'Английский';
                    });
                });

                const value3 = await this.browser.getText(PO.translate.sourceLangSwitch.text());
                assert.equal(value3, 'Русский', 'Язык в первом селекте не изменился');

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
                await this.browser.yaOpenSerp({ text: 'перевод' }, PO.translate());
                const value1 = await this.browser.getText(PO.translate.sourceLangSwitch.text());

                assert(value1 === 'Английский');

                const value2 = await this.browser.getText(PO.translate.targetLangSwitch.text());

                assert(value2 === 'Русский');

                await this.browser.click(PO.translate.swapLangsButton());

                await this.browser.yaWaitUntil('Язык оригинала не обновился', function() {
                    return this.getText(PO.translate.sourceLangSwitch.text()).then(value => {
                        return value === 'Русский';
                    });
                });

                await this.browser.yaWaitUntil('Язык перевода не обновился', function() {
                    return this.getText(PO.translate.targetLangSwitch.text()).then(value => {
                        return value === 'Английский';
                    });
                });
            });
        });
    });

    hermione.only.notIn('firefox');
    describe('Автоопределение и смена целевого языка', function() {
        const arabic = 'Арабский';

        const armenian = 'Армянский';

        const auto = '(определено)';

        beforeEach(async function() {
            await this.browser.yaOpenSerp({ text: 'перевод онлайн' }, PO.translate());
            await this.browser.setValue(PO.translate.textArea.control(), 'ما سعر التذكرة؟');

            await this.browser.yaWaitUntil('Язык в исходном селекте не определяется', function() {
                return this.getText(PO.translate.sourceLangSwitch.text()).then(value => {
                    return value === arabic + ' ' + auto;
                });
            });

            await this.browser.yaCheckBaobabCounter(() => {}, {
                event: 'tech',
                type: 'translate-language1',
                path: '/$page/$main/$result/translate-form',
            });

            await this.browser.click(PO.translate.targetLangSwitch());
            await this.browser.yaWaitForVisible(PO.translatePopup(), 'Не показался селект с языками');
        });

        it('На любой, кроме автоопределенного', async function() {
            await this.browser.yaScroll(PO.translatePopup.ArmenianLang());

            await this.browser.yaCheckBaobabCounter(() => this.browser.yaTouch(PO.translatePopup.ArmenianLang()), {
                path: '/$page/$main/$result/translate-form/select-to',
                behaviour: { type: 'dynamic' },
                data: { action: 'change' },
            });

            await this.browser.yaWaitUntil('Целевой язык не изменился', function() {
                return this.getText(PO.translate.targetLangSwitch.text()).then(value => {
                    return value === armenian;
                });
            });

            const value = await this.browser.getText(PO.translate.sourceLangSwitch.text());
            assert.equal(
                value,
                arabic + ' ' + auto,
                'Исходный язык сменился или пропало (определено)',
            );
        });

        it('На автоопределенный', async function() {
            await this.browser.yaScroll(PO.translatePopup.ArabicLang());
            await this.browser.click(PO.translatePopup.ArabicLang());

            await this.browser.yaWaitUntil('Целевой язык не изменился', function() {
                return this.getText(PO.translate.targetLangSwitch.text()).then(value => {
                    return value === arabic;
                });
            });

            const value = await this.browser.getText(PO.translate.sourceLangSwitch.text());
            assert.notInclude(
                value,
                auto,
                'Исходный язык не сменился или не пропало (определено)',
            );

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

        /**
         * В FF нет голосов для NativePlayer:
         * window.speechSynthesis.getVoices => []
         */
        hermione.only.notIn('firefox');
        it('Динамик введённого текста выключается при нажатии на динамик перевода', async function() {
            await this.browser.waitForBlinked(
                () => this.browser.click(PO.translate.speakerSource()),
                PO.translate.speakerSourceSpeaking(),
                'Динамик введенного текста не активировался',
            );

            await this.browser.waitForBlinked(
                () => this.browser.click(PO.translate.speakerTarget()),
                PO.translate.speakerTargetSpeaking(),
                'Динамик перевода не активировался',
            );

            await this.browser.yaWaitUntil('Динамик введённого текста не деактивировался', async () => {
                const cls = await this.browser.yaGetClass(PO.translate.speakerSource());
                return cls.indexOf(isSpeaking) === -1;
            });
        });

        /**
         * В FF нет голосов для NativePlayer:
         * window.speechSynthesis.getVoices => []
         */
        hermione.only.notIn('firefox');
        it('Динамик перевода выключается при нажатии на динамик введённого текста', async function() {
            await this.browser.waitForBlinked(
                () => this.browser.click(PO.translate.speakerTarget()),
                PO.translate.speakerTargetSpeaking(),
                'Динамик перевода не активировался',
            );

            await this.browser.waitForBlinked(
                () => this.browser.click(PO.translate.speakerSource()),
                PO.translate.speakerSourceSpeaking(),
                'Динамик введенного текста не активировался',
            );

            await this.browser.yaWaitUntil('Динамик перевода не деактивировался', async () => {
                const cls = await this.browser.yaGetClass(PO.translate.speakerTarget());
                return cls.indexOf(isSpeaking) === -1;
            });
        });
    });

    describe('К-к фотоперевода', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                text: 'foreverdata',
                foreverdata: 1000286293,
                data_filter: 'translate',
            }, PO.translate());
        });

        hermione.also.in(['chrome-desktop-dark']);
        it('Внешний вид к-ка фотоперевода', async function() {
            await this.browser.assertView('ocrTranslate', PO.translate());
        });
    });

    describe('Метрики', () => {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({ text: 'кот перевод', data_filter: 'translate' }, PO.translate());

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

        it('Клик в микрофон', async function() {
            await this.browser.click(PO.translate.inputVoice());

            await this.browser.yaCheckMetrics({
                // +1 dynamic-клик в кнопку — snippet/translate/text/speech
                'daily.total_session_count': 1,
                'web.total_request_count': 1,
                'web.total_title_click': 0,
                'web.abandonment': 100,
                'web.tech_part_misc_click': 100,
                'web.total_all_click': 1,
                'web.total_dynamic_click_count': 1,
            });
        });

        hermione.only.notIn('firefox');
        it('Выбор исходного языка', async function() {
            await this.browser.click(PO.translate.sourceLangSwitch());
            await this.browser.yaWaitForVisible(PO.translatePopup());
            await this.browser.yaScroll(PO.translatePopup.ArabicLang());

            await this.browser.yaCheckBaobabCounter(PO.translatePopup.ArabicLang(), {
                path: '/$page/$main/$result/translate-form/select-from',
                behaviour: { type: 'dynamic' },
                data: {
                    from: 'ru',
                    to: 'ar',
                },
            });

            await this.browser.yaCheckMetrics({
                'daily.total_session_count': 1,
                'web.total_request_count': 1,
                'web.total_title_click': 0,
                'web.abandonment': 100,
                'web.tech_part_misc_click': 100,
                'web.total_all_click': 3,
                'web.total_dynamic_click_count': 3,
            });
        });

        hermione.only.notIn('firefox');
        it('Выбор целевого языка', async function() {
            await this.browser.click(PO.translate.targetLangSwitch());
            await this.browser.yaWaitForVisible(PO.translatePopup());
            await this.browser.yaScroll(PO.translatePopup.ArabicLang());

            await this.browser.yaCheckBaobabCounter(PO.translatePopup.ArabicLang(), {
                path: '/$page/$main/$result/translate-form/select-to',
                behaviour: { type: 'dynamic' },
                data: {
                    from: 'en',
                    to: 'ar',
                },
            }, 'Неправильный счетчик выбора языка');

            await this.browser.yaCheckMetrics({
                'daily.total_session_count': 1,
                'web.total_request_count': 1,
                'web.total_title_click': 0,
                'web.abandonment': 100,
                'web.tech_part_misc_click': 100,
                'web.total_all_click': 3,
                'web.total_dynamic_click_count': 3,
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

        hermione.only.notIn('firefox');
        it('Проверка неактивных кнопок', async function() {
            await this.browser.click(PO.translate.sourceLangSwitch());
            await this.browser.yaScroll(PO.translatePopup.AzerbajaniLang());
            await this.browser.click(PO.translatePopup.AzerbajaniLang());
            await this.browser.click(PO.translate.targetLangSwitch());
            await this.browser.yaScroll(PO.translatePopup.ArmenianLang());
            await this.browser.click(PO.translatePopup.ArmenianLang());
            await this.browser.click(PO.translate.speakerSource());
            await this.browser.click(PO.translate.inputVoice());
            await this.browser.yaWaitForVisible(PO.translate.resultText(), 'Не появился результат с переводом');
            await this.browser.click(PO.translate.speakerTarget());

            // +2 клика относительно BEM-версии
            // исправить в SERP-88046
            await this.browser.yaCheckMetrics({
                'daily.total_session_count': 1,
                'web.total_request_count': 1,
                'web.total_title_click': 0,
                'web.abandonment': 100,
                'web.tech_part_misc_click': 100,
                'web.total_all_click': 6,
                'web.total_dynamic_click_count': 6,
            });
        });

        it('Смена направления перевода', async function() {
            await this.browser.yaCheckBaobabCounter(PO.translate.swapLangsButton(), [
                {
                    event: 'tech',
                    type: 'translate-language1',
                    path: '/$page/$main/$result/translate-form',
                },
                {
                    event: 'tech',
                    type: 'translate-language2',
                    path: '/$page/$main/$result/translate-form',
                },
            ]);

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

        it('Клик в словарную статью', async function() {
            const clicksCount = 1;

            await this.browser.yaCheckBaobabCounter(
                PO.translate.resultDict(),
                {
                    path: '/$page/$main/$result/translate-form/article',
                    behaviour: { type: 'dynamic' },
                },
            );
            await this.browser.yaCheckMetrics({
                'daily.total_session_count': 1,
                'web.total_request_count': 1,
                'web.total_title_click': 0,

                // +1 dynamic-клик snippet/translate/text/article
                'web.abandonment': 100,
                'web.tech_part_misc_click': 100,
                'web.total_all_click': clicksCount,
                'web.total_dynamic_click_count': clicksCount,
            });
        });
    });

    hermione.also.in(['chrome-desktop-dark']);
    it('Отступ между контролами и словарем', async function() {
        await this.browser.yaOpenSerp(
            { text: 'breit перевод с немецкого', data_filter: 'translate' },
            PO.translate(),
        );

        await this.browser.yaWaitForVisible(PO.translate.resultDict(), 'Не появился результат с переводом');
        await this.browser.yaWaitForVisible(PO.translate.speakerTarget(), 'Не появился значок озвучки');
        const str = await this.browser.yaGetClass(PO.translate.speakerTarget());
        assert(str.indexOf('hidden') === -1);
        await this.browser.assertView('plain', PO.translate.result());
    });

    hermione.also.in(['chrome-desktop-dark']);
    it('Проверка, что в словаре не едет верстка, когда пришли разные слова', async function() {
        await this.browser.yaOpenSerp({ text: 'перевод с французского' }, PO.translate());
        await this.browser.setValue(PO.translate.textArea.control(), 'croissant');
        await this.browser.yaWaitForVisible(PO.translate.resultDict(), 'Не появился словарь');
        await this.browser.assertView('dictColumns', PO.translate.resultDict());
    });

    // Такой же тест на touch-phone, отличается параметром -srv
    describe('Отправка статистики при инициализации', () => {
        [
            {
                message: 'с пустым колдунщиком',
                query: 'перевод с русского на английский онлайн',
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
                    event: 'tech',
                    type: 'translate-init',
                    path: '/$page/$main/$result/translate-form',
                    data: {
                        srv: 'wizard-back',
                        empty: type.empty,
                    },
                }, 'Не отправился счётчик инициализации');
            });
        });
    });

    // Счётчики, которые не были покрыты тестами в старой версии
    describe('Счётчики дополнительные', () => {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({ text: 'перевод с русского на английский онлайн' }, PO.translate());
        });

        it('При клике на область ввода', async function() {
            await this.browser.yaCheckBaobabCounter(PO.translate.textArea(), {
                path: '/$page/$main/$result/translate-form/input',
                behaviour: { type: 'dynamic' },
            });
        });

        // it('При клике на перевод из словаря', function() {
        //     return this.browser
        //         .setValue(PO.translate.input.control(), 'doggie')
        //         .yaWaitForVisible(PO.translate.resultText(), 'Не появился результат с переводом')
        //         .yaCheckBaobabCounter(PO.translate.resultArticle(), {
        //             path: '/$page/$main/$result/translate-form/article',
        //             behaviour: { type: 'dynamic' },
        //         });
        // });
    });

    hermione.also.in(['chrome-desktop-dark']);
    it('Проверка внешнего вида наведения на ссылки в фактовом к-ке для перевода из словаря', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: 3122732455,
            data_filter: 'translate',
        }, PO.factTranslate());

        await this.browser.moveToObject(PO.fact.answer.link());
        await this.browser.assertView('translate-answer-hover', PO.fact());

        await this.browser.moveToObject(PO.fact.sourceLink());
        await this.browser.assertView('translate-link-hover', PO.fact());

        await this.browser.moveToObject(PO.fact.footer());
        await this.browser.assertView('translate-footer-hover', PO.fact());
    });
});
