const { NAVIGATION } = require('../config').consts;
const { desktop: clientPhoto2Page, common: commonPhoto2Page } = require('../page-objects/client-photo2-page');
const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const { assert } = require('chai');

const ALBUM_URL = NAVIGATION.photo.url + '?filter=beautiful';

/**
 * @param {string} lastItemName
 * @returns {Promise<void>}
 */
async function testGoToEnd(lastItemName) {
    await this.browser.click(clientPhoto2Page.fastScroll.goToBottomButton());
    await this.browser.yaWaitPhotoSliceItemInViewport(lastItemName);
    await this.browser.yaAssertFastscrollPointerPosition(({ bottom }, { top }) => bottom === top);
}

/**
 * @param {string} firstItemName
 * @returns {Promise<void>}
 */
async function testGoToBegin(firstItemName) {
    const bro = this.browser;

    await bro.yaScrollToEnd();

    await bro.yaAssertFastscrollPointerPosition(({ bottom }, { top }) => bottom === top);

    await bro.click(clientPhoto2Page.fastScroll.goToTopButton());
    await bro.yaWaitPhotoSliceItemInViewport(firstItemName);

    await bro.yaAssertFastscrollPointerPosition(({ top: containerTop }, { top }) => top === containerTop);
}

/**
 * @param {Array<{label: string, id: string}>} expectedMonths
 * @returns {Promise<void>}
 */
async function testScroll(expectedMonths) {
    const bro = this.browser;
    const step = 50;
    const labels = new Set();
    const positions = new Set();

    let scrollPosition = 0;

    // скроллит вниз пока дата в балуне не сменится pointsCount раз
    // каждый раз при смене даты в балуне сохраняет координату на которой это произошло
    while (labels.size < expectedMonths.length) {
        const [label, position] = await bro.execute((scrollPointerSelector, pointerLabelSelector, scroll) => {
            window.scrollTo(0, scroll);

            const pointerLabelElement = document.querySelector(pointerLabelSelector);
            const pointerElement = document.querySelector(scrollPointerSelector);

            const label = pointerLabelElement.innerText;
            const position = pointerElement.getBoundingClientRect().top;

            return [label, position];
        }, clientPhoto2Page.fastScroll.scrollPointer(), clientPhoto2Page.fastScroll.pointerLabel(), scrollPosition);

        if (!labels.has(label)) {
            positions.add(position);
        }

        labels.add(label);

        scrollPosition += step;
    }

    await bro.yaWaitForHidden(clientPhoto2Page.fastScroll.pointer());
    await bro.yaWaitPhotoSliceItemsInViewportLoad();

    // собирает позиции отметок месяцов на шкале фаст-скрола
    const marksPositions = await bro.execute((selector, marksCount) => {
        return Array.from(document.querySelectorAll(selector)).slice(0, marksCount)
            .map((element) => ({
                top: element.getBoundingClientRect().top,
                month: element.dataset.month
            }))
            .reduce((result, { top, month }) => {
                result[month] = top;
                return result;
            }, {});
    }, clientPhoto2Page.fastScroll.month(), 3);

    const labelsValues = Array.from(labels);
    const positionsValues = Array.from(positions);

    // проверяет что даты в балуне менялись на ожидаемые
    // также проверяет что дата сменялась при проходе правильной отметки на шкале
    expectedMonths.forEach((expectedMonth, index) => {
        assert.equal(expectedMonth.label, labelsValues[index], 'Неправильный месяц в балуне');
        if (marksPositions[expectedMonth.id]) {
            const delta = Math.abs(positionsValues[index] - marksPositions[expectedMonth.id]);
            assert(
                delta < 3,
                `Отметка на фастскролле находится на неправильном месте (${positionsValues[index]}, ${marksPositions[expectedMonth.id]})`
            );
        }
    });
}
/**
 *
 */
async function getPointerPosition() {
    const pointerPosition = await this.browser.execute((scrollPointerSelector) => {
        const pointerElement = document.querySelector(scrollPointerSelector);
        return pointerElement.getBoundingClientRect().top;
    }, clientPhoto2Page.fastScroll.scrollPointer());
    return pointerPosition;
}

