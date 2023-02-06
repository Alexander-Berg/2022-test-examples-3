const publicPageObjects = require('../page-objects/public');
const clientNavigation = require('../page-objects/client-navigation');
const clientContentListing = require('../page-objects/client-content-listing');
const clientTuningPage = require('../page-objects/client-tuning-page');

const { consts, publicFiles } = require('../config');
const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');

/**
 * @typedef { import('@ps-int/ufo-hermione/types').Browser } Browser
 */

const GREEN = 'green';
const YELLOW = 'yellow';
const RED = 'red';
const PUBLIC_FILE_URL = publicFiles['1_GB_file'].url;

class MemoryIndicatorTestTemplate {
    /**
     * @param {Browser} bro
     * @param {string} testpalmId
     * @param {Object} spaceIndicatorSelectors
     */
    constructor(bro, testpalmId, spaceIndicatorSelectors) {
        this.browser = bro;
        this.testpalmId = testpalmId;
        this.spaceIndicatorSelectors = spaceIndicatorSelectors;
    }

    /**
     * Главная тестовая функция
     *
     * @param {string} firstColor - цвет, который мы ожидаем увидеть в начала
     * @param {string} secondColor - цвет, который мы ожидаем увидеть после того, как заполним память
     * @param {string} firstText - текст, который мы ожидаем увидеть после того, как заполним память
     * @param {string} secondText - текст, который мы ожидаем увидеть после того, как заполним память
     * @returns {Promise<void>}
     */
    async test(firstColor, secondColor, firstText, secondText) {
        await this.setUpEnvironment();

        await this.checkStartState(firstColor);
        await this.assertViewMemoryIndicator(firstColor, firstText);
        await this.fillMemory();
        await this.assertViewMemoryIndicator(secondColor, secondText);
        await this.checkStartState(firstColor);
    }

    /**
     * Абстрактный метод для натсройки начального окружения
     *
     * @returns {Promise<void>}
     */
    async setUpEnvironment() {}

    /**
     * Проверка текущего состояния индикатора. Если цвет индикатора не соответсвует переданному аргументу,
     * удаляет папку загрузки.
     *
     * @param {string} color
     * @returns {Promise<void>}
     */
    async checkStartState(color) {
        try {
            await this.browser.yaWaitForVisible(this.spaceIndicatorSelectors[color]());
        } catch (error) {
            await this.freeMemory();
        }
    }

    /**
     * Абстрактный метод для освобождения места на диске
     *
     * @returns {Promise<void>}
     */
    async freeMemory() {}

    /**
     * Абстрактный метод для заполнения места на диске
     *
     * @returns {Promise<void>}
     */
    async fillMemory() {}

    /**
     * Метод для фотографирования индикатора и текста под ним
     *
     * @param {string} color
     * @param {string} text
     * @returns {Promise<void>}
     */
    async assertViewMemoryIndicator(color, text) {
        await this.browser.yaWaitForVisible(this.spaceIndicatorSelectors[color](), consts.FILE_OPERATIONS_TIMEOUT);
        await this.browser.scroll(this.spaceIndicatorSelectors());
        await this.browser.yaAssertMemoryIndicatorText(text);
        await this.browser.yaAssertView(`${this.testpalmId}-${color}`, this.spaceIndicatorSelectors());
    }
}

