const docsObjects = require('../page-objects/docs');
const docsViewObjects = require('../page-objects/docs-view').common;
const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const listing = require('../page-objects/client-content-listing');
const { psHeader } = require('../page-objects/client');
const clientNavigation = require('../page-objects/client-navigation');
const { getDocsViewUrl, getDocsUrl, DOCS_SECTION_TITLES } = require('../helpers/docs');
const getUser = require('@ps-int/ufo-hermione/helpers/getUser')(require('../config').login);
const { consts } = require('../config');
const { assert } = require('chai');
const { performance } = require('perf_hooks');
const { overdraftContent } = require('../page-objects/client');

/**
 * Ожидает открыие документа
 *
 * @param {boolean} isPrivateDoc
 * @param {boolean} [hasNoImages]
 * @param {boolean} [isReadOnly]
 * @returns {Promise<void>}
 */
async function yaWaitDocumentOpened(isPrivateDoc, hasNoImages, isReadOnly = false) {
    const bro = this.browser;

    await bro.yaWaitForHidden(docsViewObjects.docsViewLoading(), 10000);

    const iframe = await bro.$(docsViewObjects.docsViewFrame());

    await bro.switchToFrame(iframe);
    await bro.yaWaitForVisible(docsViewObjects.docviewer.page(), 10000);

    if (!hasNoImages) {
        await bro.yaWaitPreviewsLoaded(docsViewObjects.docviewer.page.img(), true);
    }

    if (!isReadOnly) {
        await bro.yaWaitForVisible(
            isPrivateDoc ?
                docsViewObjects.docviewerEditButton() :
                docsViewObjects.docviewerEditCopyButton()
        );
    }

    await bro.switchToParentFrame();
}

/**
 * Закрашивает чёрным верхний блок рекламы во фрейме
 * Нижний блок скрывает, тк он рисуется не всегда и у него непостоянная высота - всё это влияет на высоту скроллбара
 * Обычный `ignoreElement` не работает, так как он умеет только в пределах 1 страницы, а во фреймы не заходит
 *
 * @param {boolean} [hideTopDirect]
 * @returns {Promise<void>}
 */
async function waitAndIgnoreDirectInIframe(hideTopDirect) {
    const bro = this.browser;
    await bro.pause(1000); // пересчёт размеров рекламы

    const iframe = await bro.$(docsViewObjects.docsViewFrame());

    await bro.frame(iframe);

    await bro.execute(async (pageSelector, hideTopDirect) => {
        const pages = document.querySelectorAll(pageSelector);

        const firstPage = pages[0];
        const lastPage = pages[pages.length - 1];
        let topDirectBlock;
        let bottomDirectBlock;
        if (Array.from(firstPage.classList).some((cls) => cls.startsWith('pageWithoutSize'))) {
            topDirectBlock = firstPage.parentElement.previousSibling;
            bottomDirectBlock = lastPage.parentElement.nextSibling;
        } else {
            topDirectBlock = firstPage.previousSibling;
            bottomDirectBlock = lastPage.nextSibling;
        }

        /**
         * класс рекламного блока - динамический, чтобы обходить AdBlock
         * Поэтому ищем блок ненулевой высоты перед первой страницей - это и будет реклама
         */
        while (topDirectBlock && topDirectBlock.clientHeight === 0) {
            topDirectBlock = topDirectBlock.previousSibling;
        }

        while (
            bottomDirectBlock &&
            (bottomDirectBlock.clientHeight === 0 || bottomDirectBlock.className.includes('sliderButtonsWrapper_'))
        ) {
            bottomDirectBlock = bottomDirectBlock.nextSibling;
        }

        if (topDirectBlock) {
            if (hideTopDirect) {
                topDirectBlock.style.display = 'none';
            } else {
                // дождёмся отрисовки блока рекламы - после отрисовки он меняет размеры
                while (!topDirectBlock.hasChildNodes()) {
                    await new Promise((resolve) => {
                        setTimeout(resolve, 250);
                    });
                }

                topDirectBlock.style.position = 'relative';

                const blackOverlay = document.createElement('div');
                blackOverlay.style.position = 'absolute';
                blackOverlay.style.top = '0';
                blackOverlay.style.left = '0';
                blackOverlay.style.height = '100%';
                blackOverlay.style.width = '100%';
                blackOverlay.style.zIndex = '100';
                blackOverlay.style.backgroundColor = 'black';
                topDirectBlock.appendChild(blackOverlay);
            }
        }

        if (bottomDirectBlock) {
            bottomDirectBlock.style.display = 'none';
        }
    }, docsViewObjects.docviewer.page(), hideTopDirect);

    await bro.switchToParentFrame();
}

/**
 * @typedef {Object} OpenDocumentParams
 * @property {string} url
 * @property {string} [mame]
 * @property {{ auth?: boolean, login?: string, uid?: string }} [user]
 */

/**
 * Открывает документ по переданному URL-у и дожидается загрузки документа
 *
 * @param {OpenDocumentParams} params
 * @param {boolean} [params.hasNoImages]
 * @returns {Promise<void>}
 */