/**
 * @param {string} startMonth
 * @param {string} endMonth
 * @param {string} targetPhotoName
 * @returns {Promise<void>}
 */
async function testScrollViaFastScroll(startMonth, endMonth, targetPhotoName) {
    const bro = this.browser;

    const startElement = await bro.$(clientPhoto2Page.fastScroll.month() + `[data-month="${startMonth}"]`);
    const startElementLocation = await startElement.getLocation();

    const endElement = await bro.$(clientPhoto2Page.fastScroll.month() + `[data-month="${endMonth}"]`);
    const endElementLocation = await endElement.getLocation();

    await bro.performActions([{
        type: 'pointer',
        id: 'mouseButton',
        actions: [
            {
                type: 'pointerMove',
                duration: 0,
                x: parseInt(startElementLocation.x, 10),
                y: parseInt(startElementLocation.y, 10)
            },
            { type: 'pointerDown', button: 0 },
            {
                type: 'pointerMove',
                duration: 1000,
                x: parseInt(endElementLocation.x, 10),
                y: parseInt(endElementLocation.y, 10)
            },
            { type: 'pointerUp', button: 0 },
        ]
    }]);

    const pointerPosition = await getPointerPosition.call(this);

    // в зависимости от браузера, клик может быть не очень точным
    try {
        await bro.yaWaitPhotoSliceItemInViewport(targetPhotoName);
    } catch (error) {
        await bro.yaScrollIntoView(commonPhoto2Page.photo.itemByName().replace(':title', targetPhotoName));
        const newPointerPosition = await getPointerPosition.call(this);
        const delta = newPointerPosition - pointerPosition;
        assert(Math.abs(delta) < 2, 'Элемент не находится во вьюпорте, отклонение слишком сильное');
    }
}

/**
 * @param {string} month
 * @param {string} targetPhotoName
 */
async function fastScrollAndCheckItemInViewPort(month, targetPhotoName) {
    const bro = this.browser;
    await bro.yaWaitPhotoSliceItemsInViewportLoad();

    const element = await bro.$(clientPhoto2Page.fastScroll.month() + `[data-month="${month}"]`);
    const elementLocation = await element.getLocation();

    await bro.performActions([{
        type: 'pointer',
        id: `mouseButton-${month}`,
        actions: [
            {
                type: 'pointerMove', duration: 0,
                x: parseInt(elementLocation.x, 10),
                y: parseInt(elementLocation.y, 10)
            },
            { type: 'pointerDown', button: 0 },
            { type: 'pause', duration: 100 },
            { type: 'pointerUp', button: 0 },
        ]
    }]);
    await bro.releaseActions();

    const pointerPosition = await getPointerPosition.call(this);

    // в зависимости от браузера, клик может быть не очень точным
    try {
        await bro.yaWaitPhotoSliceItemInViewport(targetPhotoName);
    } catch (error) {
        await bro.yaScrollIntoView(commonPhoto2Page.photo.itemByName().replace(':title', targetPhotoName));
        const newPointerPosition = await getPointerPosition.call(this);
        const delta = newPointerPosition - pointerPosition;
        assert(Math.abs(delta) < 1.5, 'Элемент не находится во вьюпорте, отклонение слишком сильное');
    }
}

/**
 * @returns {Promise<void>}
 */
async function testRebuildOnResize() {
    const bro = this.browser;

    await bro.yaWaitPhotoSliceItemsInViewportLoad();
    const { height, width } = await bro.windowHandleSize();

    await bro.windowHandleSize({ width: width / 2, height: height / 2 });
    await bro.yaWaitPhotoSliceItemsInViewportLoad();
    await bro.assertView(`${this.testpalmId}-1`, clientPhoto2Page.fastScroll());

    await bro.windowHandleSize({ width, height });
    await bro.yaWaitPhotoSliceItemsInViewportLoad();
    await bro.assertView(`${this.testpalmId}-2`, clientPhoto2Page.fastScroll());
}

