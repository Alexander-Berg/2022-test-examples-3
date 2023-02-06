'use strict';

specs({ feature: 'Футер' }, function() {
    describe('Стандартные проверки', function() {
        hermione.also.in('chrome-desktop-dark');
        it('Цвет фокуса ссылки', async function() {
            const { PO, browser } = this;

            // Ограничение, чтобы не уйти в бесконечный цикл
            const MAX_ELEMENTS_COUNT = 100;
            // Жмём Tab до тех пор пока не дойдём до ссылки
            for (let i = 0; i < MAX_ELEMENTS_COUNT; i++) {
                if (await browser.hasFocus(PO.serpFooter.region.link())) {
                    break;
                }

                await browser.yaKeyPress('TAB');
            }

            await browser.assertView('region-change-focus', PO.serpFooter.region.link(), { excludeElements: PO.serpFooter() });
        });

        hermione.also.in('chrome-desktop-dark');
        it('Отсутствие голубой рамки после клика в ссылку', async function() {
            const { PO, browser } = this;

            await browser.yaOpenSerp({
                text: 'test',
                lr: 129232,
                srcskip: 'YABS_DISTR',
                exp_flags: 'yabs_distr=0',
            }, PO.serpFooter());

            const id = await this.browser.getCurrentTabId();
            const prevTabId = id;
            await this.browser.click(PO.serpFooter.help.feedback());
            await this.browser.pause(3000);
            await this.browser.switchTab(prevTabId);

            await browser.assertView('feedback-focus-border', PO.serpFooter.help.feedback(), { excludeElements: PO.serpFooter() });
        });

        hermione.also.in('chrome-desktop-dark');
        it('Отсутствие голубой рамки после клика в ссылку в попапе', async function() {
            const { PO, browser } = this;

            await browser.yaOpenSerp({
                text: 'test',
                lr: 129232,
                srcskip: 'YABS_DISTR',
                exp_flags: 'yabs_distr=0',
            }, PO.serpFooter());

            await browser.click(PO.serpFooter.extralinks());
            await browser.yaWaitForVisible(PO.serpFooterExtralinksPopup());

            const id = await this.browser.getCurrentTabId();
            const prevTabId = id;
            await this.browser.click(PO.serpFooterExtralinksPopup.about());
            await this.browser.pause(3000);
            await this.browser.switchTab(prevTabId);

            await browser.assertView('about-focus-border', PO.serpFooterExtralinksPopup.about(), { excludeElements: PO.serpFooter() });
        });

        describe('Переключатель региона', function() {
            it('Длинное название региона', async function() {
                const { browser, PO } = this;

                await browser.yaOpenSerp({
                    text: 'test',
                    lr: 129232,
                    srcskip: 'YABS_DISTR',
                    exp_flags: 'yabs_distr=0',
                }, PO.serpFooter());

                await browser.yaScroll(PO.serpFooter());

                await browser.assertView('long-region', PO.serpFooter.region(), { excludeElements: PO.serpFooter() });
            });
        });

        describe('Поиск в других системах', function() {
            it('Переход на другие поисковые системы', async function() {
                const { PO, browser } = this;
                const text = 'test';
                await browser.yaOpenSerp({
                    text,
                    tld: 'ru',
                    l10n: 'ru',
                    data_filter: 'no_results',
                }, PO.serpFooter.searchengines());

                await browser.yaCheckLink2({
                    selector: PO.serpFooter.searchengines.firstLink(),
                    url: {
                        href: {
                            url: '//www.google.ru/search',
                            queryValidator: query => query && query.q === text,
                        },
                        ignore: ['protocol'],
                    },
                    message: 'Сломана ссылка на поиск Google',
                });

                await browser.yaCheckLink2({
                    selector: PO.serpFooter.searchengines.secondLink(),
                    url: {
                        href: {
                            url: '//www.bing.com/search',
                            queryValidator: query => query && query.q === text,
                        },
                        ignore: ['protocol'],
                    },
                    message: 'Сломана ссылка на поиск Bing',
                });

                await browser.yaCheckLink2({
                    selector: PO.serpFooter.searchengines.thirdLink(),
                    url: {
                        href: {
                            url: '//go.mail.ru/search',
                            queryValidator: query => query && query.q === text,
                        },
                        ignore: ['protocol'],
                    },
                    message: 'Сломана ссылка на поиск Mail',
                });
            });
        });

        beforeEach(async function() {
            const { browser, PO } = this;

            await browser.yaOpenSerp({
                text: 'test',
                lr: 213,
                srcskip: 'YABS_DISTR',
                exp_flags: 'yabs_distr=0',
            }, PO.serpFooter());

            await browser.yaScroll(PO.serpFooter());
        });

        describe('Обратная связь и справка', function() {
            it('Скрытие ссылок', async function() {
                const { browser, PO } = this;
                await this.browser.setViewportSize({ width: 600, height: 900 });

                await checkLinkVisibility(PO, browser, PO.serpFooter.help());
                await browser.assertView('plain', PO.serpFooter(), { excludeElements: PO.serpFooter() });
            });

            it('Ссылки в попапе', async function() {
                const { browser, PO } = this;
                await this.browser.setViewportSize({ width: 600, height: 900 });

                await browser.click(PO.serpFooter.extralinks());
                await browser.yaWaitForVisible(PO.serpFooterExtralinksPopup());

                await browser.assertView('plain', [
                    PO.serpFooterExtralinksPopup.help(),
                    PO.serpFooterExtralinksPopup.feedback(),
                ], {
                    excludeElements: PO.serpFooter(),
                });
            });

            it('Отправка счетчиков в попапе для ссылки \'Обратная связь\'', async function() {
                const { browser, PO } = this;
                await this.browser.setViewportSize({ width: 600, height: 900 });

                await browser.click(PO.serpFooter.extralinks());
                await browser.yaWaitForVisible(PO.serpFooterExtralinksPopup());

                await browser.yaCheckBaobabCounter(PO.serpFooterExtralinksPopup.feedback(), {
                    path: '/$page/$footer/feedback',
                });
            });

            it('Отправка счетчиков в попапе для ссылки \'Справка\'', async function() {
                const { browser, PO } = this;
                await this.browser.setViewportSize({ width: 600, height: 900 });

                await browser.click(PO.serpFooter.extralinks());
                await browser.yaWaitForVisible(PO.serpFooterExtralinksPopup());

                await browser.yaCheckBaobabCounter(PO.serpFooterExtralinksPopup.help(), {
                    path: '/$page/$footer/help',
                });
            });
        });

        describe('Настройки', function() {
            hermione.also.in('chrome-desktop-dark');
            it('Скрытие ссылки', async function() {
                const { browser, PO } = this;
                await this.browser.setViewportSize({ width: 800, height: 900 });

                await checkLinkVisibility(PO, browser, PO.serpFooter.settings());
                await browser.assertView('plain', PO.serpFooter(), { excludeElements: PO.serpFooter() });
            });

            it('Ссылка в попапе', async function() {
                const { browser, PO } = this;
                await this.browser.setViewportSize({ width: 800, height: 900 });

                await browser.click(PO.serpFooter.extralinks());
                await browser.yaWaitForVisible(PO.serpFooterExtralinksPopup());

                await browser.assertView('plain', PO.serpFooterExtralinksPopup.settings(), {
                    excludeElements: PO.serpFooter(),
                });
            });

            it('Отправка счетчиков в попапе', async function() {
                const { browser, PO } = this;
                await this.browser.setViewportSize({ width: 800, height: 900 });

                await browser.click(PO.serpFooter.extralinks());
                await browser.yaWaitForVisible(PO.serpFooterExtralinksPopup());

                await browser.yaCheckBaobabCounter(PO.serpFooterExtralinksPopup.settings(), {
                    path: '/$page/$footer/settings',
                });
            });
        });

        describe('Семейный поиск', function() {
            hermione.also.in('chrome-desktop-dark');
            it('Выключен', async function() {
                const { browser, PO } = this;

                await browser.assertView('family-mode-off', PO.serpFooter.familyMode(), { excludeElements: PO.serpFooter() });
            });

            hermione.also.in('chrome-desktop-dark');
            it('Включен', async function() {
                const { browser, PO } = this;

                await browser.yaOpenSerp({
                    text: 'котики',
                    lr: 213,
                    exp_flags: 'yabs_distr=0',
                    family: 1,
                }, PO.serpFooter());

                await browser.yaScroll(PO.serpFooter());

                await browser.assertView('family-mode-on', PO.serpFooter.familyMode(), { excludeElements: PO.serpFooter() });
            });

            it('Отправка счетчиков', async function() {
                const { browser, PO } = this;
                await browser.yaCheckBaobabCounter(PO.serpFooter.familyMode(), {
                    path: '/$page/$footer/family-mode',
                });
            });

            it('Скрытие ссылки', async function() {
                const { browser, PO } = this;
                await browser.setViewportSize({ width: 1000, height: 900 });

                await checkLinkVisibility(PO, browser, PO.serpFooter.familyMode());
                await browser.assertView('plain', PO.serpFooter(), { excludeElements: PO.serpFooter() });
            });

            it('Ссылка в попапе', async function() {
                const { browser, PO } = this;
                await browser.setViewportSize({ width: 1000, height: 900 });

                await browser.click(PO.serpFooter.extralinks());
                await browser.yaWaitForVisible(PO.serpFooterExtralinksPopup());

                await browser.assertView('plain', PO.serpFooterExtralinksPopup.familyMode(), {
                    excludeElements: PO.serpFooter(),
                });
            });

            it('Отправка счетчиков в попапе', async function() {
                const { browser, PO } = this;
                await browser.setViewportSize({ width: 1000, height: 900 });

                await browser.click(PO.serpFooter.extralinks());
                await browser.yaWaitForVisible(PO.serpFooterExtralinksPopup());

                await browser.yaCheckBaobabCounter(PO.serpFooterExtralinksPopup.familyMode(), {
                    path: '/$page/$footer/family-mode',
                });
            });
        });

        describe('О компании', function() {
            it('Внешний вид', async function() {
                const { browser, PO } = this;

                await browser.click(PO.serpFooter.extralinks());
                await browser.yaWaitForVisible(PO.serpFooterExtralinksPopup());

                await browser.assertView('about', PO.serpFooterExtralinksPopup.about(), {
                    excludeElements: PO.serpFooter(),
                });
            });

            it('Отправка счетчиков', async function() {
                const { browser, PO } = this;

                await browser.click(PO.serpFooter.extralinks());
                await browser.yaWaitForVisible(PO.serpFooterExtralinksPopup());

                await browser.yaCheckBaobabCounter(PO.serpFooterExtralinksPopup.about(), {
                    path: '/$page/$footer/about',
                });
            });
        });

        describe('Вакансии', function() {
            it('Внешний вид', async function() {
                const { browser, PO } = this;

                await browser.click(PO.serpFooter.extralinks());
                await browser.yaWaitForVisible(PO.serpFooterExtralinksPopup());

                await browser.assertView('vacancy', PO.serpFooterExtralinksPopup.vacancy(), {
                    excludeElements: PO.serpFooter(),
                });
            });

            it('Отправка счетчиков', async function() {
                const { browser, PO } = this;

                await browser.click(PO.serpFooter.extralinks());
                await browser.yaWaitForVisible(PO.serpFooterExtralinksPopup());

                await browser.yaCheckBaobabCounter(PO.serpFooterExtralinksPopup.vacancy(), {
                    path: '/$page/$footer/vacancy',
                });
            });
        });

        describe('Кебаб-меню', function() {
            hermione.also.in('chrome-desktop-dark');
            it('Внешний вид', async function() {
                const { browser, PO } = this;

                await browser.click(PO.serpFooter.extralinks());
                await browser.yaWaitForVisible(PO.serpFooterExtralinksPopup());

                await browser.assertView('plain', PO.serpFooterExtralinksPopup(), { excludeElements: PO.serpFooter() });
            });

            it('С семейным поиском', async function() {
                await checkPopupItems(this.PO, this.browser, { width: 1000, height: 900 }, 3, [
                    { selector: this.PO.serpFooterExtralinksPopup.familyMode(), label: 'Включить семейный поиск' },
                ]);
            });

            it('С настройками', async function() {
                await checkPopupItems(this.PO, this.browser, { width: 800, height: 900 }, 4, [
                    { selector: this.PO.serpFooterExtralinksPopup.settings(), label: 'Настройки' },
                ]);
            });

            it('С обратной связью и справкой', async function() {
                await checkPopupItems(this.PO, this.browser, { width: 600, height: 900 }, 6, [
                    { selector: this.PO.serpFooterExtralinksPopup.feedback(), label: 'Обратная связь' },
                    { selector: this.PO.serpFooterExtralinksPopup.help(), label: 'Справка' },
                ]);
            });
        });
    });

    it('Широкий десктоп', async function() {
        const { browser, PO } = this;
        await browser.yaOpenSerp({
            text: 'test',
            lr: 213,
            srcskip: 'YABS_DISTR',
            exp_flags: 'yabs_distr=0',
        }, PO.serpFooter());

        await browser.yaScroll(PO.serpFooter());

        await this.browser.setViewportSize({ width: 1600, height: 900 });

        await browser.assertView('plain', [PO.pager(), PO.serpFooter()], { excludeElements: PO.serpFooter() });
    });
});

