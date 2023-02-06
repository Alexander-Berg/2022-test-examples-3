'use strict';

const connection = require('../../../../hermione/client-scripts/connection');
const TranslatePO = require('./Translate.page-object');
let PO;
let clearButton;
let clearButtonPath;
let sourceSelectButton;
let targetSelectButton;

specs({ feature: 'Колдунщик переводов' }, function() {
    beforeEach(function() {
        PO = TranslatePO(this.currentPlatform);
        clearButton = PO.translate.clear();
        clearButtonPath = '/$page/$main/$result/translate-form/clear';
        sourceSelectButton = PO.translate.sourceLangSwitch.button();
        targetSelectButton = PO.translate.targetLangSwitch.button();

        return this.browser;
    });

    hermione.also.in(['chrome-desktop-dark', 'iphone-dark']);
    it('Внешний вид с пустым полем ввода', async function() {
        await this.browser.yaOpenSerp(
            { text: 'переводчик онлайн с русского на английский', data_filter: 'translate' },
            PO.translate(),
        );

        await this.browser.assertView('empty', PO.translate());
    });

    it('Если пришел новый неизвестный язык в пустой к-к', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: 2195945197,
            data_filter: 'translate',
        }, PO.translate());

        await this.browser.assertView('unknownEmptySource', PO.translate.sourceLangSwitch());
        await this.browser.assertView('unknownEmptyTarget', PO.translate.targetLangSwitch());
    });

    hermione.also.in(['chrome-desktop-dark', 'iphone-dark']);
    it('Проверка внешнего вида фактового к-ка', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: 3127763789,
            data_filter: 'translate',
        }, PO.factTranslate());

        await this.browser.assertView('fact', [
            PO.factTranslate(),
        ]);
    });

    it('Не показывается спиннер, при потере фокуса у инпута, если текст не поменялся', async function() {
        await this.browser.yaOpenSerp({
            text: 'кот перевод',
            data_filter: 'translate',
        }, PO.translate());

        await this.browser.click(PO.translate.textArea.control());

        await this.browser.execute(function(input) {
            window.$(input).blur();
        }, PO.translate.textArea.control());

        await this.browser.yaShouldNotBeVisible(PO.translate.spinner());
    });

    hermione.also.in(['chrome-desktop-dark', 'iphone-dark']);
    it('Проверка внешнего вида фактового к-ка для перевода из словаря', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: 3122732455,
            data_filter: 'translate',
        }, PO.factTranslate());

        await this.browser.assertView('fact-dictionary', [
            PO.factTranslate(),
        ]);
    });

    hermione.also.in(['chrome-desktop-dark', 'iphone-dark']);
    it('Если перевод не пришел на сервере', async function() {
        await this.browser.yaOpenSerp(
            { text: 'неглиже перевод на татарский язык онлайн', data_filter: 'translate' },
            PO.translate(),
        );

        await this.browser.yaWaitUntil('Не дождались перевода', function() {
            return this.getHTML(PO.translate.resultText(), false);
        });

        await this.browser.assertView('plain', PO.translate.translate());
    });

    it('Автоопределение языка', async function() {
        const browser = this.browser;

        await browser.yaOpenSerp(
            { text: 'белиссимо с итальянского перевод онлайн', data_filter: 'translate' },
            PO.translate(),
        );

        await browser.yaCheckBaobabCounter(() => { }, [
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
            {
                path: '/$page/$main/$result/translate-form',
                event: 'tech',
                type: 'translate',
            },
        ]);

        await browser.yaCheckMetrics({
            'web.total_all_click': 0,
            'web.total_dynamic_click_count': 0,
        });
    });

    it('Проверка на доступность', async function() {
        await this.browser.yaOpenSerp({
            text: 'кот перевод',
            data_filter: 'translate',
        }, PO.translate());

        const role1 = await this.browser.getAttribute(PO.translate.swapLangsButton(), 'role');
        assert.equal(role1, 'button', 'Сломана роль свопа языков');
        const role2 = await this.browser.getAttribute(PO.translate.swapLangsButton(), 'tabindex');
        assert.equal(role2, '0', 'Сломан атрибут tabindex свопа языков');
        const role3 = await this.browser.getAttribute(PO.translate.speakerSource(), 'role');
        assert.equal(role3, 'button', 'Сломана роль озвучки исходного текста');
        const role4 = await this.browser.getAttribute(PO.translate.speakerSource(), 'tabindex');
        assert.equal(role4, '0', 'Сломан атрибут tabindex озвучки исходного текста');
        const role5 = await this.browser.getAttribute(PO.translate.speakerTarget(), 'role');
        assert.equal(role5, 'button', 'Сломана роль озвучки перевода');
        const role6 = await this.browser.getAttribute(PO.translate.speakerTarget(), 'tabindex');
        assert.equal(role6, '0', 'Сломан атрибут озвучки перевода');
        const role7 = await this.browser.getAttribute(PO.translate.copyIcon(), 'role');
        assert.equal(role7, 'button', 'Сломана роль копирования');
        const role8 = await this.browser.getAttribute(PO.translate.copyIcon(), 'tabindex');
        assert.equal(role8, '0', 'Сломан атрибут tabindex копирования');
        const role9 = await this.browser.getAttribute(sourceSelectButton, 'role');
        assert.equal(role9, 'button', 'Сломана роль селекта исходного языка');
        const role10 = await this.browser.getAttribute(sourceSelectButton, 'tabindex');
        assert.equal(role10, '0', 'Сломан атрибут tabindex селекта исходного языка');
        const role11 = await this.browser.getAttribute(targetSelectButton, 'role');
        assert.equal(role11, 'button', 'Сломана роль селекта языка перевода');
        const role12 = await this.browser.getAttribute(targetSelectButton, 'tabindex');
        assert.equal(role12, '0', 'Сломан атрибут tabindex селекта языка перевода');
    });

    describe('Общие проверки', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                text: 'перевод с английского на русский онлайн',
                srcskip: 'YABS_DISTR',
                data_filter: 'translate',
            }, PO.translate());
        });

        hermione.only.notIn('iphone');
        hermione.also.in(['safari13', 'ipad']);
        it('Копирование', async function() {
            await this.browser.setValue(PO.translate.textArea.control(), 'get');
            await this.browser.yaWaitForVisible(PO.translate.result(), 'Не появился результат с переводом');
            await this.browser.yaWaitForVisible(PO.translate.copyIcon(), 'Иконка не появилась');

            await this.browser.yaCheckBaobabCounter(PO.translate.copyIcon(), {
                path: '/$page/$main/$result/translate-form/copy',
                behaviour: { type: 'dynamic' },
            }, 'Не сработал динамический счётчик на копирование перевода');
        });

        it('Неизвестный язык при автодетекте', async function() {
            await this.browser.yaOpenSerp(
                { text: 'перевод с русского на английский онлайн', data_filter: 'translate' },
                PO.translate(),
            );

            await this.browser.yaMockXHR({
                urlDataMap: {
                    '^https://translate.yandex.net/api/v1/tr.json/detect': {
                        code: 200,
                        lang: 'new_lang',
                    },
                },
            });

            await this.browser.setValue(PO.translate.textArea.control(), 'ダイレクト');
            await this.browser.pause(2000);
            await this.browser.assertView('autodetect_unknown', PO.translate.sourceLangSwitch());
        });

        hermione.also.in(['safari13', 'firefox', 'ipad']);
        it('Вставка текста из буфера', async function() {
            await this.browser.setValue(PO.translate.textArea.control(), 'ハ');
            await this.browser.pause(2000);
            await this.browser.yaWaitForVisible(PO.translate.resultText(), 'Не появился результат с переводом');
            await this.browser.click(PO.translate.resultText());

            await this.browser.assertView('plain', PO.translate());
        });

        it('Очистка поля ввода крестиком', async function() {
            await this.browser.setValue(PO.translate.textArea.control(), 'search');
            await this.browser.yaWaitForVisible(clearButton, 'Не появился крестик для очистки поля ввода');

            await this.browser.yaCheckBaobabCounter(clearButton, {
                path: clearButtonPath,
            });

            await this.browser.yaWaitUntil('Текст не удалился', async () => {
                const value = await this.browser.getValue(PO.translate.textArea.control());
                return value === '';
            });
        });

        describe('Проверки с контролами', function() {
            beforeEach(async function() {
                await this.browser.setValue(PO.translate.textArea.control(), 'friend');

                await this.browser.yaWaitUntil('Не появился результат с переводом', function() {
                    return this.getText(PO.translate.resultText()).then(value => value === 'друг');
                });
            });

            it('При смене языков местами значение инпута изменяется', async function() {
                await this.browser.yaCheckBaobabCounter(PO.translate.swapLangsButton(), {
                    path: '/$page/$main/$result/translate-form/change',
                    behaviour: { type: 'dynamic' },
                });

                await this.browser.yaWaitUntil('Результат с переводом не обновился', function() {
                    return this.getValue(PO.translate.textArea.control()).then(value => value !== 'friend');
                });
            });
        });

        it('Смена направления при плохом интернете', async function() {
            await this.browser.execute(connection.stop);
            await this.browser.setValue(PO.translate.textArea.control(), 'test');

            await this.browser.yaCheckBaobabCounter(PO.translate.swapLangsButton(), {
                path: '/$page/$main/$result/translate-form/change',
            });

            const value = await this.browser.getValue(PO.translate.textArea.control());
            (await value) === 'test';
        });
    });

    describe('Проверки с текстом', () => {
        it('Повторный перевод (пустой колдунщик)', async function() {
            const textToTranslate = 'test';

            const translation = 'тест';

            await this.browser.yaOpenSerp(
                { text: 'перевод онлайн с русского на английский', data_filter: 'translate' },
                PO.translate(),
            );

            await this.browser.setValue(PO.translate.textArea.control(), textToTranslate);

            await this.browser.yaWaitUntil('Не появился результат с переводом', function() {
                return this.getText(PO.translate.resultText()).then(value => value === translation);
            });

            await this.browser.click(clearButton);

            await this.browser.yaWaitUntil('Текст не удалился', function() {
                return this.getValue(PO.translate.textArea.control()).then(value => value === '');
            });

            await this.browser.setValue(PO.translate.textArea.control(), textToTranslate);

            await this.browser.yaWaitUntil('Не появился результат с повторным переводом', function() {
                return this.getText(PO.translate.resultText()).then(value => value === translation);
            });
        });

        it('Повторный перевод другого слова', async function() {
            const textToTranslate = 'test';

            const translation = 'тест';

            await this.browser.yaOpenSerp(
                { text: 'перевод онлайн с русского на английский', data_filter: 'translate' },
                PO.translate(),
            );

            await this.browser.setValue(PO.translate.textArea.control(), textToTranslate);

            await this.browser.yaWaitUntil('Не появился результат с переводом', function() {
                return this.getText(PO.translate.resultText()).then(value => value === translation);
            });

            await this.browser.click(clearButton);

            await this.browser.yaWaitUntil('Текст не удалился', function() {
                return this.getValue(PO.translate.textArea.control()).then(value => value === '');
            });

            await this.browser.setValue(PO.translate.textArea.control(), 'cat');

            await this.browser.yaWaitUntil('Не появился результат с повторным переводом другого слова', function() {
                return this.getText(PO.translate.resultText()).then(value => value === 'кошка');
            });
        });

        it('Повторный перевод (заполненный колдунщик)', async function() {
            const textToTranslate = 'test';

            const translation = 'тест';

            await this.browser.yaOpenSerp(
                { text: 'перевести ' + textToTranslate + ' онлайн', data_filter: 'translate' },
                PO.translate(),
            );

            await this.browser.yaWaitUntil('Не появился результат с переводом', function() {
                return this.getText(PO.translate.resultText()).then(value => value === translation);
            });

            await this.browser.click(clearButton);

            await this.browser.yaWaitUntil('Текст не удалился', function() {
                return this.getValue(PO.translate.textArea.control()).then(value => value === '');
            });

            await this.browser.setValue(PO.translate.textArea.control(), textToTranslate);

            await this.browser.yaWaitUntil('Не появился результат с повторным переводом', function() {
                return this.getText(PO.translate.resultText()).then(value => value === translation);
            });
        });

        it('Повторный перевод (колдунщик без словаря)', async function() {
            const textToTranslate = 'test get';
            const textToTranslateWithDict = 'test';

            await this.browser.yaOpenSerp(
                { text: 'перевести ' + textToTranslate + ' онлайн', data_filter: 'translate' },
                PO.translate(),
            );

            await this.browser.yaWaitForVisible(PO.translate.resultText(), 'Не появился результат с переводом');
            await this.browser.click(clearButton);
            await this.browser.setValue(PO.translate.textArea.control(), textToTranslateWithDict);
            await this.browser.yaWaitForVisible(PO.translate.resultDict(), 'Не появился словарь');
            await this.browser.click(clearButton);
            await this.browser.setValue(PO.translate.textArea.control(), textToTranslate);
            await this.browser.yaWaitForVisible(PO.translate.resultText(), 'Не появился результат с повторным переводом');
        });

        it('Перевод берется из поля translation', async function() {
            const translation = 'отлично сработано';

            await this.browser.yaOpenSerp({
                text: 'foreverdata',
                foreverdata: 652258663,
                data_filter: 'translate',
            }, PO.translate(), 'Не появился колдунщик переводов');

            const value1 = await this.browser.getText(PO.translate.resultText());
            assert.equal(value1, translation, 'Перевод не совпадает с ожидаемым');
            const value2 = await this.browser.getText(PO.translate.resultDict());
            assert.notEqual(value2, '', 'Словарная статья пустая');
            await this.browser.yaShouldBeVisible(PO.translate.resultDict());
        });

        it('Перевод возьмется из ручки переводчика, а не из словаря', async function() {
            const translation = 'visionary';

            await this.browser.yaOpenSerp({
                text: 'foreverdata',
                foreverdata: 1123886152,
                data_filter: 'translate',
            }, PO.translate(), 'Не появился колдунщик переводов');

            const value1 = await this.browser.getText(PO.translate.resultText());
            assert.isTrue(value1.indexOf(translation) !== -1, 'Перевод не совпадает с бэкендным');
            const value2 = await this.browser.getText(PO.translate.resultDict());
            assert.notEqual(value2, '', 'Словарная статья пустая');
        });
    });

    describe('Проверка технических кликов при свопе', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp(
                { text: 'кот перевод с русского на английский онлайн', data_filter: 'translate' },
                PO.translate(),
            );

            await this.browser.yaCheckMetrics({
                'daily.total_session_count': 1,
                'web.total_request_count': 1,
                'web.total_title_click': 0,
                'web.abandonment': 100,
                'web.total_all_click': 0,
                'web.total_dynamic_click_count': 0,
                'web.tech_part_misc_click': 0,
            });
        });

        it('При смене языков местами нет повторений технических кликов', async function() {
            await this.browser.yaCheckBaobabCounter(PO.translate.swapLangsButton(), [
                {
                    path: '/$page/$main/$result/translate-form/change',
                    behaviour: { type: 'dynamic' },
                },
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
                {
                    path: '/$page/$main/$result/translate-form',
                    event: 'tech',
                    type: 'translate',
                },
            ]);

            await this.browser.yaWaitUntil('Результат с переводом не обновился', function() {
                return this.getValue(PO.translate.textArea.control()).then(value => value !== 'кот');
            });
        });
    });

    describe('Локализация в словарной статье', () => {
        const languages = [
            {
                title: 'язык uk',
                l10n: 'uk',
                tld: 'ru',
            },
            {
                title: 'язык be',
                l10n: 'be',
                tld: 'ru',
            },
            {
                title: 'язык tr',
                l10n: 'tr',
                tld: 'com.tr',
            },
            {
                title: 'язык kk',
                l10n: 'kk',
                tld: 'ru',
            },
        ];

        languages.forEach(function(data) {
            it(data.title, async function() {
                await this.browser.yaOpenSerp({
                    text: 'перевод с английского на русский онлайн',
                    tld: data.tld,
                    l10n: data.l10n,
                    data_filter: 'translate',
                }, PO.translate());

                await this.browser.setValue(PO.translate.textArea.control(), 'cat.');
                await this.browser.yaWaitForVisible(PO.translate.resultDict(), 'Результат с переводом не появился');

                await this.browser.assertView('plain', PO.translate.resultDict(), {
                    hideElements: PO.translate.copyIcon(),
                });
            });
        });
    });

    describe('Озвучка', () => {
        // Проверяем конкретно 2 скриншота динамиков,
        // так тест не зависит от общей верстки к-ка
        hermione.also.in(['chrome-desktop-dark', 'iphone-dark']);
        it('Некликабельный контрол для неподдерживаемых языков', async function() {
            await this.browser.yaOpenSerp(
                { text: 'перевод с болгарского на македонский Здравей, свят', data_filter: 'translate' },
                PO.translate(),
            );

            await this.browser.assertView('nonclickableSource', PO.translate.speakerSource());
            await this.browser.assertView('nonclickableTarget', PO.translate.speakerTarget());
        });
    });

    describe('Метрики', () => {
        beforeEach(async function() {
            await this.browser.yaOpenSerp(
                { text: 'кот перевод с русского на английский онлайн', data_filter: 'translate' },
                PO.translate(),
            );

            await this.browser.yaCheckMetrics({
                'daily.total_session_count': 1,
                'web.total_request_count': 1,
                'web.total_title_click': 0,
                'web.abandonment': 100,
                'web.tech_part_misc_click': 0,
                'web.total_all_click': 0,
                'web.total_dynamic_click_count': 0,
                'web.click_count_overlong_p1_120$': 0,
            });
        });

        hermione.only.notIn(['firefox']);
        it('Озвучание перевода', async function() {
            await this.browser.click(PO.translate.speakerTarget());

            await this.browser.yaCheckMetrics({
                'daily.total_session_count': 1,
                'web.total_request_count': 1,
                'web.abandonment': 100,
                'web.tech_part_misc_click': 100,
                'web.total_all_click': 1,
                'web.total_dynamic_click_count': 1,
                'web.click_count_overlong_p1_120$': 0,
            });
        });

        /**
         * В FF нет голосов для NativePlayer:
         * window.speechSynthesis.getVoices => []
         */
        hermione.only.notIn(['firefox']);
        it('Озвучание введенного текста', async function() {
            await this.browser.click(PO.translate.speakerSource());

            await this.browser.yaCheckMetrics({
                'daily.total_session_count': 1,
                'web.total_request_count': 1,
                'web.abandonment': 100,
                'web.tech_part_misc_click': 100,
                'web.total_all_click': 1,
                'web.total_dynamic_click_count': 1,
                'web.click_count_overlong_p1_120$': 0,
            });
        });

        hermione.only.notIn('iphone');
        it('Копирование перевода', async function() {
            await this.browser.click(PO.translate.copyIcon());

            await this.browser.yaCheckMetrics({
                'daily.total_session_count': 1,
                'web.total_request_count': 1,
                'web.total_title_click': 0,
                'web.total_all_click': 1,
                'web.total_dynamic_click_count': 1,
                'web.abandonment': 100,
                'web.tech_part_misc_click': 100,
                'web.click_count_overlong_p1_120$': 0,
            });
        });

        it('Клик в крестик', async function() {
            await this.browser.click(clearButton);

            await this.browser.yaCheckMetrics({
                'daily.total_session_count': 1,
                'web.abandonment': 100,
                'web.tech_part_misc_click': 100,
                'web.total_all_click': 2,
                'web.total_dynamic_click_count': 2,
                'web.total_request_count': 1,
                'web.total_title_click': 0,
                'web.click_count_overlong_p1_120$': 0,
            });
        });

        it('Клик в инпут + очистка', async function() {
            await this.browser.click(PO.translate.textArea());
            await this.browser.click(clearButton);

            await this.browser.yaCheckMetrics({
                'daily.total_session_count': 1,
                'web.abandonment': 100,
                'web.tech_part_misc_click': 100,
                'web.total_all_click': 3,
                'web.total_dynamic_click_count': 3,
                'web.total_request_count': 1,
                'web.total_title_click': 0,
                'web.click_count_overlong_p1_120$': 0,
            });
        });

        it('Клик в выдачу перевода', async function() {
            const clicksCount = this.currentPlatform === 'touch-phone' ? 2 : 1;

            await this.browser.click(PO.translate.resultDict());
            await this.browser.yaCheckMetrics({
                'daily.total_session_count': 1,
                'web.abandonment': 100,
                'web.tech_part_misc_click': 100,
                'web.total_all_click': clicksCount,
                'web.total_dynamic_click_count': clicksCount,
                'web.total_request_count': 1,
                'web.total_title_click': 0,
                'web.click_count_overlong_p1_120$': 0,
            });
        });

        describe('Фактовый к-к', function() {
            beforeEach(async function() {
                await this.browser.yaOpenSerp({
                    text: 'foreverdata',
                    foreverdata: 3127763789,
                    data_filter: 'translate',
                }, PO.factTranslate());
            });

            it('Клик в синюю ссылку', async function() {
                await this.browser.yaCheckBaobabServerCounter({
                    path: '/$page/$main/$result[@wizard_name="translate" and @subtype="fact"]',
                });

                await this.browser.yaCheckBaobabCounter(PO.fact.sourceLink(), {
                    path: '/$page/$main/$result/title',
                });
            });

            it('Клик в ответ', async function() {
                await this.browser.yaCheckBaobabCounter(PO.fact.answer(), {
                    path: '/$page/$main/$result/link',
                });
                // https://st.yandex-team.ru/SERP-108244
                // .yaCheckMetrics({
                //     'web.total_request_count': 2,
                //     'web.abandonment': 100,
                //     'web.total_all_click': 1,
                //     'web.total_dynamic_click_count': 0,
                //     'web.tech_part_misc_click': 100,
                // });
            });
        });
    });

    describe('Проверки в Турции', function() {
        beforeEach(async function() {
            await this.browser.yaOpenSerp({
                text: 'cat çeviri',
                tld: 'com.tr',
                lr: 11508,
                l10n: 'tr',
                data_filter: 'translate',
            }, PO.translate());
        });

        it('При вводе слова \'dog\' появляется результат с переводом', async function() {
            await this.browser.click(clearButton);
            await this.browser.setValue(PO.translate.textArea.control(), 'dog');

            // execute нужен, чтобы убрать фокус и каретку в инпуте со скриншота
            await this.browser.execute(function(input) {
                window.$(input).blur();
            }, PO.translate.textArea.control());

            await this.browser.yaWaitForVisible(PO.translate.resultDict(), 'Не появился результат с переводом');
            const text = await this.browser.getText(PO.translate.result());
            assert.include(text, 'köpek', 'В результате нет перевода');
        });

        it('При смене языков местами значение инпута изменяется', async function() {
            await this.browser.yaCheckBaobabCounter(PO.translate.swapLangsButton(), {
                path: '/$page/$main/$result/translate-form/change',
                behaviour: { type: 'dynamic' },
            });

            await this.browser.yaWaitUntil('Результат с переводом не обновился', function() {
                return this.getValue(PO.translate.textArea.control()).then(value => value === 'kediler');
            });
        });
    });
});