/**
 * @param {string} user
 * @param {boolean} isAlbums
 * @param {"tile"|"wow"} listingType
 * @returns {Promise<void>}
 */
async function beforeEachTemplate(user, isAlbums, listingType) {
    const bro = this.browser;
    await bro.yaClientLoginFast(user);
    if (isAlbums) {
        await bro.url(ALBUM_URL);
    } else {
        await bro.yaOpenSection('photo');
    }
    await bro.yaSetPhotoSliceListingType(listingType);

    await bro.yaWaitPhotoSliceItemsInViewportLoad();
}

hermione.only.in(clientDesktopBrowsersList, 'Актуально только для десктопной версии');
describe('Фаст-скролл в фотосрезе -> ', () => {
    beforeEach(async function() {
        await beforeEachTemplate.call(this, 'yndx-ufo-test-44', false, 'tile');
    });

    it('diskclient-4551: Переход в конец раздела Фото по клику к старым фото', async function() {
        this.testpalmId = 'diskclient-4551';
        await testGoToEnd.call(this, '2015-01-01 15-18-39.JPG');
    });

    it('diskclient-4552: Переход в начало раздела Фото по клику к новым фото', async function() {
        this.testpalmId = 'diskclient-4552';
        await testGoToBegin.call(this, 'The Talking Tree.mp4');
    });

    hermione.only.notIn('firefox-desktop', 'https://st.yandex-team.ru/CHEMODAN-72347');
    it('diskclient-4553: Отображение подсказок по ховеру на иконки к новым фото и к старым фото', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-4553';

        await bro.yaWaitForVisible(clientPhoto2Page.fastScroll.goToTopButton());

        await bro.pause(5000); // прогрузка для ФФ
        await bro.moveToObject(clientPhoto2Page.fastScroll.goToTopButton(), 1, 1);
        await bro.yaWaitForVisible(clientPhoto2Page.fastScrollGoToTopTooltip(), 10000);
        await bro.pause(200); // анимация тултипа
        await bro.assertView(
            `${this.testpalmId}-top`, [
                clientPhoto2Page.fastScroll.goToTopButton(),
                clientPhoto2Page.fastScrollGoToTopTooltip()
            ]
        );

        await bro.moveToObject(clientPhoto2Page.fastScroll.goToBottomButton());
        await bro.yaWaitForVisible(clientPhoto2Page.fastScrollGoToBottomTooltip(), 10000);
        await bro.pause(200); // анимация тултипа
        await bro.assertView(
            `${this.testpalmId}-bottom`, [
                clientPhoto2Page.fastScroll.goToBottomButton(),
                clientPhoto2Page.fastScrollGoToBottomTooltip()
            ]
        );
    });

    hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-84270'); // сильно мигает везде
    it('diskclient-4554: Скролл колесиком мыши', async function() { /* жаль что у меня нет колёсика на мыши :'( */
        this.browser.executionContext.timeout(400000);
        this.testpalmId = 'diskclient-4554';
        await testScroll.call(this, [
            { label: 'Май 2019', id: '4-2019' },
            { label: 'Апрель 2019', id: '3-2019' },
            { label: 'Декабрь 2018', id: '11-2018' },
            { label: 'Ноябрь 2018', id: '10-2018' }
        ]);
    });

    hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-84270'); // сильно мигает везде
    it('diskclient-4555: Скролл колесиком мыши с курсором на шкале фотосреза', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-4555';

        await bro.yaHoverOnFastscrollMonth('6-2018', 0, 1);
        await bro.yaWaitForVisible(clientPhoto2Page.fastScroll.pointer());

        await bro.assertView(this.testpalmId, clientPhoto2Page.fastScroll.pointer());

        await bro.yaAssertFastscrollPointerPosition(({ top: containerTop }, { top }) => containerTop === top);

        await bro.scroll(0, 10000);

        await bro.yaWaitPhotoSliceItemsInViewportLoad();
        await bro.yaWaitForVisible(clientPhoto2Page.fastScroll.pointer());

        await bro.yaAssertFastscrollPointerPosition(({ top: containerTop }, { top }) => containerTop !== top);

        assert.equal(await bro.getText(clientPhoto2Page.fastScroll.pointerLabel()), 'Июль 2018');
    });

    it('diskclient-4558: Скролл перетягиванием балуна с датой', async function() {
        this.testpalmId = 'diskclient-4558';
        await testScrollViaFastScroll.call(this, '11-2018', '3-2018', '2018-04-29 12-48-51.JPG');
    });

    it('diskclient-4560: Переход в разделе Фото по клику на фастскролл', async function() {
        this.testpalmId = 'diskclient-4560';
        await fastScrollAndCheckItemInViewPort.call(this, '6-2015', '2015-07-31 22-40-18.JPG');
    });

    it('diskclient-4562: Адаптация шкалы фастскролла при ресайзе', async function() {
        this.testpalmId = 'diskclient-4562';
        await testRebuildOnResize.call(this);
    });
});