async function openDocument({
    url,
    name = url.split('/').pop(),
    user = { auth: true, login: 'yndx-ufo-test-627', uid: '1013657710' },
    hasNoImages = false,
    isReadOnly = false
}) {
    const bro = this.browser;

    if (user.auth) {
        await bro.yaClientLoginFast(user.login);
    }
    const isPrivateDoc = url.startsWith('ya-disk:');
    await bro.url(getDocsViewUrl(bro, {
        url,
        name,
        ...(isPrivateDoc ? {
            uid: user.uid
        } : null)
    }));

    await yaWaitDocumentOpened.call(this, isPrivateDoc, hasNoImages, isReadOnly);
}

/**
 * Тест с открытием документа и его скриншотом
 *
 * @param {OpenDocumentParams} params
 * @param {boolean} [params.shouldIgnoreDirect]
 * @param {boolean} [params.hideTopDirect]
 * @returns {Promise<void>}
 */
async function assertDocumentView({
    url,
    name,
    user,
    shouldIgnoreDirect = true,
    hideTopDirect,
    hasNoImages,
    isReadOnly
}) {
    await openDocument.call(this, { url, name, user, hasNoImages, isReadOnly });

    if (shouldIgnoreDirect) {
        await waitAndIgnoreDirectInIframe.call(this, hideTopDirect);
    }

    await this.browser.yaAssertView(this.testpalmId, docsObjects.common.docsPage());
}

/**
 * Возвращает top-позицию элемента по переданному селектору
 *
 * @param {string} selector
 * @returns {Promise<number>}
 */
async function getElementTopPosition(selector) {
    const bro = this.browser;
    const result = await bro.execute((elemSelector) => {
        return document.querySelector(elemSelector).getBoundingClientRect().top;
    }, selector);
    return result;
}

