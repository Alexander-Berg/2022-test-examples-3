const slider = require('../page-objects/slider').common;
const { photo } = require('../page-objects/client-photo2-page').common;
const popups = require('../page-objects/client-popups');
const consts = require('../config').consts;
const { assert } = require('chai');

/**
 * @typedef { import('@ps-int/ufo-hermione/types').Browser } Browser
 */

/**
 * Тесты для кнопки Убрать из альбома проходят по схеме:
 * 1. Переходим в нужный альбом
 * 2. Выбираем случайное фото
 * 3. Копируем выбранный файл с заменой имени на {timestamp}_{имяФайла} (если название уже такого вида - обновляем в нем
 * значение timestamp)
 * 4. Убираем исходный файл из альбома
 * 5. Удаляем исходный файл
 * (скопированный файл через некоторое время появится снова в альбоме и будет выбран для удаления из альбома в следующих
 * тестах)
 *
 * У пользователя
 * - много фото в альбоме Красивые - тесты про исключения делаем в этом альбоме
 * - 1 фото из чужой общей папки в альбоме Разобрать - для проверки отсутствия кнопки Убрать из альбома
 * - 2 фото в папке Камера(исключенные из альбома Красивые - чтобы они случайно не удалились) - для проверки отсутствия
 * кнопки Убрать из альбома в альбоме Камера
 */