describe('Индикатор свободного места', () => {
    hermione.only.in(clientDesktopBrowsersList);
    describe('Сайдбар', () => {
        class SidebarMemoryIndicator extends MemoryIndicatorTestTemplate {
            /**
             * @returns {Promise<void>}
             */
            async freeMemory() {
                await this.browser.yaDeleteCompletely('Загрузки', { safe: true, fast: true });
                await this.browser.url(consts.NAVIGATION.disk.url);
            }

            /**
             * @returns {Promise<void>}
             */
            async fillMemory() {
                await this.browser.newWindow(PUBLIC_FILE_URL);
                await this.browser.yaWaitForVisible(publicPageObjects.desktopToolbar.saveButton());
                await this.browser.click(publicPageObjects.desktopToolbar.saveButton());
                await this.browser.close();
            }
        }

        hermione.auth.tus({ tus_consumer: 'disk-front-client', tags: ['diskclient-616-1'] });
        it('diskclient-616: Индикатор свободного места на Диске. Переход с зеленого на желтый и обратно.', async function() {
            const sidebarMemoryIndicator = new SidebarMemoryIndicator(
                this.browser,
                'diskclient-616',
                clientNavigation.desktop.infoSpaceIndicator
            );
            await sidebarMemoryIndicator.test(GREEN, YELLOW, 'Свободно 6 ГБ из 10 ГБ', 'Свободно 5 ГБ из 10 ГБ');
        });

        hermione.auth.tus({ tus_consumer: 'disk-front-client', tags: ['diskclient-616-2'] });
        it('diskclient-616: Индикатор свободного места на Диске. Переход с желтого на красный и обратно.', async function() {
            const sidebarMemoryIndicator = new SidebarMemoryIndicator(
                this.browser,
                'diskclient-616',
                clientNavigation.desktop.infoSpaceIndicator
            );
            await sidebarMemoryIndicator.test(YELLOW, RED, 'Свободно 2 ГБ из 10 ГБ', 'Остался 1 ГБ из 10 ГБ');
        });
    });

    describe('Тюнинг', () => {
        class TuningMemoryIndicator extends MemoryIndicatorTestTemplate {
            /**
             * @returns {Promise<void>}
             */
            async freeMemory() {
                await this.browser.newWindow(consts.NAVIGATION.disk.url);
                await this.browser.pause(3000); // открытие вкладки/окна может происходить долго
                await this.browser.yaWaitForHidden(clientContentListing.common.listingSpinner());
                await this.browser.yaDeleteCompletely('Загрузки', { safe: true, fast: true });
                await this.browser.close();
                await this.browser.switchTab(this.tuningTabId);
            }

            /**
             * @returns {Promise<void>}
             */
            async fillMemory() {
                const isMobile = await this.browser.yaIsMobile();

                await this.browser.newWindow(PUBLIC_FILE_URL);
                await this.browser.yaWaitForVisible(
                    isMobile ?
                        publicPageObjects.toolbar.saveButton() :
                        publicPageObjects.desktopToolbar.saveButton()
                );
                await this.browser.click(
                    isMobile ?
                        publicPageObjects.toolbar.saveButton() :
                        publicPageObjects.desktopToolbar.saveButton()
                );
                await this.browser.close();
                await this.browser.switchTab(this.tuningTabId);
            }

            /**
             * @returns {Promise<void>}
             */
            async setUpEnvironment() {
                await this.browser.yaOpenSection('tuning', true);
                this.tuningTabId = await this.browser.getCurrentTabId();
            }
        }

        hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-74389');
        hermione.auth.tus({ tus_consumer: 'disk-front-client', tags: ['diskclient-1135-1', 'diskclient-1076-1'] });
        it('diskclient-1135, 1076: Подраздел "Доступное место". Переход с зеленого на желтый и обратно.', async function() {
            const testpalmId = await this.browser.yaIsMobile() ? 'diskclient-1135' : 'diskclient-1076';
            const tuningMemoryIndicator = new TuningMemoryIndicator(
                this.browser,
                testpalmId,
                clientTuningPage.common.tuningPage.body.availableSpace.bar
            );
            await tuningMemoryIndicator.test(GREEN, YELLOW);
        });

        hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-74389');
        hermione.auth.tus({ tus_consumer: 'disk-front-client', tags: ['diskclient-1135-2', 'diskclient-1076-2'] });
        it('diskclient-1135, 1076: Подраздел "Доступное место". Переход с желтого на красный и обратно.', async function() {
            const testpalmId = await this.browser.yaIsMobile() ? 'diskclient-1135' : 'diskclient-1076';

            const tuningMemoryIndicator = new TuningMemoryIndicator(
                this.browser,
                testpalmId,
                clientTuningPage.common.tuningPage.body.availableSpace.bar
            );
            await tuningMemoryIndicator.test(YELLOW, RED);
        });
    });
});
