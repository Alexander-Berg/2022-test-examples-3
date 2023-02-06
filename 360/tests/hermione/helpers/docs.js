const uriHelper = require('../../../components/helpers/uri');
const docs = require('../page-objects/docs');
const clientNavigation = require('../page-objects/client-navigation');

/**
 * @typedef { import('@ps-int/ufo-hermione/types').Browser } Browser
 */

/**
 * @param {Browser} bro
 * @returns {string}
 */
function getDocsUrl(bro) {
    return bro.options.baseUrl
        .replace(/\.regtests\./, '.docs.regtests.')
        .replace('disk', 'docs');
}

/**
 * @param {Browser} bro
 * @param {Object} dvParams
 * @returns {string}
 */
function getDocsViewUrl(bro, dvParams) {
    const docsUrl = getDocsUrl(bro);
    return uriHelper.addParams(docsUrl + '/docs/view', dvParams);
}

/**
 * @param {string} login
 * @param {string} testId
 */
async function openDocsUrl(login, testId) {
    const bro = this.browser;
    await bro.yaClientLoginFast(login);

    let url = getDocsUrl(bro);

    if (testId) {
        if (url.match(/\?\w*=?.*/)) {
            url = url + `&test-id=${testId}`;
        } else {
            url = url + `?test-id=${testId}`;
        }
    }

    await bro.url(url.toString());
}
const DOCS_SECTION_TITLES = {
    docx: 'Недавние документы',
    xlsx: 'Недавние таблицы',
    pptx: 'Недавние презентации',
    scans: 'Сканы'
};

/**
 * @param {'docx'|'xlsx'|'pptx'} type
 * @param {boolean} withoutWaiting
 */
async function openDocsSection(type, withoutWaiting) {
    const bro = this.browser;
    if (await this.browser.yaIsMobile()) {
        await bro.click(docs.touch[`${type}Section`]());
    } else {
        await bro.click(docs.desktop.docsSidebar[`${type}Section`]());
    }
    if (!withoutWaiting) {
        await bro.yaWaitForVisible(docs.common.docsPage.title().replace(':title:', DOCS_SECTION_TITLES[type]));
    }
}

/**
 *
 */
async function openDocsSort() {
    const bro = this.browser;
    const isMobile = await bro.yaIsMobile();
    if (isMobile) {
        bro.click(docs.touch.touchListingSettingsButton());
        bro.yaWaitForVisible(docs.touch.touchListingSettings());
    } else {
        bro.click(docs.common.docsPage.titleWrapper());
        bro.yaWaitForVisible(docs.desktop.sortPopup());
    }
    await bro.pause(isMobile ? 1000 : 500);
}

/**
 *
 */
async function closeDocsSort() {
    const bro = this.browser;
    if (await bro.yaIsMobile()) {
        await bro.yaExecuteClick(clientNavigation.touch.modalCell());
        bro.yaWaitForHidden(docs.touch.touchListingSettings());
    } else {
        bro.keys('Escape');
        bro.yaWaitForHidden(docs.desktop.sortPopup());
    }
}

/**
 *
 */
async function setDocsSort({ type, order }) {
    const bro = this.browser;
    const isMobile = await bro.yaIsMobile();
    if (type) {
        await openDocsSort.call(this);
        await bro.click(isMobile ?
            docs.touch.touchListingSettings[`${type}SortType`]() :
            docs.desktop.sortPopup[`${type}SortType`]()
        );
        await bro.yaWaitForHidden(isMobile ? docs.touch.touchListingSettings() : docs.desktop.sortPopup());
    }

    if (order) {
        await openDocsSort.call(this);
        await bro.click(isMobile ?
            docs.touch.touchListingSettings[`${order}SortOrder`]() :
            docs.desktop.sortPopup[`${order}SortOrder`]()
        );
        await bro.yaWaitForHidden(isMobile ? docs.touch.touchListingSettings() : docs.desktop.sortPopup());
    }
}

module.exports = {
    openDocsSection,
    openDocsSort,
    closeDocsSort,
    setDocsSort,
    openDocsUrl,
    DOCS_SECTION_TITLES,
    getDocsUrl,
    getDocsViewUrl
};
