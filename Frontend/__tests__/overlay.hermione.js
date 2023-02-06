const { URL } = require('url');
const getPlatformByBrowser = require('../../../hermione/utils/get-platform-by-browser');

hermione.only.notIn(['chrome-desktop', 'firefox'], 'Оверлей не открывается на десктопе');
specs({
    feature: 'Overlay',
}, () => {
    const hooksNames = {
        onOpen: 'onOverlayOpen',
        onClose: 'onOverlayClose',
        onContentShown: 'onOverlayContentShown',
        onFallback: 'onOverlayFallback',

        onNavigate: 'onOverlayNavigate',
        onRelocate: 'onOverlayRelocate',
        onNewFrameOpened: 'onOverlayNewFrameOpened',

        onActiveDocumentChanged: 'onOverlayActiveDocumentChanged',
        onPageChanged: 'onOverlayPageChanged',

        onHeaderHidden: 'onOverlayHeaderHidden',
        onHeaderVisible: 'onOverlayHeaderVisible',

        onCloseButtonClick: 'onOverlayCloseButtonClick',
        onBackButtonClick: 'onOverlayBackButtonClick',
    };

    const trackHooks = async browser => {
        await browser.execute(function(hooks) {
            window.__hooks_store__ = [];
            hooks.forEach(function(hookName) {
                window.Ya.turboOverlay[hookName] = function() {
                    window.__hooks_store__.push({ hookName: hookName, args: Array.from(arguments) });
                };
            });
        }, Object.values(hooksNames));
    };

    const isHookCalled = async(browser, waitedHookName, error) => {
        const { value: hooks } = await browser.execute(function() { return window.__hooks_store__ });

        assert.isTrue(
            hooks.some(({ hookName }) => hookName === waitedHookName),
            error
        );
    };

    const getFilteredUrl = href => {
        const url = new URL(href, 'https://yandex.ru');

        ['tpid', 'testRunId', 'exp_flags', 'nomooa', 'report', 'overlay-drawer', 'srcrwr', 'renderer_export', 'renderer_render_on_export', 'waitall'].forEach(param => {
            url.searchParams.delete(param);
        });

        // гермиона зачем-то добавляет неудалимый параметр +=1
        return `${url.pathname}${url.search}`.replace('&+=1', '');
    };

    const moveToFrame = async(browser, selector = 'iframe') => {
        const { value } = await browser.element(selector);
        await browser.frame(value);
    };

    const waitForVisibleOutside = async(browser, selector = 'iframe') => {
        await moveToFrame(browser, selector);
        await browser.yaWaitForVisible(PO.page(), 'Страница с turbo-урлом не загрузилась');
        await browser.frameParent();
    };

    const getUrlAndTitle = async browser => {
        const { value } = await browser.execute(function() {
            return {
                href: location.href,
                title: document.title,
            };
        });

        return value;
    };

    const checkUrls = async(browser, num) => {
        await waitForVisibleOutside(browser);

        await browser.assertView('plain_' + num, PO.turboOverlay());
        const { href } = await getUrlAndTitle(browser);
        assert.strictEqual(getFilteredUrl(href), '/turbo?stub=page%2Fpovarenok-infinite-1.json');

        const { value: src } = await browser.execute(function() {
            return document.querySelector('iframe').getAttribute('src');
        });

        assert.strictEqual(getFilteredUrl(src), '/turbo?stub=page%2Fpovarenok-infinite-1.json&parent-reqid=100&new_overlay=1');
    };

    async function getOverlayItemsNumber(browser) {
        return await browser.execute(function(bodySelector, bodyVisibleSelector, iframeSelector) {
            var $ = document.querySelectorAll.bind(document);

            return {
                body: $(bodySelector).length,
                visibleBody: $(bodyVisibleSelector).length,
                iframe: $(iframeSelector).length,
            };
        }, PO.turboOverlay.body(), PO.turboOverlay.bodyVisible(), PO.turboOverlay.iframe());
    }

    hermione.only.notIn('safari13');
    it('Открытие и закрытие оверлея', async function() {
        const browser = this.browser;
        await browser.url('/overlay?urls=/turbo?stub=page%2Fpovarenok-infinite-1.json');
        trackHooks(browser);
        const { href: initialUrl } = await getUrlAndTitle(browser);

        await browser.click('a');

        await checkUrls(browser, 1);

        await browser.back();
        await browser.yaWaitForHidden(PO.turboOverlay());

        const { href } = await getUrlAndTitle(browser);
        assert.strictEqual(initialUrl, href, 'Урл не вернулся при шаге назад');

        await browser.forward();
        await checkUrls(browser, 2);
    });

    hermione.only.notIn('safari13');
    it('Открытие и закрытие оверлея с хуком, заменяющим историю', async function() {
        const browser = this.browser;
        await browser.url('/overlay?urls=/turbo?text=about');
        trackHooks(browser);
        const { href: initialUrl } = await getUrlAndTitle(browser);

        await browser.execute(function() {
            window.Ya.turboOverlay.onOverlayContentShown = function(params) {
                return { displayUrl: params.displayUrl + '&updated-display=1', title: '1234' + params.title };
            };
        });

        await browser.click('a');
        await isHookCalled(browser, 'onOverlayOpen', 'Хук на открытие оверлея не вызвался');

        await browser.yaWaitUntil(
            'Урл и тайтл не заменился на хук по истории',
            () => getUrlAndTitle(browser)
                .then(({ href, title }) =>
                    getFilteredUrl(href) === '/turbo?text=about&updated-display=1' &&
                        title === '1234Турбо-страницы для владельцев сайтов')
        );

        await browser.back();
        await browser.yaWaitForHidden(PO.turboOverlay());

        const { href } = await getUrlAndTitle(browser);
        assert.strictEqual(initialUrl, href, 'Урл не вернулся при шаге назад');

        await isHookCalled(browser, 'onOverlayClose', 'Хук на открытие оверлея не вызвался');
        await browser.forward();
    });

    hermione.only.notIn('safari13');
    it('Кнопка назад возвращает назад', async function() {
        const browser = this.browser;
        await browser.url('/overlay?urls=/turbo?stub=page%2Fpovarenok-infinite-1.json');
        const { href: initialUrl } = await getUrlAndTitle(browser);

        await browser.click('a');
        await checkUrls(browser, 1);

        await browser.click(PO.turboOverlay.backButton());
        await browser.yaWaitForHidden(PO.turboOverlay());

        const { href } = await getUrlAndTitle(browser);
        assert.strictEqual(initialUrl, href, 'Урл не вернулся при шаге назад');

        await browser.forward();
        await checkUrls(browser, 2);
    });

    hermione.only.notIn('safari13');
    it('Ссылки турбо-в-турбо должны работать корректно', async function() {
        const checkFirstLink = () => ({ href, title }) => {
            assert.strictEqual(getFilteredUrl(href), '/turbo?stub=link%2Finternal.json');
            assert.strictEqual(title, 'Пример блока link');
        };

        const checkSecondLink = () => ({ href, title }) => {
            assert.strictEqual(getFilteredUrl(href), '/turbo?text=https%3A%2F%2Fen.wikipedia.org%2Fwiki%2FPrivate_school');
            assert.strictEqual(title, 'Private school');
        };

        const browser = this.browser;

        await browser.url('overlay?urls=/turbo?stub=link/react-internal.json');
        await browser.click('a');
        await waitForVisibleOutside(browser);

        await getUrlAndTitle(browser).then(checkFirstLink);
        await browser.assertView('FirstPage', PO.turboOverlay());

        await moveToFrame(browser);
        await browser.click(PO.link());
        await browser.yaWaitForVisible(PO.page());
        await browser.frameParent();

        await getUrlAndTitle(browser).then(checkFirstLink);
        await browser.assertView('Wiki', PO.turboOverlay());

        await browser.back();
        await waitForVisibleOutside(browser);
        await getUrlAndTitle(browser).then(checkFirstLink);

        await browser.forward();
        await waitForVisibleOutside(browser);
        await getUrlAndTitle(browser).then(checkSecondLink);
        await browser.assertView('WikiBack', PO.turboOverlay());
    });

    hermione.only.notIn('safari13');
    it('Замена урла при переходе по ссылкам работает корректно', async function() {
        const browser = this.browser;

        await browser.url('overlay?urls=/turbo?text=bin:selection-filter-reload');
        const { href: initialUrl } = await getUrlAndTitle(browser);
        await browser.click('a');
        await waitForVisibleOutside(browser);
        await moveToFrame(browser);

        // инитим контрол, иначе не отработает событие
        await browser.click(PO.select.control());
        await browser.selectByValue(PO.select.control(), 'https://en.wikipedia.org/wiki/Private_school');
        await browser.yaWaitForVisible(PO.page());
        await browser.frameParent();

        const { href: turboHref, title: turboTitle } = await getUrlAndTitle(browser);

        assert.strictEqual(
            getFilteredUrl(turboHref),
            '/turbo?text=https%3A%2F%2Fen.wikipedia.org%2Fwiki%2FPrivate_school'
        );

        assert.strictEqual(turboTitle, 'Private school');

        await browser.back();
        const { href: returnHref, title: returnTitle } = await getUrlAndTitle(browser);

        assert.strictEqual(returnHref, initialUrl);
        assert.strictEqual(returnTitle, 'Iframe');
    });

    hermione.only.notIn('safari13');
    it('Должен фолбэчить на оригинал', async function() {
        const browser = this.browser;

        await browser.url('overlay?urls=/turbo?text=nodata&turbo-fallback-timeout=100000000');
        await browser.click('a');
        await browser.yaWaitUntil(
            'Не произошел фолбэк на оригинал',
            () => getUrlAndTitle(browser).then(({ href }) => href === 'https://hamster.yandex.ru/turbo')
        );
    });

    hermione.only.notIn('safari13');
    it('Хук на фолбэк работает корректно', async function() {
        const browser = this.browser;

        await browser.url('overlay?urls=/turbo?text=nodata');
        await browser.execute(function() {
            window.Ya.turboOverlay.onOverlayFallback = function(params) {
                window.__fallback_hook_params__ = params;
                return { preventDefault: true };
            };
        });
        await browser.click('a');

        await browser.pause(5000);
        const { href } = await getUrlAndTitle(browser);

        assert.strictEqual(getFilteredUrl(href), '/turbo?text=nodata');

        await browser.execute(function() {
            var params = window.__fallback_hook_params__;
            params.callback(params.originalUrl + '?text=about');
        });

        await browser.yaWaitUntil(
            'Не произошел фолбэк на оригинал',
            () => getUrlAndTitle(browser).then(({ href }) => href === 'https://hamster.yandex.ru/turbo?text=about')
        );
    });

    hermione.only.notIn('safari13');
    it('Должен фолбэчить по таймауту на некорректном домене', async function() {
        const browser = this.browser;

        await browser.url('overlay?urls=https://hamster.yandex.ru/turbo?text=test_news');
        await browser.click('a');
        await browser.yaWaitUntil(
            'Не произошел фолбэк на оригинал',
            () => getUrlAndTitle(browser).then(({ href }) => href === 'https://hamster.yandex.ru/turbo')
        );
    });

    hermione.only.notIn('safari13');
    it('Должен фолбэчить на турбо-в-турбо', async function() {
        const browser = this.browser;

        const isDesktop = getPlatformByBrowser(hermione, this.browser) === 'desktop';
        const redirectLink = `https://en${isDesktop ? '' : '.m'}.wikipedia.org/wiki/Collaborative_softwar`;

        await browser.url('overlay?urls=/turbo?text=bin:broken-navigate3');
        await browser.click('a');
        await moveToFrame(browser);
        await browser.yaIndexify(PO.link());

        await browser.click(`${PO.link()}[data-index="${1}"]`); // вторая ссылка
        await browser.frameParent();
        await browser.yaWaitUntil(
            'Не произошел фолбэк на оригинал',
            () => getUrlAndTitle(browser).then(({ href }) => href === redirectLink)
        );
    });

    hermione.only.notIn('safari13');
    it('Должен менять url при скролле бесконечной ленты', async function() {
        const browser = this.browser;

        const isDesktop = getPlatformByBrowser(hermione, this.browser) === 'desktop';

        if (isDesktop) {
            // Для desktop это иногда слишком короткие статьи, поэтому
            // уменьшим viewport
            await browser.setViewportSize({ width: 600, height: 600 });
        }

        await browser.url('overlay?urls=/turbo?text=news-infinite-1');
        await browser.click('a');
        await moveToFrame(browser);

        await browser.yaWaitForVisible(PO.page(), 'Не загрузилась первая новость');
        await browser.yaScrollPageToBottom();
        await browser.yaWaitForVisible(PO.firstRelatedAutoload(), 'Не загрузилась вторая новость');
        await browser.yaScrollAndSleep(PO.firstRelatedAutoload.image());
        await browser.frameParent();

        const { href, title } = await getUrlAndTitle(browser);
        assert.strictEqual(getFilteredUrl(href), '/turbo?text=news-infinite-2');
        assert.strictEqual(title, 'Совершенно другой title', 'Неправильный заголовок на странице');

        await moveToFrame(browser);
        await browser.yaScrollAndSleep(PO.page.resultFirst());
        await browser.frameParent();

        const { href: secondHref, title: secondTitle } = await getUrlAndTitle(browser);
        assert.strictEqual(getFilteredUrl(secondHref), '/turbo?text=news-infinite-1');
        assert.strictEqual(
            secondTitle,
            'ЕВС рассмотрит вопрос о нарушении правил "Евровидения" в Киеве после завершения конкурса',
            'Неправильный заголовок на странице'
        );
    });

    hermione.only.notIn('safari13');
    it('Показывает и скрывает крестик', async function() {
        const browser = this.browser;

        await browser.url('overlay?urls=/turbo?stub=page%2Fpovarenok-infinite-1.json');
        await browser.click('a');
        await waitForVisibleOutside(browser);

        await browser.assertView('without_cross', PO.turboOverlay());
        await browser.execute(function() { window.Ya.turboOverlay.showCloseButton() });
        await browser.assertView('with_cross', PO.turboOverlay());

        await browser.execute(function() { window.Ya.turboOverlay.hideCloseButton() });
        await browser.assertView('with_cross_hidden', PO.turboOverlay());
    });

    hermione.only.notIn('safari13');
    it('Закрывает несколько страниц по клику на крестик', async function() {
        const browser = this.browser;

        await browser.url('overlay?urls=/turbo?stub=link/internal.json');
        await browser.execute(function() { window.Ya.turboOverlay.showCloseButton() });
        const { href: initialHref } = await getUrlAndTitle(browser);

        await browser.click('a');
        await waitForVisibleOutside(browser);

        await browser.assertView('with_cross', PO.turboOverlay());

        await moveToFrame(browser);
        await browser.click('a');
        await browser.frameParent();
        await waitForVisibleOutside(browser);

        const { href: firstPageUrl } = await getUrlAndTitle(browser);
        assert.strictEqual(
            getFilteredUrl(firstPageUrl),
            '/turbo/en.wikipedia.org/s/wiki/Private_school'
        );

        await browser.click(PO.turboOverlay.closeButton());
        await browser.yaWaitForHidden(PO.turboOverlay());

        const { href: backHref } = await getUrlAndTitle(browser);

        assert.strictEqual(initialHref, backHref);

        await browser.forward();
        const { href: secondPageUrl } = await getUrlAndTitle(browser);

        assert.strictEqual(
            getFilteredUrl(secondPageUrl),
            '/turbo?stub=link%2Finternal.json'
        );
    });

    hermione.only.notIn('safari13');
    it('Открывает страницы в новом iframe по обычной ссылке под флагом на новостях', async function() {
        const browser = this.browser;

        await browser.url('overlay?urls=/turbo?stub=link%2Fnews-turbo-or-not-turbo.json&exp_flags=touch_alternate_sidebar=2');
        await browser.click('a');
        await waitForVisibleOutside(browser);

        await moveToFrame(browser);
        await browser.yaIndexify(PO.link());

        await browser.execute(function() { window.I_WAS_HERE = true });
        await browser.click(`${PO.link()}[data-index="${4}"]`);
        await browser.frameParent();

        await browser.yaWaitUntil('Не было перехода по турбо-ссылке', () =>
            getUrlAndTitle(browser).then(({ href }) => getFilteredUrl(href) === '/turbo?text=about')
        );

        const { value } = await getOverlayItemsNumber(browser);

        assert.strictEqual(value.body, 2, 'На странице нет необходимого количества тел оверлея');
        assert.strictEqual(value.iframe, 2, 'На странице нет необходимого количества айфреймов');
        assert.strictEqual(value.visibleBody, 1, 'Видимо больше одного iframe');

        await moveToFrame(browser, PO.turboOverlay.bodyVisible.iframe());
        const { href: frameUrl } = await getUrlAndTitle(browser);

        assert.strictEqual(getFilteredUrl(frameUrl), '/turbo?text=about&new_overlay=1&depth=1', 'Урл видимого iframe неправильный');

        await browser.frameParent();
        await browser.back();
        await browser.yaWaitUntil('Браузер не вернулся назад', () =>
            getUrlAndTitle(browser).then(({ href }) => getFilteredUrl(href) === '/turbo?stub=link%2Fnews-turbo-or-not-turbo.json')
        );

        const { value: valueOnReturn } = await getOverlayItemsNumber(browser);

        assert.strictEqual(valueOnReturn.body, 1, 'На странице нет необходимого количества тел оверлея');
        assert.strictEqual(valueOnReturn.iframe, 1, 'На странице нет необходимого количества айфреймов');
        assert.strictEqual(valueOnReturn.visibleBody, 1, 'Видимо больше одного iframe');

        await moveToFrame(browser, PO.turboOverlay.bodyVisible.iframe());
        const { href: returnFrameUrl } = await getUrlAndTitle(browser);

        assert.strictEqual(
            getFilteredUrl(returnFrameUrl),
            '/turbo?stub=link%2Fnews-turbo-or-not-turbo.json&new_overlay=1&depth=0',
            'Урл видимого iframe неправильный при  возврате назад'
        );

        const { value: isSameFrame } = await browser.execute(function() { return window.I_WAS_HERE });
        assert.isTrue(isSameFrame, 'Iframe после возврата назад загрузился заново');
    });

    hermione.only.notIn('safari13');
    it('Открывает страницы в новом iframe по реактовой ссылке под флагом на новостях', async function() {
        const browser = this.browser;

        await browser.url('overlay?urls=/turbo?stub=link%2Fnews-turbo-or-not-turbo.json&exp_flags=touch_alternate_sidebar=2');
        await browser.click('a');
        await waitForVisibleOutside(browser);

        await moveToFrame(browser);
        await browser.yaIndexify(PO.link());

        await browser.execute(function() { window.I_WAS_HERE = true });
        await browser.click(`${PO.link()}[data-index="${5}"]`);
        await browser.frameParent();
        await browser.yaWaitUntil('Не было перехода по турбо-ссылке', () =>
            getUrlAndTitle(browser).then(({ href }) => getFilteredUrl(href) === '/turbo?text=about')
        );

        const { value } = await getOverlayItemsNumber(browser);

        assert.strictEqual(value.body, 2, 'На странице нет необходимого количества тел оверлея');
        assert.strictEqual(value.iframe, 2, 'На странице нет необходимого количества айфреймов');
        assert.strictEqual(value.visibleBody, 1, 'Видимо больше одного iframe');

        await moveToFrame(browser, PO.turboOverlay.bodyVisible.iframe());
        const { href: frameUrl } = await getUrlAndTitle(browser);

        assert.strictEqual(getFilteredUrl(frameUrl), '/turbo?text=about&new_overlay=1&depth=1', 'Урл видимого iframe неправильный');

        await browser.frameParent();
        await browser.back();
        await browser.yaWaitUntil('Браузер не вернулся назад', () =>
            getUrlAndTitle(browser).then(({ href }) => getFilteredUrl(href) === '/turbo?stub=link%2Fnews-turbo-or-not-turbo.json')
        );

        const { value: valueOnReturn } = await getOverlayItemsNumber(browser);

        assert.strictEqual(valueOnReturn.body, 1, 'На странице нет необходимого количества тел оверлея');
        assert.strictEqual(valueOnReturn.iframe, 1, 'На странице нет необходимого количества айфреймов');
        assert.strictEqual(valueOnReturn.visibleBody, 1, 'Видимо больше одного iframe');

        await moveToFrame(browser, PO.turboOverlay.bodyVisible.iframe());
        const { href: returnFrameUrl } = await getUrlAndTitle(browser);

        assert.strictEqual(
            getFilteredUrl(returnFrameUrl),
            '/turbo?stub=link%2Fnews-turbo-or-not-turbo.json&new_overlay=1&depth=0',
            'Урл видимого iframe неправильный при  возврате назад'
        );

        const { value: isSameFrame } = await browser.execute(function() { return window.I_WAS_HERE });
        assert.isTrue(isSameFrame, 'Iframe после возврата назад загрузился заново');
    });

    hermione.only.notIn('safari13');
    it('Открывает страницы в том же iframe по обычной ссылке без флага на новостях', async function() {
        const browser = this.browser;

        await browser.url('overlay?urls=/turbo?stub=link%2Fnews-turbo-or-not-turbo.json');
        await browser.click('a');
        await waitForVisibleOutside(browser);

        await moveToFrame(browser);
        await browser.yaIndexify(PO.link());

        await browser.execute(function() { window.I_WAS_HERE = true });
        await browser.click(`${PO.link()}[data-index="${4}"]`);
        await browser.frameParent();

        await browser.yaWaitUntil('Не было перехода по турбо-ссылке', () =>
            getUrlAndTitle(browser).then(({ href }) => getFilteredUrl(href) === '/turbo?text=about')
        );

        const { value } = await getOverlayItemsNumber(browser);

        assert.strictEqual(value.body, 1, 'На странице нет необходимого количества тел оверлея');
        assert.strictEqual(value.iframe, 1, 'На странице нет необходимого количества айфреймов');
        assert.strictEqual(value.visibleBody, 1, 'Видимо больше одного iframe');

        await moveToFrame(browser, PO.turboOverlay.bodyVisible.iframe());
        const { href: frameUrl } = await getUrlAndTitle(browser);

        assert.strictEqual(getFilteredUrl(frameUrl), '/turbo?text=about&new_overlay=1&depth=1', 'Урл видимого iframe неправильный');

        await browser.frameParent();
        await browser.back();
        await browser.yaWaitUntil('Браузер не вернулся назад', () =>
            getUrlAndTitle(browser).then(({ href }) => getFilteredUrl(href) === '/turbo?stub=link%2Fnews-turbo-or-not-turbo.json')
        );

        const { value: valueOnReturn } = await getOverlayItemsNumber(browser);

        assert.strictEqual(valueOnReturn.body, 1, 'На странице нет необходимого количества тел оверлея');
        assert.strictEqual(valueOnReturn.iframe, 1, 'На странице нет необходимого количества айфреймов');
        assert.strictEqual(valueOnReturn.visibleBody, 1, 'Видимо больше одного iframe');

        await moveToFrame(browser, PO.turboOverlay.bodyVisible.iframe());
        const { href: returnFrameUrl } = await getUrlAndTitle(browser);

        assert.strictEqual(
            getFilteredUrl(returnFrameUrl),
            '/turbo?stub=link%2Fnews-turbo-or-not-turbo.json&new_overlay=1&depth=0',
            'Урл видимого iframe неправильный при  возврате назад'
        );

        const { value: isSameFrame } = await browser.execute(function() { return window.I_WAS_HERE });
        assert.isNotOk(isSameFrame, 'Iframe после возврата назад не загрузился заново');
    });

    hermione.only.notIn('safari13');
    it('Открывает страницы в том же iframe по реактовой ссылке без флага на новостях', async function() {
        const browser = this.browser;

        await browser.url('overlay?urls=/turbo?stub=link%2Fnews-turbo-or-not-turbo.json');
        await browser.click('a');
        await waitForVisibleOutside(browser);

        await moveToFrame(browser);
        await browser.yaIndexify(PO.link());

        await browser.execute(function() { window.I_WAS_HERE = true });
        await browser.click(`${PO.link()}[data-index="${5}"]`);
        await browser.frameParent();
        await browser.yaWaitUntil('Не было перехода по турбо-ссылке', () =>
            getUrlAndTitle(browser).then(({ href }) => getFilteredUrl(href) === '/turbo?text=about')
        );

        const { value } = await getOverlayItemsNumber(browser);

        assert.strictEqual(value.body, 1, 'На странице нет необходимого количества тел оверлея');
        assert.strictEqual(value.iframe, 1, 'На странице нет необходимого количества айфреймов');
        assert.strictEqual(value.visibleBody, 1, 'Видимо больше одного iframe');

        await moveToFrame(browser, PO.turboOverlay.bodyVisible.iframe());
        const { href: frameUrl } = await getUrlAndTitle(browser);

        assert.strictEqual(getFilteredUrl(frameUrl), '/turbo?text=about&new_overlay=1&depth=1', 'Урл видимого iframe неправильный');

        await browser.frameParent();
        await browser.back();
        await waitForVisibleOutside(browser);

        const { value: valueOnReturn } = await getOverlayItemsNumber(browser);
        await browser.yaWaitUntil('Браузер не вернулся назад', () =>
            getUrlAndTitle(browser).then(({ href }) => getFilteredUrl(href) === '/turbo?stub=link%2Fnews-turbo-or-not-turbo.json')
        );

        assert.strictEqual(valueOnReturn.body, 1, 'На странице нет необходимого количества тел оверлея');
        assert.strictEqual(valueOnReturn.iframe, 1, 'На странице нет необходимого количества айфреймов');
        assert.strictEqual(valueOnReturn.visibleBody, 1, 'Видимо больше одного iframe');

        await moveToFrame(browser, PO.turboOverlay.bodyVisible.iframe());
        const { href: returnFrameUrl } = await getUrlAndTitle(browser);

        assert.strictEqual(
            getFilteredUrl(returnFrameUrl),
            '/turbo?stub=link%2Fnews-turbo-or-not-turbo.json&new_overlay=1&depth=0',
            'Урл видимого iframe неправильный при  возврате назад'
        );

        const { value: isSameFrame } = await browser.execute(function() { return window.I_WAS_HERE });
        assert.isNotOk(isSameFrame, 'Iframe после возврата назад не загрузился заново');
    });

    hermione.only.notIn('safari13');
    it('Тайтл страницы', async function() {
        const browser = this.browser;
        await browser.url('overlay?urls=/turbo?stub=link%2Fnews-turbo-or-not-turbo.json');
        await browser.click('a:nth-of-type(2)');
        await waitForVisibleOutside(browser);

        await browser.assertView('with-title', PO.turboOverlay());
    });

    hermione.only.notIn('safari13');
    it('Multipage режим', async function() {
        const browser = this.browser;
        await browser.url('overlay?urls=/turbo?stub=link%2Fnews-turbo-or-not-turbo.json,/turbo?stub=link%2Fdefault.json,/turbo?stub=link%2Ftel.json');
        await browser.click('a.multipage');
        await waitForVisibleOutside(browser);

        await browser.assertView('multipage', PO.turboOverlay());
    });

    hermione.only.notIn('safari13');
    it('Multipage режим с тайтлом', async function() {
        const browser = this.browser;
        await browser.url('overlay?urls=/turbo?stub=link%2Fnews-turbo-or-not-turbo.json,/turbo?stub=link%2Fdefault.json,/turbo?stub=link%2Ftel.json');
        await browser.click('a.multipage_with-title');
        await waitForVisibleOutside(browser);

        await browser.assertView('multipage-with-title', PO.turboOverlay());
    });

    hermione.only.notIn('safari13');
    it('Перелистывание страниц в multipage-режиме', async function() {
        const browser = this.browser;
        await browser.url('overlay?urls=/turbo?stub=link%2Fnews-turbo-or-not-turbo.json,/turbo?stub=link%2Fdefault.json,/turbo?stub=link%2Ftel.json');
        await browser.click('a.multipage');
        await waitForVisibleOutside(browser);

        await browser.yaTouchScroll(PO.turboOverlay(), 5000);
        await waitForVisibleOutside(browser);
        await browser.assertView('multipage-swipe-right', PO.turboOverlay());

        await browser.yaTouchScroll(PO.turboOverlay(), -5000);
        await waitForVisibleOutside(browser);
        await browser.assertView('multipage-swipe-left', PO.turboOverlay());
    });

    hermione.only.notIn('safari13');
    it('Оверлей со скрытой шапкой выглядит корректно при открытии просмотрщика', async function() {
        const browser = this.browser;
        await browser.url('overlay?urls=/turbo?stub=page/images.json');
        await browser.click('a');
        await moveToFrame(browser);
        await browser.yaTouchScroll(PO.smallImage(), 0, 100);
        await browser.click(PO.smallImage());
        await browser.yaWaitForVisible(PO.viewer(), 500, 'Viewer не открылся');
        await browser.yaMockImages();
        await browser.frameParent();
        await browser.assertView('header-hidden', PO.turboOverlay());
    });

    describe('Drawer', () => {
        hermione.only.notIn('safari13');
        it('Открытие и закрытие оверлея', async function() {
            const browser = this.browser;
            await browser.url('/overlay?urls=/turbo?stub=page%2Fpovarenok-infinite-1.json&view-type=drawer');
            trackHooks(browser);
            const { href: initialUrl } = await getUrlAndTitle(browser);

            await browser.click('a');
            await browser.yaWaitForVisible(PO.turboOverlay());

            await checkUrls(browser, 1);

            await browser.back();
            await browser.yaWaitForHidden(PO.turboOverlay());

            const { href } = await getUrlAndTitle(browser);
            assert.strictEqual(initialUrl, href, 'Урл не вернулся при шаге назад');

            await browser.forward();
            await checkUrls(browser, 2);
        });

        hermione.only.notIn('safari13');
        it('Закрывается по post-message "close"', async function() {
            const browser = this.browser;
            await browser.url('/overlay?urls=/turbo?stub=page%2Fpovarenok-infinite-1.json&view-type=drawer');

            await browser.click('a');
            await browser.yaWaitForVisible(PO.turboOverlay());

            await browser.waitForExist('iframe');
            const { value: iframe } = await browser.execute(() => {
                return document.querySelector('iframe');
            });
            await browser.frame(iframe);
            await browser.execute(() => {
                window.top.postMessage({ action: 'close' }, '*');
            });

            await browser.yaWaitForHidden(PO.turboOverlay());
        });

        hermione.only.notIn('safari13');
        it('Оверлей пробрасывает cgi-параметр в iframe', async function() {
            const browser = this.browser;
            await browser.url('/overlay?urls=/turbo?stub=page%2Fpovarenok-infinite-1.json&view-type=drawer');

            await browser.click('a');
            await browser.yaWaitForVisible(PO.turboOverlay());

            const { value: src } = await browser.execute(() => {
                return document.querySelector('iframe').getAttribute('src');
            });

            assert.include(src, 'overlay-drawer=1', 'В ссылке нет cgi-параметра от оверлея');
        });
    });
});