hermione.only.in(clientDesktopBrowsersList, 'Актуально только для десктопной версии');
describe('Фаст-скролл в фотосрезе в режиме вау-сетки -> ', () => {
    it('diskclient-4595: [Вау-сетка] Переход к старым фото', async function() {
        this.testpalmId = 'diskclient-4595';
        await beforeEachTemplate.call(this, 'yndx-ufo-test-143', false, 'wow');
        await testGoToEnd.call(this, '2018-04-19 21-22-19.JPG');
    });

    hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-84270');
    it('diskclient-4663: [Вау-сетка] Фастскролл фотосреза в режиме вау-сетки', async function() {
        this.testpalmId = 'diskclient-4663';
        await beforeEachTemplate.call(this, 'yndx-ufo-test-272', false, 'wow');
        await fastScrollAndCheckItemInViewPort.call(this, '10-2019', 'IMG_20191129_152928.jpg');
        await fastScrollAndCheckItemInViewPort.call(this, '11-2019', 'IMG_20190823_132521.jpg');
    });
});

hermione.only.in(clientDesktopBrowsersList, 'Актуально только для десктопной версии');
describe('Фаст-скролл в фотосрезе альбомов -> ', () => {
    beforeEach(async function() {
        await beforeEachTemplate.call(this, 'yndx-ufo-test-228', true, 'wow');
    });

    it('diskclient-5441: Переход в конец автоальбома по клику на иконку к старым фото', async function() {
        await testGoToEnd.call(this, '2015-01-03 11-53-34.JPG');
    });

    it('diskclient-5443: Переход в начало автоальбома по клику на иконку к новым фото', async function() {
        await testGoToBegin.call(this, '2019-06-04 13-09-44.JPG');
    });

    hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-84270'); // сильно мигает везде
    it('diskclient-5444: Скролл колесиком мыши в автоальбоме', async function() {
        await testScroll.call(this, [
            { label: 'Июнь 2019', id: '5-2019' },
            { label: 'Апрель 2019', id: '3-2019' },
            { label: 'Февраль 2019', id: '1-2019' },
            { label: 'Январь 2019', id: '0-2019' }
        ]);
    });

    hermione.only.notIn('firefox-desktop', 'https://st.yandex-team.ru/CHEMODAN-84270'); // нестабильно ведет себя в ff
    it('diskclient-5445: Скролл перетягиванием балуна с датой в автоальбоме', async function() {
        await testScrollViaFastScroll.call(this, '0-2019', '4-2018', '2018-05-31 20-34-55.JPG');
    });

    it('diskclient-5446: Переход по клику на фастскролл в авитоальбоме', async function() {
        await fastScrollAndCheckItemInViewPort.call(this, '6-2015', '2015-07-30 15-40-32.JPG');
    });

    it('diskclient-5447: Адаптация шкалы фастскролла при ресайзе в автоальбоме', async function() {
        this.testpalmId = 'diskclient-5447';
        await testRebuildOnResize.call(this);
    });
});