hermione.only.in(clientDesktopBrowsersList); // DV в Доксах только на десктопах
describe('Отображение документов в обвязке Доксов -> ', () => {
    it('diskclient-6800: Открытие / Отображение личного документа (.docx) в DV', async function () {
        this.testpalmId = 'diskclient-6800';

        await assertDocumentView.call(this, { url: 'ya-disk:///disk/Ластоногие.docx' });
    });

    it('diskclient-6762: Отображение личной таблицы (.xlsx) в DV', async function () {
        this.testpalmId = 'diskclient-6762';

        await assertDocumentView.call(this, { url: 'ya-disk:///disk/SWIMWEAR_COLLECTION_2014_ladies.xlsx' });
    });

    it('diskclient-6763: Отображение личной презентации (.pptx) в DV', async function () {
        this.testpalmId = 'diskclient-6763';

        await assertDocumentView.call(this, { url: 'ya-disk:///disk/OpenDocument Presentation.pptx' });
    });

    it('diskclient-6802: Отображение документа в DV из паблика', async function () {
        this.testpalmId = 'diskclient-6802';

        // владелец документа - `yndx-ufo-test-628`
        await assertDocumentView.call(
            this,
            {
                // eslint-disable-next-line max-len
                url: 'ya-disk-public://fNRHWcIgJbdevzd7FbhH/4NBXQDomSpPfGvDzPylMfTQE9EmkQe+4/uOwAoxJJEWq/J6bpmRyOJonT3VoXnDag==',
                name: 'Гваделупский енот.docx'
            }
        );
    });

    hermione.only.notIn('chrome-desktop', 'в хроме мигает высота скроллбара и шрифт на кнопке "Сохранить"');
    it('diskclient-6765: Отображение таблицы в DV из паблика', async function () {
        this.testpalmId = 'diskclient-6765';

        // владелец документа - `yndx-ufo-test-628`
        await assertDocumentView.call(
            this,
            {
                // eslint-disable-next-line max-len
                url: 'ya-disk-public://uSPZMAdAjGFnb3cOz0/qoHYBZ8UejvUAPfo2J/LpHV5WLSZoukmwDi+2z8dfgtBHq/J6bpmRyOJonT3VoXnDag==',
                name: 'Таблица перевода REIZ.xlsx'
            }
        );
    });

    it('diskclient-6766: Отображение презентации в DV из паблика', async function () {
        this.testpalmId = 'diskclient-6766';

        // владелец документа - `yndx-ufo-test-628`
        await assertDocumentView.call(
            this,
            {
                // eslint-disable-next-line max-len
                url: 'ya-disk-public://Xo9y+hmxuNd9dshaLKN/ClUz9dvsRXBo94xaqeiU+VTIkzVeEKbJ8O572lDCXv40q/J6bpmRyOJonT3VoXnDag==',
                name: '10-25-1-kompjuternye-prezentacii.pptx 10-25-1-kompjuternye-prezentacii.pptx'
            }
        );
    });

    it('diskclient-6773: Отображение чужого документа в DV из Доксов (+ diskclient-6807: Открытие чужого документа в DV из Доксов)', async function () {
        this.testpalmId = 'diskclient-6773';

        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-627');

        await bro.url(getDocsUrl(bro) + '/docs?type=docx');

        await bro.yaWaitForVisible(docsObjects.common.docsListing());
        // владелец документа - `yndx-ufo-test-628`, ссылка - https://disk.yandex.ru/i/jZ4FjbjSOG25Ig
        await bro.yaCallActionInActionPopup('Енот-ракоед.docx', 'view');

        await yaWaitDocumentOpened.call(this, false);

        await waitAndIgnoreDirectInIframe.call(this);
        await bro.yaAssertView(this.testpalmId, docsObjects.common.docsPage());
    });

    it('diskclient-dv-in-docs-for-unauth: Отображение документа в DV из паблика, для неавторизованного пользователя', async function () {
        this.testpalmId = 'diskclient-dv-in-docs-for-unauth';

        // владелец документа - `yndx-ufo-test-628`
        await assertDocumentView.call(
            this,
            {
                // eslint-disable-next-line max-len
                url: 'ya-disk-public://fNRHWcIgJbdevzd7FbhH/4NBXQDomSpPfGvDzPylMfTQE9EmkQe+4/uOwAoxJJEWq/J6bpmRyOJonT3VoXnDag==',
                name: 'Гваделупский енот.docx',
                user: { auth: false },
                // у неавторизованного реклама может отрисоваться, а может нет
                // вероятно, это связано с отсутствием рекламных рекомендаций для пустого пользователя
                hideTopDirect: true
            }
        );
    });

    it('diskclient-6783: Скролл документов', async function () {
        this.testpalmId = 'diskclient-6783';

        await openDocument.call(this, { url: 'ya-disk:///disk/Ластоногие.docx' });

        const bro = this.browser;
        await bro.yaAssertInViewport(psHeader());

        const iframe = await bro.$(docsViewObjects.docsViewFrame());
        await bro.switchToFrame(iframe);

        await bro.yaWaitForVisible(docsViewObjects.docviewer.title());
        let dvHeaderTop = await getElementTopPosition.call(this, docsViewObjects.docviewer.title());
        assert(dvHeaderTop > 0, 'dvHeaderTop === ' + dvHeaderTop + ', а ожидали > 0');

        await bro.yaScroll(1100);
        await bro.yaWaitForVisible(docsViewObjects.docviewer.title());
        dvHeaderTop = await getElementTopPosition.call(this, docsViewObjects.docviewer.title());
        // заголовок документа "залип"
        assert(dvHeaderTop === 0, 'dvHeaderTop === ' + dvHeaderTop + ', а ожидали === 0');

        await assert.equal(
            await bro.getText(docsViewObjects.docviewer.pageCounter.text()),
            '2 из 4'
        );

        await bro.switchToParentFrame();

        // шапка с сервисами скрылась
        await bro.yaAssertInViewport(psHeader(), false);

        await bro.frame(iframe);

        await bro.yaScroll(2200);
        await assert.equal(
            await bro.getText(docsViewObjects.docviewer.pageCounter.text()),
            '3 из 4'
        );

        await bro.yaScrollToEnd();
        await assert.equal(
            await bro.getText(docsViewObjects.docviewer.pageCounter.text()),
            '4 из 4'
        );

        await bro.yaScroll(0);
        await assert.equal(
            await bro.getText(docsViewObjects.docviewer.pageCounter.text()),
            '1 из 4'
        );

        // заголовок документа "отлип"
        dvHeaderTop = await getElementTopPosition.call(this, docsViewObjects.docviewer.title());
        assert(dvHeaderTop > 0, 'dvHeaderTop === ' + dvHeaderTop + ', а ожидали > 0');

        await bro.switchToParentFrame();

        // шапка с сервисами появилась
        await bro.yaAssertInViewport(psHeader());
    });

    hermione.skip.in(['chrome-desktop'], 'https://st.yandex-team.ru/CHEMODAN-82549');
    it('diskclient-6784: Быстрый подскролл к первой странице (кнопка "Наверх"), скролл руками', async function () {
        this.testpalmId = 'diskclient-6784';

        await openDocument.call(this, { url: 'ya-disk:///disk/1400_alb (9).docx' });

        const bro = this.browser;
        await bro.yaAssertInViewport(psHeader());

        const iframe = await bro.element(docsViewObjects.docsViewFrame());
        await bro.frame(iframe);

        const initialDVHeaderTop = await getElementTopPosition.call(this, docsViewObjects.docviewer.title());
        assert(initialDVHeaderTop > 0, 'dvHeaderTop === ' + initialDVHeaderTop + ', а ожидали > 0');

        await bro.yaScroll(20000);
        await assert.equal(
            await bro.getText(docsViewObjects.docviewer.pageCounter.text()),
            '34 из 1401'
        );
        const dvHeaderTop = await getElementTopPosition.call(this, docsViewObjects.docviewer.title());
        // заголовок документа "залип"
        assert(dvHeaderTop === 0, 'dvHeaderTop === ' + dvHeaderTop + ', а ожидали === 0');
        await bro.yaWaitForVisible(docsViewObjects.docviewer.upButton());

        await bro.switchToParentFrame();

        // шапка с сервисами пропала
        await bro.yaAssertInViewport(psHeader(), false);

        await bro.frame(iframe);

        await bro.yaClick(docsViewObjects.docviewer.upButton());

        await assert.equal(
            await bro.getText(docsViewObjects.docviewer.pageCounter.text()),
            '1 из 1401'
        );
        const newDVHeaderTop = await getElementTopPosition.call(this, docsViewObjects.docviewer.title());
        // заголовок документа "отлип" обратно
        assert(
            newDVHeaderTop === initialDVHeaderTop,
            'newDVHeaderTop === ' + newDVHeaderTop + ', а ожидали === ' + initialDVHeaderTop
        );

        await bro.switchToParentFrame();

        // шапка с сервисами появилась обратно
        await bro.yaAssertInViewport(psHeader());
    });

    hermione.only.notIn('chrome-desktop', 'в хроме либо таймаутит, либо неполностью вводит значение в пейджер');
    it('diskclient-6784: Быстрый подскролл к первой странице (кнопка "Наверх"), скролл через пейджер', async function () {
        this.testpalmId = 'diskclient-6784';

        await openDocument.call(this, { url: 'ya-disk:///disk/1400_alb (9).docx' });

        const bro = this.browser;
        await bro.yaAssertInViewport(psHeader());

        const iframe = await bro.$(docsViewObjects.docsViewFrame());
        await bro.switchToFrame(iframe);

        const initialDVHeaderTop = await getElementTopPosition.call(this, docsViewObjects.docviewer.title());
        assert(initialDVHeaderTop > 0, 'dvHeaderTop === ' + initialDVHeaderTop + ', а ожидали > 0');

        const pageToGo = 1111;
        await bro.moveToObject(docsViewObjects.docviewer.pageCounter());
        await bro.yaClick(docsViewObjects.docviewer.pageCounter());
        await bro.yaSetValue(docsViewObjects.docviewer.pageCounter.input(), pageToGo.toString());
        await bro.keys('Enter');

        await assert.equal(
            await bro.getText(docsViewObjects.docviewer.pageCounter.text()),
            pageToGo + ' из 1401'
        );
        const dvHeaderTop = await getElementTopPosition.call(this, docsViewObjects.docviewer.title());
        // заголовок документа "залип"
        assert(dvHeaderTop === 0, 'dvHeaderTop === ' + dvHeaderTop + ', а ожидали === 0');
        await bro.yaWaitForVisible(docsViewObjects.docviewer.upButton());

        await bro.switchToParentFrame();

        // шапка с сервисами пропала
        await bro.yaAssertInViewport(psHeader(), false);

        await bro.switchToFrame(iframe);

        await bro.yaClick(docsViewObjects.docviewer.upButton());

        await assert.equal(
            await bro.getText(docsViewObjects.docviewer.pageCounter.text()),
            '1 из 1401'
        );
        const newDVHeaderTop = await getElementTopPosition.call(this, docsViewObjects.docviewer.title());
        // заголовок документа "отлип" обратно
        assert(
            newDVHeaderTop === initialDVHeaderTop,
            'newDVHeaderTop === ' + newDVHeaderTop + ', а ожидали === ' + initialDVHeaderTop
        );

        await bro.switchToParentFrame();

        // шапка с сервисами появилась обратно
        await bro.yaAssertInViewport(psHeader());
    });

    it('diskclient-6789: Отображение рекламы бесплатному пользователю', async function () {
        this.testpalmId = 'diskclient-6789';

        await openDocument.call(this, { url: 'ya-disk:///disk/Ластоногие.docx' });

        const bro = this.browser;
        const iframe = await bro.$(docsViewObjects.docsViewFrame());
        await bro.switchToFrame(iframe);

        const { hasDirectBlock, directBlockHeight } = await bro.execute((pageSelector) => {
            const firstPage = document.querySelector(pageSelector);
            let directBlock;
            if (Array.from(firstPage.classList).some((cls) => cls.startsWith('pageWithoutSize'))) {
                directBlock = firstPage.parentElement.previousSibling;
            } else {
                directBlock = firstPage.previousSibling;
            }

            /**
             * класс рекламного блока - динамический, чтобы обходить AdBlock
             * Поэтому ищем блок ненулевой высоты перед первой страницей - это и будет реклама
             */
            while (directBlock && directBlock.clientHeight === 0) {
                directBlock = directBlock.previousSibling;
            }
            return {
                hasDirectBlock: Boolean(directBlock),
                directBlockHeight: directBlock ? directBlock.clientHeight : 0
            };
        }, docsViewObjects.docviewer.page());
        assert(hasDirectBlock, 'Ожидается наддичие блока рекламы');
        assert.equal(directBlockHeight, 36);
        await bro.switchToParentFrame();
    });

    it('diskclient-6790: Отсутствие рекламы платного пользователя', async function () {
        this.testpalmId = 'diskclient-6790';

        await assertDocumentView.call(this, {
            // eslint-disable-next-line max-len
            url: 'ya-disk-public://fNRHWcIgJbdevzd7FbhH/4NBXQDomSpPfGvDzPylMfTQE9EmkQe+4/uOwAoxJJEWq/J6bpmRyOJonT3VoXnDag==',
            name: 'Гваделупский енот.docx',
            user: { auth: true, login: 'yndx-ufo-test-oligarh' }
        });

        const bro = this.browser;
        const iframe = await bro.$(docsViewObjects.docsViewFrame());
        await bro.switchToFrame(iframe);
        await bro.yaScrollToEnd();
        await bro.switchToParentFrame();

        await bro.yaAssertView(this.testpalmId + '-bottom', docsObjects.common.docsPage());
    });
});