describe('Альбомы-срезы -> ', () => {
    describe('Убрать из альбома -> ', () => {
        beforeEach(async function() {
            const bro = this.browser;
            await bro.yaClientLoginFast('yndx-ufo-test-230');
        });

        afterEach(async function() {
            const listingResources = this.currentTest.ctx.listingResources || [];

            if (listingResources.length) {
                const bro = this.browser;
                await bro.yaOpenSection('disk');
                await bro.yaDeleteCompletely(listingResources, { safe: true, fast: true });
            }
        });

        /**
         * Выбирает в срезе фото, в названии которой есть root (это фото лежит в корне диска -
         * данные тесты должны работать с этими файлами - те, которые лежат в папках - временные)
         *
         * @param {Browser} bro
         * @returns {string}
         */
        const getRandomPhotoName = async(bro) => {
            return bro.yaGetPhotosliceItemName(await bro.yaGetPhotosliceRandomPhoto());
        };

        /**
         * Возвращает имя для копирования вида {timestamp}_{имяФайла}
         *
         * @param {string} photoName
         * @returns {string}
         */
        const getNameForCopy = (photoName) => {
            const timestamp = Date.now();
            const nameParts = photoName.match(/[\w]+_([\w\. \-]+)/);
            const originalName = nameParts && nameParts[1] || photoName;
            return timestamp + '_' + originalName;
        };

        /**
         * @param {string} photoName
         * @param {string} newPhotoName
         * @returns {Promise<void>}
         */
        async function copyPhoto(photoName, newPhotoName) {
            const bro = this.browser;

            await bro.url('/client/disk');

            await bro.yaScrollToEnd();

            this.currentTest.ctx.listingResources = (this.currentTest.ctx.listingResources || []).concat([photoName]);
            await bro.yaSelectResource(photoName);
            await bro.yaCopySelected('');
            await bro.yaSetValue(popups.common.confirmationPopup.nameInput(), newPhotoName);
            await bro.yaWaitForVisible(popups.common.confirmationPopup.acceptButton());
            await bro.click(popups.common.confirmationPopup.acceptButton());
            await bro.yaWaitNotificationForResource({
                name: newPhotoName,
                folder: 'Файлы'
            }, consts.TEXT_NOTIFICATION_FILE_COPIED_TO_FOLDER);
        }

        /**
         * Переход в автоальбом
         *
         * @param {string} filter
         * @param {string} [photoName] - если не указано имя - проверяем появление любого item'а
         * @returns {Promise<void>}
         */
        async function goToPhotoInAutoAlbum(filter, photoName = '') {
            const bro = this.browser;
            await bro.url('/client/photo?filter=' + filter);

            const photoSelector = photoName ? photo.itemByName().replace(':title', photoName) : photo.item();
            await bro.yaWaitForVisible(photoSelector, 10000);
        }

        /**
         * @param {string} [photoName]
         * @returns {Promise<void>}
         */
        async function checkNoButtonForPhoto(photoName = '') {
            const bro = this.browser;

            const photoSelector = photoName ? photo.itemByName().replace(':title', photoName) : photo.item();

            // кнопки Убрать из альбома нет  в слайдере
            await bro.yaWaitForVisible(photoSelector);
            await bro.click(photoSelector);
            await bro.pause(2000);
            await bro.yaWaitForVisible(slider.sliderButtons());
            await bro.yaWaitForHidden(slider.sliderButtons.excludeFromAlbumButton());
            await bro.yaWaitForVisible(slider.sliderButtons.closeButton(), 5000);
            await bro.click(slider.sliderButtons.closeButton());
            await bro.yaWaitForHidden(slider.contentSlider());

            // кнопки Убрать из альбома нет в топбаре
            if (photoName) {
                await bro.yaSelectPhotoItemByName(photoName, true);
            } else {
                await bro.yaSelectPhotoItem(photoSelector, true);
            }
            await bro.yaWaitActionBarDisplayed();
            await bro.yaWaitForHidden(popups.common.actionBar.excludeFromAlbumButton());

            // кнопки Убрать из альбома нет в КМ
            await bro.rightClick(photoSelector);
            await bro.yaWaitForHidden(popups.common.actionPopup.excludeFromAlbumButton());
        }

        hermione.skip.notIn('', 'Мигающий тест https://st.yandex-team.ru/CHEMODAN-68772');
        it('diskclient-5208, diskclient-5583: Убрать файл из слайдера автоальбома', async function() {
            const bro = this.browser;
            const filter = 'beautiful';
            await goToPhotoInAutoAlbum.call(this, filter);

            const photoName = await getRandomPhotoName(bro);
            if (photoName) {
                await copyPhoto.call(this, photoName, getNameForCopy(photoName));

                await goToPhotoInAutoAlbum.call(this, filter, photoName);

                const photoSelector = photo.itemByName().replace(':title', photoName);
                await bro.click(photoSelector);
                await bro.yaWaitForVisible(slider.contentSlider.previewImage());
                await bro.yaWaitForVisible(slider.sliderButtons.excludeFromAlbumButton());
                await bro.click(slider.sliderButtons.excludeFromAlbumButton());

                await bro.pause(500);
                assert.notEqual(await bro.yaGetActiveSliderImageName(), photoName);

                await bro.click(slider.sliderButtons.closeButton());
                await bro.yaWaitForHidden(slider.contentSlider.previewImage());

                await bro.yaWaitForHidden(photoSelector);
            }
        });

        hermione.skip.notIn('', 'Мигающий тест https://st.yandex-team.ru/CHEMODAN-68772');
        it('diskclient-5469: Убрать файл из автоальбома через КМ', async function() {
            const bro = this.browser;
            const filter = 'beautiful';
            await goToPhotoInAutoAlbum.call(this, filter);

            const photoName = await getRandomPhotoName(bro);
            if (photoName) {
                await copyPhoto.call(this, photoName, getNameForCopy(photoName));

                await goToPhotoInAutoAlbum.call(this, filter, photoName);

                const photoSelector = photo.itemByName().replace(':title', photoName);
                await bro.rightClick(photoSelector);
                await bro.yaWaitForVisible(popups.common.actionPopup.excludeFromAlbumButton());
                await bro.click(popups.common.actionPopup.excludeFromAlbumButton());
                await bro.yaWaitForHidden(photoSelector);
            }
        });

        hermione.skip.notIn('', 'Мигающий тест https://st.yandex-team.ru/CHEMODAN-68772');
        it('diskclient-5205, diskclient-5582: Убрать несколько файлов из автоальбома', async function() {
            const bro = this.browser;
            const filter = 'beautiful';
            const isMobile = await bro.yaIsMobile();
            await goToPhotoInAutoAlbum.call(this, filter);

            const photoName1 = await getRandomPhotoName(bro);
            const photoName2 = await getRandomPhotoName(bro);

            if (photoName1 && photoName2) {
                await copyPhoto.call(this, photoName1, getNameForCopy(photoName1));
                await copyPhoto.call(this, photoName2, getNameForCopy(photoName2));

                await goToPhotoInAutoAlbum.call(this, filter);

                await bro.yaSelectPhotoItemByName(photoName1, true, true);
                await bro.yaSelectPhotoItemByName(photoName2, true, true);

                await bro.yaWaitActionBarDisplayed();

                if (isMobile) {
                    await bro.click(popups.common.actionBar.moreButton());
                    await bro.yaWaitForVisible(popups.common.actionBarMorePopup.excludeFromAlbumButton());
                    await bro.click(popups.common.actionBarMorePopup.excludeFromAlbumButton());
                } else {
                    await bro.yaWaitForVisible(popups.common.actionBar.excludeFromAlbumButton());
                    await bro.click(popups.common.actionBar.excludeFromAlbumButton());
                }

                await bro.yaWaitForHidden(photo.itemByName().replace(':title', photoName1));
                await bro.yaWaitForHidden(photo.itemByName().replace(':title', photoName2));
            }
        });

        it('diskclient-5215, diskclient-5591: Нельзя убрать файл общей папки readonly из автоальбома', async function() {
            await goToPhotoInAutoAlbum.call(this, 'unbeautiful');
            await checkNoButtonForPhoto.call(this, '2019-11-28 12-29-55.JPG');
        });

        it('diskclient-5209, diskclient-5584: Нельзя убрать файл из автоальбома Скриншоты', async function() {
            await goToPhotoInAutoAlbum.call(this, 'screenshots');
            await checkNoButtonForPhoto.call(this);
        });

        it('diskclient-5471, diskclient-5585: Нельзя убрать файл из автоальбома Видео', async function() {
            await goToPhotoInAutoAlbum.call(this, 'videos');
            await checkNoButtonForPhoto.call(this);
        });

        it('diskclient-5472, diskclient-5586: Нельзя убрать файл из автоальбома Камера', async function() {
            await goToPhotoInAutoAlbum.call(this, 'camera');
            await checkNoButtonForPhoto.call(this);
        });
    });
});