async function checkLinkVisibility(PO, browser, linkSelector) {
    let targetOffsetTop;
    let comparedOffsetTop;
    await browser.execute((selector, targetSelector) => {
        targetOffsetTop = document.querySelector(targetSelector).offsetTop;
        comparedOffsetTop = document.querySelector(selector).offsetTop;
    }, linkSelector, PO.serpFooter.content());

    assert.equal(targetOffsetTop, comparedOffsetTop, 'При данной ширине элемент должен быть скрыт');
}

async function checkPopupItems(PO, browser, viewportSize, linksCount, checkSelectors) {
    await browser.setViewportSize(viewportSize);

    await browser.click(PO.serpFooter.extralinks());
    await browser.yaWaitForVisible(PO.serpFooterExtralinksPopup());

    const count = await browser.yaVisibleCount(PO.serpFooterExtralinksPopup.item());
    assert.equal(count, linksCount, `В попапе должно быть следующее количество ссылок: ${linksCount}`);

    checkSelectors.map(async value => {
        const linkLabel = await browser.getText(value.selector);
        assert.equal(linkLabel, value.label, `В попапе должна быть ссылка: ${value.label}`);
    });

    await browser.assertView('plain', PO.serpFooterExtralinksPopup(), {
        excludeElements: PO.serpFooter(),
        invisibleElements: PO.contentLeft(),
    });
}