hermione.only.in(clientDesktopBrowsersList); // DV в Доксах только на десктопах
describe('Авторизация в документах в Доксах -> ', () => {
    it('diskclient-6792: Переключение пользователя на личном документе', async function () {
        this.testpalmId = 'diskclient-6792';

        await openDocument.call(this, { url: 'ya-disk:///disk/Ластоногие.docx' });

        const bro = this.browser;
        await bro.yaSecondUserAuthorize(getUser('yndx-ufo-test-629'));

        const iframe = await bro.$(docsViewObjects.docsViewFrame());

        await bro.switchToFrame(iframe);
        await bro.yaAssertView(this.testpalmId, docsViewObjects.docviewer.viewport());
        await bro.switchToParentFrame();
    });

    it('diskclient-6793: Авторизация на странице публичного документа', async function () {
        this.testpalmId = 'diskclient-6793';

        await openDocument.call(this, {
            // eslint-disable-next-line max-len
            url: 'ya-disk-public://uSPZMAdAjGFnb3cOz0/qoHYBZ8UejvUAPfo2J/LpHV5WLSZoukmwDi+2z8dfgtBHq/J6bpmRyOJonT3VoXnDag==',
            name: 'Таблица перевода REIZ.xlsx',
            user: { auth: false }
        });

        const bro = this.browser;
        await bro.yaWaitForVisible(psHeader.loginButton());
        await bro.yaClick(psHeader.loginButton());
        await bro.login(getUser('yndx-ufo-test-629'));

        await bro.yaWaitForHidden(psHeader.loginButton());
        await bro.yaWaitForVisible(psHeader.legoUser());

        await yaWaitDocumentOpened.call(this, false);
    });
});

hermione.only.in(clientDesktopBrowsersList); // DV в Доксах только на десктопах
describe('Переходы к просмотру документа в Доксах -> ', () => {
    afterEach(async function () {
        const { items } = this.currentTest.ctx;
        if (Array.isArray(items)) {
            await this.browser.url(consts.NAVIGATION.disk.url);
            await this.browser.yaDeleteCompletely([].concat(items), { safe: true, fast: true });
        }
    });

    it('diskclient-6782: Переходы в раздел "Просмотр" (+ diskclient-6761: Открытие документа в DV из Доксов)', async function () {
        this.testpalmId = 'diskclient-6782';

        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-630');
        const baseDocsUrl = getDocsUrl(bro);
        await bro.url(baseDocsUrl + '/docs?type=docx');

        await bro.yaWaitForVisible(docsObjects.common.docsListing());
        await bro.yaWaitForHidden(docsObjects.desktop.docsSidebar.viewSection());
        await bro.yaCallActionInActionPopup('енот.docx', 'view');

        const currentURL = await bro.getUrl();
        assert(
            currentURL.startsWith(baseDocsUrl + '/docs/view?url='),
            'Ссылка не поменялась на просмотр документа: ' + currentURL
        );
        await bro.yaWaitForVisible(docsObjects.desktop.docsSidebar.viewSection());

        await yaWaitDocumentOpened.call(this, true);

        await bro.yaAssertView(this.testpalmId + '-1', docsObjects.desktop.docsSidebar());

        await bro.yaClick(docsObjects.desktop.docsSidebar.docxSection());
        await bro.yaWaitForHidden(docsObjects.desktop.docsSidebar.viewSection());
        await bro.yaWaitForVisible(docsObjects.common.docsPage.title().replace(':title:', DOCS_SECTION_TITLES.docx));
        await bro.yaAssertView(this.testpalmId + '-2', docsObjects.desktop.docsSidebar(), {
            ignoreElements: [clientNavigation.desktop.spaceInfoSection()]
        });
    });

    it('diskclient-6785: Переход из раздела "Просмотр" при создании документа', async function () {
        this.testpalmId = 'diskclient-6785';

        await openDocument.call(this, {
            url: 'ya-disk:///disk/енот.docx',
            user: { auth: true, login: 'yndx-ufo-test-630', uid: '1013657729' }
        });

        const bro = this.browser;
        await bro.yaWaitForVisible(docsObjects.desktop.docsSidebar.createButton());
        await bro.yaClick(docsObjects.desktop.docsSidebar.createButton());
        await bro.yaWaitForVisible(docsObjects.desktop.docsSidebarCreatePopup.xlsx());
        await bro.yaClick(docsObjects.desktop.docsSidebarCreatePopup.xlsx());

        await bro.yaWaitForVisible(docsObjects.common.documentTitleDialog());

        const fileName = `tmp-${performance.now()}`;
        const fullFileName = fileName + '.xlsx';
        this.currentTest.ctx.items = [fullFileName];
        await bro.yaSetValue(docsObjects.common.documentTitleDialog.nameInput(), fileName);
        await bro.yaClick(docsObjects.common.documentTitleDialog.submitButton());

        const tabs = await bro.getTabIds();
        assert(tabs.length === 2, 'Новый таб не открылся');

        await bro.window(tabs[1]);
        let url;
        await bro.waitUntil(async () => {
            url = await bro.getUrl();
            return url.includes('/edit/disk/disk');
        }, 5000, 'Не перешли в редактор');

        // нужно дождаться полного открытия редактора (=появления кнопки), чтобы таблица прорасла в Доксы
        await bro.yaWaitForVisible(docsObjects.desktop.editorShareButton(), 10000);
        await bro.close();

        await bro.yaWaitForHidden(docsObjects.common.documentTitleDialog());

        await bro.yaWaitForHidden(docsObjects.desktop.docsSidebar.viewSection());
        await bro.yaWaitForVisible(docsObjects.common.docsPage.title().replace(':title:', DOCS_SECTION_TITLES.xlsx));
        await bro.yaAssertListingHas(fullFileName);
    });

    it('diskclient-6786: Переход из раздела "Просмотр" при загрузке документа', async function () {
        this.testpalmId = 'diskclient-6786';

        const bro = this.browser;

        await openDocument.call(this, {
            url: 'ya-disk:///disk/енот.docx',
            user: { auth: true, login: 'yndx-ufo-test-630', uid: '1013657729' }
        });

        const fileName = await bro.yaUploadFiles('test-file.pptx', {
            uniq: true,
            selectFolder: true,
            target: 'docs-sidebar'
        });

        this.currentTest.ctx.items = [fileName];

        await bro.yaWaitForHidden(docsObjects.desktop.docsSidebar.viewSection());
        await bro.yaWaitForVisible(docsObjects.common.docsPage.title().replace(':title:', DOCS_SECTION_TITLES.pptx));
        await bro.yaAssertListingHas(fileName);
    });

    it('diskclient-6788: Открытие документа из Диска в разделе "Просмотр"', async function () {
        this.testpalmId = 'diskclient-6788';

        const bro = this.browser;

        await bro.yaClientLoginFast('yndx-ufo-test-630');

        const fileName = await bro.yaUploadFiles('test-file.xlsx', { uniq: true });

        this.currentTest.ctx.items = [fileName];

        await openDocument.call(this, {
            url: 'ya-disk:///disk/енот.docx',
            user: { auth: true, login: 'yndx-ufo-test-630', uid: '1013657729' }
        });

        await bro.yaWaitForVisible(docsObjects.desktop.docsSidebar.createButton());
        await bro.yaClick(docsObjects.desktop.docsSidebar.createButton());
        await bro.yaWaitForVisible(docsObjects.desktop.docsSidebarCreatePopup.open());
        await bro.yaClick(docsObjects.desktop.docsSidebarCreatePopup.open());

        await bro.yaWaitForVisible(docsObjects.common.openFromDiskDialog());

        const fileSelector = listing.common.listingBodyItemsInfoXpath().replace(/:titleText/g, fileName);
        await bro.yaWaitForVisible(fileSelector);
        await bro.yaClick(fileSelector);

        await bro.waitForEnabled(docsObjects.common.openFromDiskDialog.acceptButton());
        await bro.yaClick(docsObjects.common.openFromDiskDialog.acceptButton());

        const tabs = await bro.getTabIds();
        assert(tabs.length === 2, 'Новый таб не открылся');

        await bro.window(tabs[1]);
        await bro.waitUntil(async () => {
            return (await bro.getUrl()).includes('/edit/disk/disk');
        }, 5000, 'Не перешли в редактор');
        await bro.close();

        await bro.yaWaitForHidden(docsObjects.common.openFromDiskDialog());

        await bro.yaWaitForVisible(docsObjects.desktop.docsSidebar.viewSection());
    });

    /**
     * Тест на доабвление в Доксы документа переданного типа
     *
     * @param {'docx'|'xlsx'|'pptx'} extension
     * @returns {Promise<void>}
     */
    async function assertAddToDocsOnDocumentView(extension) {
        const bro = this.browser;

        await bro.yaClientLoginFast('yndx-ufo-test-631');

        const fileName = await bro.yaUploadFiles(`test-file.${extension}`, { uniq: true });
        this.currentTest.ctx.items = [fileName];

        await bro.yaAssertListingHas(fileName);

        const baseDocsUrl = getDocsUrl(bro);
        await bro.url(`${baseDocsUrl}/docs?type=${extension}`);

        await bro.yaWaitForVisible(docsObjects.common.docsPage());
        await bro.yaWaitForHidden(docsObjects.common.docsPage.spin());

        // заглушка == пустой листинг
        // но если не заглушка, то проверим чтоо наш файл ещё не появился в доксах
        if (!(await bro.isVisible(docsObjects.common.docsStub[extension]()))) {
            await bro.yaAssertListingHasNot(fileName, true);
        }

        await openDocument.call(this, {
            url: 'ya-disk:///disk/' + fileName,
            user: { auth: true, login: 'yndx-ufo-test-631', uid: '1013657729' },
            hasNoImages: extension === 'xlsx'
        });

        await bro.url(`${baseDocsUrl}/docs?type=${extension}`);

        await bro.yaWaitForVisible(docsObjects.common.docsPage());
        await bro.yaWaitForHidden(docsObjects.common.docsPage.spin());

        await bro.yaAssertListingHas(fileName);
    }

    it('diskclient-6779: Добавление документа (.docx) в Доксы', async function () {
        this.testpalmId = 'diskclient-6779';
        await assertAddToDocsOnDocumentView.call(this, 'docx');
    });

    it('diskclient-6780: Добавление таблицы (.xlsx) в Доксы', async function () {
        this.testpalmId = 'diskclient-6780';
        await assertAddToDocsOnDocumentView.call(this, 'xlsx');
    });

    it('diskclient-6781: Добавление презентации (.pptx) в Доксы', async function () {
        this.testpalmId = 'diskclient-6781';
        await assertAddToDocsOnDocumentView.call(this, 'pptx');
    });
});

hermione.only.in(clientDesktopBrowsersList); // DV в Доксах только на десктопах
describe('Действия над документом в просмотре -> ', () => {
    afterEach(async function () {
        const { items } = this.currentTest.ctx;
        if (Array.isArray(items)) {
            await this.browser.url(consts.NAVIGATION.disk.url);
            await this.browser.yaDeleteCompletely([].concat(items), { safe: true, fast: true });
        }
    });

    /* ToDo: поддержать автотесты в DV - https://st.yandex-team.ru/CHEMODAN-80281
    it('diskclient-6771: Скачивание личного документа', async function () {
        this.testpalmId = 'diskclient-6771';

        await openDocument.call(this, { url: 'ya-disk:///disk/Ластоногие.docx' });

        const bro = this.browser;
        const iframe = await bro.element(docsViewObjects.docsViewFrame());
        await bro.frame(iframe);
        await bro.yaClick(docsViewObjects.docviewer.title.downloadButton());
        await bro.frameParent();
    });

    it('diskclient-6774: Скачивание публичного документа', async function () {
        this.testpalmId = 'diskclient-6774';
    });

    it('diskclient-6778: Скачивание публичного документа без авторизации', async function () {
        this.testpalmId = 'diskclient-6778';
    });
    */

    it('diskclient-6770: Открытие в редакторе личного документа', async function () {
        this.testpalmId = 'diskclient-6770';

        await openDocument.call(this, { url: 'ya-disk:///disk/Ластоногие.docx' });

        const bro = this.browser;
        const iframe = await bro.$(docsViewObjects.docsViewFrame());
        await bro.switchToFrame(iframe);
        await bro.yaClick(docsViewObjects.docviewerEditButton());

        await bro.waitUntil(async () => {
            return (await bro.getUrl()).includes('/edit/disk/disk');
        }, 5000, 'Не перешли в редактор');

        const tabs = await bro.getTabIds();
        assert(tabs.length === 1, 'Табов > 1 (редактор открылся в новом, а не том же табе?)');
    });

    it('diskclient-6797: Открытие в редакторе публичного документа', async function () {
        this.testpalmId = 'diskclient-6797';

        // владелец документа - `yndx-ufo-test-628`
        await openDocument.call(this, {
            // eslint-disable-next-line max-len
            url: 'ya-disk-public://fNRHWcIgJbdevzd7FbhH/4NBXQDomSpPfGvDzPylMfTQE9EmkQe+4/uOwAoxJJEWq/J6bpmRyOJonT3VoXnDag==',
            name: 'Гваделупский енот.docx'
        });

        const bro = this.browser;
        const viewUrl = await bro.getUrl();
        const iframe = await bro.$(docsViewObjects.docsViewFrame());
        await bro.switchToFrame(iframe);
        await bro.yaClick(docsViewObjects.docviewerEditCopyButton());
        await bro.switchToParentFrame();

        const tabs = await bro.getTabIds();
        assert(tabs.length === 2, 'Новый таб не открылся');

        await bro.window(tabs[1]);
        await bro.waitUntil(async () => {
            return (await bro.getUrl()).includes('/edit/disk/disk');
        }, 10000, 'Не перешли в редактор');
        await bro.close();

        assert(
            (await bro.isVisible(docsObjects.desktop.docsSidebar.viewSection())) === true,
            'Секция "Просмотр" в сайдбаре пропала'
        );
        assert(await bro.getUrl() === viewUrl, 'В основном табе поменялась ссылка');
    });

    it('diskclient-6776: Сохранение на Диск публичного документа', async function () {
        this.testpalmId = 'diskclient-6776';

        // владелец документа - `yndx-ufo-test-628`
        await openDocument.call(this, {
            // eslint-disable-next-line max-len
            url: 'ya-disk-public://fNRHWcIgJbdevzd7FbhH/4NBXQDomSpPfGvDzPylMfTQE9EmkQe+4/uOwAoxJJEWq/J6bpmRyOJonT3VoXnDag==',
            name: 'Гваделупский енот.docx',
            user: { auth: true, login: 'yndx-ufo-test-632', uid: '1013657749' }
        });

        const bro = this.browser;
        const iframe = await bro.$(docsViewObjects.docsViewFrame());
        await bro.switchToFrame(iframe);
        await bro.yaClick(docsViewObjects.saveToDiskButton());
        await bro.yaWaitForHidden(docsViewObjects.saveToDiskButton());
        await bro.yaWaitForVisible(
            docsViewObjects.openDiskButton(),
            'Кнопка "Открыть Яндекс.Диск" не появилась',
            100
        );
        await bro.switchToParentFrame();

        await bro.yaWaitForVisible(
            docsViewObjects.docsViewSaveModal(),
            'Модалка "Сохранено в Загрузки" не появилась',
            100
        );
        // пользователь чистится GC-таском
    });

    hermione.only.notIn('firefox-desktop', 'FF не нравится, что фрейм триггерит переход на другой домен');
    it('diskclient-6777: Сохранение на Диск публичного документа без авторизации', async function () {
        this.testpalmId = 'diskclient-6777';

        // владелец документа - `yndx-ufo-test-628`
        await openDocument.call(this, {
            // eslint-disable-next-line max-len
            url: 'ya-disk-public://fNRHWcIgJbdevzd7FbhH/4NBXQDomSpPfGvDzPylMfTQE9EmkQe+4/uOwAoxJJEWq/J6bpmRyOJonT3VoXnDag==',
            name: 'Гваделупский енот.docx',
            user: { auth: false }
        });

        const bro = this.browser;
        const viewUrl = await bro.getUrl();
        let iframe = await bro.$(docsViewObjects.docsViewFrame());
        await bro.switchToFrame(iframe);
        await bro.yaClick(docsViewObjects.saveToDiskButton());
        await bro.switchToParentFrame();

        await bro.waitUntil(async () => {
            return (await bro.getUrl()).startsWith('https://passport.yandex.ru/');
        }, 2000, 'Не перешли в паспорт');

        await bro.login(getUser('yndx-ufo-test-633'));

        await bro.waitUntil(async () => {
            return (await bro.getUrl()) === viewUrl;
        }, 5000, 'Не вернулись на просмотр документа в Доксах');

        await yaWaitDocumentOpened.call(this, false);
        await bro.yaWaitForVisible(docsViewObjects.docsViewSaveModal());

        iframe = await bro.$(docsViewObjects.docsViewFrame());
        await bro.switchToFrame(iframe);
        await bro.yaWaitForHidden(
            docsViewObjects.saveToDiskButton(),
            'Кнопка "Сохранить на Яндекс.Диск" не пропала',
            100
        );
        await bro.yaWaitForVisible(
            docsViewObjects.openDiskButton(),
            'Кнопка "Открыть Яндекс.Диск" не появилась',
            100
        );
        await bro.switchToParentFrame();
        // пользователь чистится GC-таском
    });

    hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-83680');
    it('diskclient-6796: Сохранение на Диск публичного документа переполненным пользователем', async function () {
        this.testpalmId = 'diskclient-6796';

        // владелец документа - `yndx-ufo-test-628`
        await openDocument.call(this, {
            // eslint-disable-next-line max-len
            url: 'ya-disk-public://fNRHWcIgJbdevzd7FbhH/4NBXQDomSpPfGvDzPylMfTQE9EmkQe+4/uOwAoxJJEWq/J6bpmRyOJonT3VoXnDag==',
            name: 'Гваделупский енот.docx',
            user: { auth: true, login: 'fullOfUser' }
        });

        const bro = this.browser;

        const iframe = await bro.$(docsViewObjects.docsViewFrame());
        await bro.switchToFrame(iframe);
        await bro.yaClick(docsViewObjects.saveToDiskButton());
        await bro.switchToParentFrame();

        await bro.yaWaitForVisible(overdraftContent());
        await bro.pause(500);
        await bro.click(overdraftContent.closeButton());
        await bro.yaWaitForHidden(overdraftContent());

        await bro.frame(iframe);
        await bro.yaWaitForVisible(
            docsViewObjects.saveToDiskButton(),
            'Кнопка "Сохранить на Яндекс.Диск" пропала',
            100
        );
        await bro.switchToParentFrame();
    });
});

hermione.only.in(clientDesktopBrowsersList); // DV в Доксах только на десктопах
describe('Ограниченный шаринг', () => {
    it('diskclient-7295: Отображение таблицы в DV из паблика c запретом скачивания', async function() {
        this.testpalmId = 'diskclient-7295';

        await assertDocumentView.call(this, {
            // eslint-disable-next-line max-len
            url: 'ya-disk-public://n7FZxy6l3OHr8gb3XR/Zw+snNsGrRtporIpsaTY2a4ByI5aUUAAibJ8MDYdTNGtRq/J6bpmRyOJonT3VoXnDag==',
            name: 'readonly document.docx',
            hideTopDirect: true,
            isReadOnly: true
        });
    });

    it('diskclient-7296: Отображение документа в DV из паблика c запретом скачивания', async function() {
        this.testpalmId = 'diskclient-7296';

        await assertDocumentView.call(this, {
            // eslint-disable-next-line max-len
            url: 'ya-disk-public://ThP1w0DPovSLSAvAnXer9LDle/Fp/7U5GowXryQWPrCymSKNc7CuvcEqK4NyvP9cq/J6bpmRyOJonT3VoXnDag==',
            name: 'readonly table.xlsx',
            hideTopDirect: true,
            hasNoImages: true,
            isReadOnly: true
        });
    });

    it('diskclient-7297: Отображение презентации в DV из паблика c запретом скачивания', async function() {
        this.testpalmId = 'diskclient-7297';

        await assertDocumentView.call(this, {
            // eslint-disable-next-line max-len
            url: 'ya-disk-public://d7eWoq7Os64Nj/qPv5dzJ2v6Szrz/yJAPdNbEEM0sx4HgAzLz86BfPECBTVGhId+q/J6bpmRyOJonT3VoXnDag==',
            name: 'readonly presentation.pptx',
            hideTopDirect: true,
            isReadOnly: true
        });
    });
});
