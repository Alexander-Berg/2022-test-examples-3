const fs = require('fs');
const util = require('util');
const readFile = util.promisify(fs.readFile);
const popups = require('../page-objects/client-popups');
const slider = require('../page-objects/slider').common;
const navigation = require('../page-objects/client-navigation');
const consts = require('../config').consts;
const listing = require('../page-objects/client-content-listing.js');
const search = require('../page-objects/client-search-form').common;
const docs = require('../page-objects/docs');
const { psHeader } = require('../page-objects/client');
const PageObjects = require('../page-objects/public');
const path = require('path');
const copyFileAsync = util.promisify(fs.copyFile);
const unlinkAsync = util.promisify(fs.unlink);
const { retriable } = require('@ps-int/ufo-hermione/helpers/retries');
const { assert } = require('chai');

/**
 * @typedef { import('@ps-int/ufo-hermione/types').Browser } Browser
 * @typedef { import('@ps-int/ufo-hermione/types').Resource } Resource
 */

/**
 * Генерирует файлы с уникальными именами (и создает их физически путем копирования)
 *
 * @param {string|string[]} files - оригинальные названия файлов
 * @returns {Promise<string|string[]>} сгенерированные имена файлоы
 */
const makeUniqFiles = async(files) => {
    const uniqFileNames = await Promise.all([].concat(files).map((filename, i) => {
        const uniqPrefix = String(Date.now());
        const uniqFilename = `tmp-${uniqPrefix}-${i}-${filename}`;

        const filePath = path.resolve(consts.TEST_FILES_PATH, filename);
        const uniqFilePath = path.resolve(consts.TEST_FILES_PATH, uniqFilename);

        return copyFileAsync(filePath, uniqFilePath).then(() => uniqFilename);
    }));

    return Array.isArray(files) ? uniqFileNames : uniqFileNames[0];
};
/**
 * Удаляет сгенерированные при помощи makeUniqFiles файлы
 *
 * @param {string[]} files - название файла
 * @returns {Promise<void>}
 */
const cleanupUniqFiles = (...files) => Promise.all(
    files.map((filename) => unlinkAsync(path.resolve(consts.TEST_FILES_PATH, filename)))
);

const actions = {
    //desktop-имплементации
    desktop: {
        /**
         * Открывает попап создания, появляющийся по нажатию кнопки "+ Создать" в сайдбаре
         *
         * @returns {Promise<void>}
         */
        async yaOpenCreatePopup() {
            await this.yaWaitForVisible(navigation.desktop.sidebarButtons.create());
            await this.click(navigation.desktop.sidebarButtons.create());
            await this.pause(500);
            return this.yaWaitForVisible(popups.desktop.createPopup());
        },

        /**
         * Открывает диалог создания новой директории (где нужно вводить имя)
         *
         * @param {boolean} [isFromListing=true]
         * @returns {Promise<Browser>}
         */
        async yaOpenCreateDirectoryDialog(isFromListing = true) {
            await this.yaOpenCreatePopup();
            await this.click(popups.desktop.createPopup.createDirectory());
            await this.yaWaitForHidden(popups.desktop.createPopup());
            if (isFromListing) {
                await this.yaWaitForVisible(popups.common.createDialog());
            } else {
                await this.yaWaitForVisible(popups.common.selectFolderPopup());
            }
        },
        /**
         * Вызывает удаление выбранных в данный момент элементов
         *
         * @param {boolean} [fromTrash=false] Удаление из корзины
         * @returns {Promise<Browser>}
         */
        async yaDeleteSelected(fromTrash = false) {
            const buttonSelector = fromTrash ?
                popups.desktop.actionBar.deleteFromTrashButton() :
                popups.common.actionBar.deleteButton();
            await this.yaWaitForVisible(buttonSelector);
            await this.click(buttonSelector);
            await this.yaAcceptDeletePopup();
            await this.yaWaitActionBarHidden();
        },
        /**
         * Открывает публичный доступ к выделенному ресурсу
         *
         * @returns {Promise<string>} ссылка на опубликованный ресурс
         */
        async yaShareSelected() {
            const publishButton = await this.$(popups.desktop.actionBar.publishButton());

            await publishButton.click();

            const input = await this.$(popups.common.shareDialog.textInput());

            return await input.getValue();
        },
        /**
         * Вызывает загрузку одного или нескольких файлов по переданным именам
         * Файлы должны находиться на машине с тестом в директории TEST_FILES_PATH
         *
         * @param {string|string[]} fileNames - название файла
         * @param {Object} opts
         * @param {boolean} [opts.replace=false] - загрузка с заменой
         * @param {boolean} [opts.disableHashes=false] - отключить подсчёт хэшей.
         *                                               Нужно чтобы проверить полный цикл аплоада
         * @param {boolean} [opts.uniq=false] - загрузка с уникальным именем
         * @param {boolean} [opts.dragAndDrop=false] - симулировать перетаскивание файла в клиент
         * @param {boolean} [opts.closeUploader=false] - закрывать загрузчик после загрузки.
         * @param {string} [opts.waitForVisibleSelector]
         * @param {string} [opts.inputSelector]
         * @param {boolean} [opts.selectFolder]
         * @returns {Promise<string|string[]>}
         */
        async yaUploadFiles(fileNames, opts) {
            const options = Object.assign({}, {
                replace: false,
                disableHashes: false,
                uniq: false,
                dragAndDrop: false,
                closeUploader: true,
                waitForVisibleSelector: navigation.desktop.sidebarButtons(),
                inputSelector: navigation.desktop.sidebarButtons.upload.input(),
                selectFolder: false
            }, opts);

            if (options.target === 'docs-sidebar') {
                Object.assign(options, {
                    waitForVisibleSelector: docs.desktop.docsSidebar.createButton(),
                    inputSelector: docs.desktop.docsSidebarCreatePopup.upload(),
                });

                await this.yaWaitForVisible(options.waitForVisibleSelector);
                await this.click(options.waitForVisibleSelector);
                await this.yaWaitForVisible(docs.desktop.docsSidebarCreatePopup());
            }

            await this.yaWaitForVisible(options.waitForVisibleSelector);

            if (options.disableHashes) {
                await this.execute(() => {
                    window.__DISABLE_UPLOAD_HASHES = true;
                });
            }

            const uploadedFilenames = options.dragAndDrop ?
                await this.yaDragAndDropFiles(fileNames) :
                await this.doUpload(fileNames, options.inputSelector, options.uniq);

            if (options.selectFolder) {
                await this.waitForEnabled(popups.common.selectFolderDialog.acceptButton());
                await this.click(popups.common.selectFolderDialog.acceptButton());
            }

            await this.yaWaitForVisible(popups.common.uploader());

            await this.waitUntil(async function() {
                const title = await this.getText(popups.common.uploader.progressText());
                return title.startsWith(consts.TEXT_UPLOAD_DIALOG_UPLOAD);
            }, consts.FILE_OPERATIONS_TIMEOUT, 'Загрузка файла не началась');

            if (options.replace) {
                await this.yaWaitForVisible(popups.common.uploader.replaceButton());
                this.click(popups.common.uploader.replaceButton());
            }

            await this.waitUntil(async function() {
                const title = await this.getText(popups.common.uploader.progressText());
                return title === consts.TEXT_UPLOAD_DIALOG_UPLOAD_COMPLETE;
            }, consts.FILE_OPERATIONS_TIMEOUT, 'Загрузка файла не завершилась');

            if (options.closeUploader) {
                await this.click(popups.common.uploader.closeButton());
                await this.yaWaitForHidden(popups.common.uploader());
            }

            return uploadedFilenames;
        },
        /**
         * Авторизуемся под пользователем, переходим на нужный url, дожидаемся загрузки страницы
         *
         * @param {string} login
         * @param {string} url
         * @param {string} selector
         * @returns {Promise<void>}
         */
        async getReady(login, url, selector) {
            await this.yaClientLoginFast(login);
            await this.url(url);
            await this.yaWaitForVisible(selector);
        },
        /**
         * @param {string} startSelector
         * @param {string} finishSelector
         * @param {Function} [onBeforeDrop]
         * @returns {Promise<void>}
         */
        async yaDragAndDrop(startSelector, finishSelector, onBeforeDrop) {
            await this.moveToObject(startSelector);
            await this.buttonDown(0);
            await this.moveToObject(finishSelector);
            if (onBeforeDrop) {
                await onBeforeDrop.call(this);
            }
            await this.buttonUp(0);
        },

        /**
         * Открывает контексное меню файла
         *
         * @param {string} fileName
         * @returns {Promise<void>}
         */
        async yaOpenActionPopup(fileName) {
            await this.rightClick(listing.common.listingBodyItemsIconXpath().replace(/:titleText/g, fileName));
            await this.yaWaitForVisible(popups.common.actionPopup());
        },

        /**
         * Симулирует перетаскивание файла в клиент
         *
         * @param {string|string[]} fileNames
         * @param {number} [offsetX]
         * @param {number} [offsetY]
         * @returns {string}
         */
        async yaDragAndDropFiles(fileNames, offsetX = 0, offsetY = 0) {
            await this.execute((targetName, offsetX, offsetY) => {
                const target = document.getElementsByClassName(targetName)[0];

                const input = document.createElement('INPUT');
                input.type = 'file';
                input.multiple = true;
                input.className = 'TEMP_INPUT';
                input.style.display = 'none';
                input.onchange = function() {
                    const rect = target.getBoundingClientRect();
                    const x = rect.left + (offsetX || (rect.width >> 1));
                    const y = rect.top + (offsetY || (rect.height >> 1));
                    const dataTransfer = { files: this.files, types: ['Files'] };
                    const dispatchMouseEvent = (name, target) => {
                        const event = document.createEvent('MouseEvent');
                        event.initMouseEvent(name, true, true, window, 0, 0, 0, x, y);
                        event.dataTransfer = dataTransfer;
                        target.dispatchEvent(event);
                    };
                    dispatchMouseEvent('dragenter', target);

                    setTimeout(() => {
                        ['dragover', 'drop'].forEach((name) => {
                            const dropzone = document.getElementsByClassName('nb-dropzone')[0];
                            dispatchMouseEvent(name, dropzone);
                        });
                        document.body.removeChild(input);
                    }, 100);
                };
                document.body.appendChild(input);
                return input;
            }, 'm-client', offsetX, offsetY);

            await this.pause(100);
            return await this.doUpload(fileNames, '.TEMP_INPUT');
        },
        /**
         * Создает txt файлы нужных (больших) размеров в браузере и начинает их загрузку через dnd
         * Работает только для десктопа
         *
         * @param {number|Array<number>} sizes - размер(ы) в мегабайтах, до 2 ГБ
         * @returns {Promise<Array<string>>}
         */
        async yaUploadHugeFilesViaDragAndDrop(sizes) {
            return (await this.executeAsync((sizes, done) => {
                // у FilesList нет конструктора, поэтому используется DataTransfer
                const dt = new DataTransfer();
                const fileNames = [];
                if (!Array.isArray(sizes)) {
                    sizes = [].concat([sizes]);
                }
                for (const size of sizes) {
                    const garbage = new Uint8Array(1024 * 1024 * size);
                    const file = new File([garbage], `test-file-${size}mb.txt`,
                        { type: 'text/plain' });
                    dt.items.add(file);
                    fileNames.push(`test-file-${size}mb.txt`);
                }

                const dataTransfer = { files: dt.files, types: ['Files'] };

                const dispatchDnDEvent = (name, targetName) => {
                    const target = document.querySelector(targetName);
                    const event = document.createEvent('MouseEvent');
                    event.initMouseEvent(name, true, true);
                    event.dataTransfer = dataTransfer;
                    target.dispatchEvent(event);
                };

                dispatchDnDEvent('dragenter', '.draganddrop');

                const endDragAndDrop = () => {
                    setTimeout(() => {
                        const dropzone = document.querySelector('.nb-dropzone');
                        if (!dropzone) {
                            endDragAndDrop();
                        } else {
                            ['dragover', 'drop'].forEach((name) => {
                                dispatchDnDEvent(name, '.nb-dropzone');
                            });
                            done(fileNames);
                        }
                    }, 100);
                };
                endDragAndDrop();
            }, sizes));
        },

        /**
         * Функция для проверки текста над индикатором места
         *
         * @param {string} text
         * @returns {Promise<void>}
         */
        async yaAssertMemoryIndicatorText(text) {
            await this.yaWaitForVisible(navigation.desktop.spaceInfoSection.infoSpaceText());

            const memoryIndicatorText = await this.getText(navigation.desktop.spaceInfoSection.infoSpaceText());
            assert.equal(memoryIndicatorText, text);
        }
    },
    //touch-имплементации
    touch: {
        /**
         * Открывает контексное меню файла
         *
         * @param {string} fileName
         * @returns {Promise<void>}
         */
        async yaOpenActionPopup(fileName) {
            const itemSelector = listing.common.listingBodyItemsIconXpath().replace(/:titleText/g, fileName);
            await this.yaLongPress(itemSelector);
            await this.yaWaitForVisible(popups.common.actionPopup());
        },
        /**
         * Открывает диалог создания новой директории (где нужно вводить имя)
         *
         * @returns {Promise<Browser>}
         */
        async yaOpenCreateDirectoryDialog() {
            //убеждаемся, что кнопка не перекрыта action-bar'ом
            await this.scroll(0, 0);
            await this.yaWaitForVisible(navigation.touch.touchListingSettings.plus());
            await this.click(navigation.touch.touchListingSettings.plus());
            await this.yaWaitForVisible(popups.touch.createPopup.createDirectory());
            //wait for animation end
            await this.pause(200);
            await this.click(popups.touch.createPopup.createDirectory());
            await this.yaWaitForHidden(popups.touch.createPopup());
            await this.yaWaitForVisible(popups.common.createDialog());
        },
        /**
         * Вызывает удаление в данный момент выбранных элементов
         *
         * @param {boolean} [fromTrash=false] Удаление из корзины
         * @returns {Promise<Browser>}
         */
        async yaDeleteSelected(fromTrash = false) {
            if (fromTrash) {
                await this.yaWaitForVisible(popups.touch.actionBar.deleteFromTrashButton());
                await this.click(popups.touch.actionBar.deleteFromTrashButton());
            } else {
                await this.yaWaitActionBarDisplayed();
                const isDeleteButtonVisible = await this.isVisible(popups.common.actionBar.deleteButton());
                await this.yaCallActionInActionBar('delete', !isDeleteButtonVisible);
            }

            await this.yaAcceptDeletePopup();
            return await this.yaWaitActionBarHidden();
        },
        /**
         * Открывает публичный доступ к выделенному ресурсу
         *
         * @returns {Promise<string>} ссылка на опубликованный ресурс
         */
        async yaShareSelected() {
            await this.yaWaitForVisible(popups.common.actionBar());
            await this.click(popups.touch.actionBar.publishButton());
            await this.yaWaitForVisible(popups.common.shareDialog.textInput());

            return (await this.getValue(popups.common.shareDialog.textInput()));
        },
        /**
         * Вызывает загрузку одного или нескольких файлов по переданным именам
         * Файлы должны находиться на машине с тестом в директории TEST_FILES_PATH
         *
         * @param {string|string[]} fileNames - название файла
         * @param {Object} opts
         * @param {boolean} [opts.uniq=false] - загрузка с уникальным именем
         * @param {boolean} [opts.closeUploader=false] - закрывать загрузчик после загрузки
         * @returns {Promise<string|string[]>}
         */
        async yaUploadFiles(fileNames, opts) {
            const options = Object.assign({}, {
                uniq: false,
                closeUploader: true,
                inputSelector: popups.touch.createPopup.uploadFile.input(),
                target: 'listing-plus'
            }, opts);

            if (options.target === 'listing-plus') {
                await this.yaWaitForVisible(navigation.touch.touchListingSettings.plus());
                await this.click(navigation.touch.touchListingSettings.plus());
                await this.pause(500); // Drawer animation
                await this.yaWaitForVisible(popups.touch.createPopup.uploadFile());
            } else if (options.target === 'docs-plus') {
                options.inputSelector = docs.touch.docsCreateDrawer.upload.input();
                await this.yaWaitForVisible(docs.touch.docsCreateButton());
                await this.click(docs.touch.docsCreateButton());
                await this.yaWaitForVisible(docs.touch.docsCreateDrawer());
                await this.pause(500); // Drawer animation
                await this.click(docs.touch.docsCreateDrawer.upload());
                await this.yaWaitForHidden(docs.touch.docsCreateDrawer());
            }

            const uploadedFilenames = await this.doUpload(fileNames, options.inputSelector, options.uniq);

            if (options.selectFolder) {
                await this.waitForEnabled(popups.common.selectFolderDialog.acceptButton());
                await this.click(popups.common.selectFolderDialog.acceptButton());
            }

            await this.yaWaitForVisible(popups.common.uploader());

            await this.waitUntil(async function() {
                const title = await this.getText(popups.common.uploader.progressText());
                return title.startsWith(consts.TEXT_UPLOAD_DIALOG_UPLOAD);
            }, consts.FILE_OPERATIONS_TIMEOUT, 'Загрузка файла не началась');

            if (options.replace) {
                await this.yaWaitForVisible(popups.common.uploader.replaceButton());
                this.click(popups.common.uploader.replaceButton());
            }

            await this.waitUntil(async function() {
                const title = await this.getText(popups.common.uploader.progressText());
                return title === consts.TEXT_UPLOAD_DIALOG_UPLOAD_COMPLETE;
            }, consts.FILE_OPERATIONS_TIMEOUT, 'Загрузка файла не завершилась');

            if (options.closeUploader) {
                await this.click(popups.common.uploader.closeButton());
                await this.yaWaitForHidden(popups.common.uploader());
            }

            return uploadedFilenames;
        },
        /**
         * Авторизуемся под пользователем, переходим на нужный url, дожидаемся загрузки страницы
         *
         * @param {{ login: string, password: string }} user
         * @param {string} url
         * @param {string} selector
         * @returns {Promise<void>}
         */
        async getReady(user, url, selector) {
            await this.yaClientLoginFast(user);
            await this.url(url);
            await this.yaWaitForVisible(selector);
        },
        /**
         * Закрывает открытый попап и дожидается пока он не исчезнет.
         *
         * @returns {Promise<void>}
         */
        async yaCloseVisibleMobilePane() {
            if (await this.isExisting(popups.touch.mobilePaneVisible())) {
                await this.yaExecuteClick(navigation.touch.modalCell());
                await this.yaWaitForHidden(popups.touch.mobilePaneVisible());
            }
        }
    },
    //общие имплементации для desktop и touch
    common: {
        // resources-action-bar_visible
        async yaIsActionBarDisplayed() {
            const actionBar = await this.$(popups.common.actionBar());

            return (await actionBar.getAttribute('class')).includes('resources-action-bar_visible');
        },
        async yaIsActionBarHidden() {
            const actionBar = await this.$(popups.common.actionBar());

            return !(await actionBar.getAttribute('class')).includes('resources-action-bar_visible');
        },
        async yaWaitActionBarDisplayed() {
            const actionBar = await this.$(popups.common.actionBar());
            await this.waitUntil(async () => {
                return (await actionBar.getAttribute('class')).includes('resources-action-bar_visible');
            });
        },
        async yaWaitActionBarHidden() {
            const actionBar = await this.$(popups.common.actionBar());
            await this.waitUntil(async () => {
                return !(await actionBar.getAttribute('class')).includes('resources-action-bar_visible');
            });
        },
        /**
         * Вызывает действие из контекстного меню файла
         *
         * @param {string} fileName
         * @param {"view"|"publish"|"rename"|"addToAlbum"|"addToCurrentAlbum"|"excludeFromAlbum"|"goToFile"} action
         * @param {boolean} [waitForPopupHidden=true]
         * @returns {Promise<void>}
         */
        async yaCallActionInActionPopup(fileName, action, waitForPopupHidden = true) {
            await this.yaOpenActionPopup(fileName);
            await this.click(popups.common.actionPopup[`${action}Button`]());
            if (waitForPopupHidden) {
                await this.yaWaitForHidden(popups.common.actionPopup());
            }
        },

        /**
         * Вызывает ввод имени директории и нажатие кнопки "Сохранить"
         * в открытом окне создания/переименования
         *
         * @param {string} resourceName
         * @param {Object} opts
         * @param {boolean} [opts.isDialogWillBeHidden=true]
         * @param {boolean} [opts.needToClick=true]
         * @returns {Promise<Browser>}
         */
        async yaSetResourceNameAndApply(resourceName, opts) {
            const options = Object.assign({ isDialogWillBeHidden: true, needToClick: true }, opts);
            const selector = popups.common.createDialog.nameInput();

            const input = await this.$(selector);
            const dialog = await this.$(popups.common.createDialog());

            await dialog.waitForDisplayed();

            await this.waitUntil(async () => {
                return await input.isFocused();
            });

            await this.yaSetValue(selector, resourceName);

            if (options.needToClick) {
                const submit = await this.$(popups.common.createDialog.submitButton());

                await submit.click();
            }

            if (options.isDialogWillBeHidden) {
                await dialog.waitForDisplayed({
                    reverse: true,
                    timeout: consts.FILE_OPERATIONS_TIMEOUT,
                    timeoutMsg: 'Диалог переименования не исчез'
                });
            }
        },

        /**
         * Выбирает папку в диалоге выбора папки и нажимает кнопку подтверждения
         *
         * @param {...string} folderNames - массив из названий папок от корня до той папки, которую надо выбрать
         * @returns {Promise<void>}
         */
        async yaSelectFolderInDialogAndApply(...folderNames) {
            const tree = await this.$(popups.common.selectFolderDialog.treeContent());

            await tree.waitForExist({ timeout: 5000 });

            const tooltip = await this.$(popups.common.selectFolderPopup.warningTooltip());

            if (await tooltip.isDisplayed()) {
                const hideButton = await this.$(popups.common.selectFolderPopup.warningTooltip.hide());

                await hideButton.click();
                await tooltip.waitForDisplayed({ reverse: true });
            }

            if (folderNames[0] === undefined) {
                folderNames[0] = 'Файлы';
            }

            await retriable(async() => {
                for (let i = 0; i < folderNames.length; ++i) {
                    const folderName = folderNames[i];

                    if (i + 1 === folderNames.length) { // клик по конечной папке
                        await this.click(
                            popups.common.selectFolderDialogItemsXpath().replace(':titleText', folderName)
                        );
                    } else { // клик по кнопке, открывающей содержимое папки
                        const element = await this.element(
                            popups.common.selectFolderDialogItemsToggleButtonXpath().replace(':titleText', folderName)
                        );
                        await this.yaExecuteClickOnElement(element);
                        await this.yaWaitForHidden(
                            popups.common.selectFolderDialogItemsTreeWaitingXpath().replace(':titleText', folderName)
                        );
                    }
                }

                await this.waitForEnabled(popups.common.selectFolderDialog.acceptButton());
                await this.click(popups.common.selectFolderDialog.acceptButton());
                await this.yaWaitForHidden(popups.common.selectFolderDialog());
            }, 10, 1000);
        },

        /**
         * Вызывает операцию в топбаре
         *
         * @param {'copy'|'move'|'rename'|'delete'|'publish'|'edit'|
         *         'download'|'deleteFromTrash'|'restoreFromTrash'|
         *         'createAlbum'|'addToAlbum'|'setAsCover'} action - тип операции
         * @param {boolean} [useMoreButton] действие прячется за точками
         * @returns {Promise<void>}
         */
        async yaCallActionInActionBar(action, useMoreButton) {
            const isMobile = await this.yaIsMobile();

            await this.yaWaitActionBarDisplayed();
            if (['publish', 'download', 'deleteFromTrash', 'restoreFromTrash'].includes(action)) {
                return this.click(popups[isMobile ? 'touch' : 'desktop'].actionBar[`${action}Button`]());
            }

            if (isMobile && typeof useMoreButton === 'undefined' || useMoreButton) {
                await this.click(popups.common.actionBar.moreButton());
                await this.yaWaitForVisible(popups.common.actionBarMorePopup[`${action}Button`]());
                await this.pause(200); // анимация модалки
                await this.click(popups.common.actionBarMorePopup[`${action}Button`]());
                await this.yaWaitForHidden(popups.common.actionBarMorePopup());
            } else {
                await this.click(popups.common.actionBar[`${action}Button`]());
            }
        },

        /**
         * @param {string} action
         * @returns {Promise<void>}
         */
        async yaCallActionInSlider(action) {
            const actionsInMorePopup = ['move', 'rename', 'setAsCover', 'addToAlbum'];
            if (actionsInMorePopup.includes(action)) {
                await this.yaWaitForVisible(slider.sliderButtons.moreButton());
                await this.click(slider.sliderButtons.moreButton());
                await this.yaWaitForVisible(slider.sliderMoreButtonPopup());
                await this.pause(200);
                await this.click(slider.sliderMoreButtonPopup[`${action}Button`]());
            } else {
                await this.yaWaitForVisible(slider.sliderButtons[`${action}Button`]());
                await this.click(slider.sliderButtons[`${action}Button`]());
            }
        },

        /**
         * Возвращает тип выбранного ресурса (file, dir, etc.)
         *
         * @returns {Promise<string|string[]>}
         */
        async yaSelectedResourceType() {
            const getResourceType = (className) => {
                const regexp = new RegExp(/listing-item_type_(\w+)/);
                const [, result] = className.match(regexp) || [];
                return result;
            };

            const className = await this.getAttribute(listing.common.listing.item_selected(), 'class');
            if (!Array.isArray(className)) {
                return getResourceType(className);
            }
            return className.map(getResourceType);
        },

        /**
         * Проверяет, был ли выбран ресурс
         *
         * @param {string} [resourceName] - имя ресурса
         * @returns {Promise<boolean>}
         */
        async yaIsResourceSelected(resourceName) {
            if (resourceName) {
                return this.isExisting(
                    listing.common.listingBodySelectedItemsInfoXpath()
                        .replace(/:titleText/g, resourceName)
                );
            }
            return this.isExisting(listing.common.listing.item_selected());
        },

        /**
         * Вызывает операцию копирования в топбаре. Если передан folderName - выбрет нужную папку
         *
         * @param {string} [folderName] - название папки куда нужно скопировать выделенные ресурсы
         * @returns {Promise<void>}
         */
        async yaCopySelected(folderName) {
            await this.yaCallActionInActionBar('copy');
            await this.yaWaitForVisible(popups.common.selectFolderDialog());
            await this.yaSelectFolderInDialogAndApply(folderName);
            await this.yaWaitForHidden(popups.common.selectFolderDialog());
        },

        /**
         * Вызывает переименование выбранного элемента
         *
         * @param {string} newResourceName - новое название ресурса
         * @returns {Promise<void>}
         */
        async yaRenameSelected(newResourceName) {
            await this.yaCallActionInActionBar('rename');
            await this.yaWaitForVisible(popups.common.createDialog());
            await this.yaSetValue(popups.common.createDialog.nameInput(), newResourceName);
            await this.click(popups.common.createDialog.submitButton());
            await this.yaWaitForHidden(popups.common.createDialog());
        },

        /**
         * Вызывает операцию перемещения в топбаре. Если передан folderName - выбрет нужную папку
         *
         * @param {string} [folderName] - название папки куда нужно переместить выделенные ресурсы
         * @returns {Promise<void>}
         */
        async yaMoveSelected(...folderName) {
            await this.yaCallActionInActionBar('move');
            await this.yaWaitForVisible(popups.common.selectFolderDialog());
            await this.yaSelectFolderInDialogAndApply(...folderName);
            await this.yaWaitForHidden(popups.common.selectFolderDialog());
        },
        /**
         * Вызывает удаление элемента по заданному имени в открытой директории.
         * Если удаляемый элемент не найден, выбрасывается ошибка.
         *
         * @param {string} resourceName
         * @param {Object} options
         * @param {boolean} options.trash Удаление производится в корзине
         * @returns {Promise<void>}
         */
        async yaDeleteResource(resourceName, { trash = false } = {}) {
            const relativeSelector = listing.common.listingBodyItemsInfoXpath().replace(/:titleText/g, resourceName);

            await this.yaScrollToEnd();
            await this.yaSelectResource(resourceName);

            const selectedType = await this.yaSelectedResourceType();
            const notificationTemplate = selectedType === 'dir' ?
                consts.TEXT_NOTIFICATION_FOLDER_MOVED_TO_TRASH :
                consts.TEXT_NOTIFICATION_FILE_MOVED_TO_TRASH;

            await this.yaDeleteSelected(trash);
            await this.yaAssertProgressBarAppeared();
            await this.yaAssertProgressBarDisappeared();
            await this.yaWaitForHidden(relativeSelector);

            return trash ?
                undefined :
                this.yaWaitNotificationForResource(resourceName, notificationTemplate);
        },
        /**
         * Вызывает удаление списка элементов по заданным именам в открытой директории
         * Если любой из элементов не найден и не передан ключ safe, выбрасывается ошибка
         *
         * @param {string[]} resourceNames
         * @param {Object} options
         * @param {boolean} [options.fast=false] Использовать ns.Model
         * @param {boolean} [options.safe=false] Проверять на наличие в листинге
         * @param {boolean} [options.trash=false] Удаление производится в корзине
         * @returns {Promise<Browser>}
         */
        async yaDeleteResources(resourceNames, { fast = false, safe = false, trash = false } = {}) {
            if (fast) {
                return this._yaDeleteResourcesFast(resourceNames, { safe, trash });
            }

            let resources = [].concat(resourceNames);

            try {
                await Promise.all(resources.map((resource) => this.yaAssertListingHas(resource)));
            } catch (error) {
                await this.yaScrollToEnd();
            }

            if (safe) {
                const allResourcesInListing = await this.yaGetListingElementsTitles();
                resources = resources.filter((resource) => allResourcesInListing.includes(resource));
            }
            if (!resources.length) {
                return;
            }

            await this.yaSelectResources(resources);
            await this.yaDeleteSelected(trash);
            await this.yaAssertProgressBarAppeared();
            await this.yaAssertProgressBarDisappeared();
        },

        /**
         * @param {number} amount
         * @param {string} [idContext]
         * @returns {Promise<Object[]>}
         */
        async fetchResources(amount, idContext) {
            return (await this.executeAsync((amount, idContext, done) => {
                window.rawFetchModel('resources', {
                    idContext: idContext || ns.page.current.params.idContext,
                    offset: 0,
                    amount
                }).then(
                    ({ resources }) => done(resources),
                    () => done()
                );
            }, amount, idContext));
        },

        /**
         * Вызывает удаление списка элементов по заданным именам в открытой директории c использованием ns.Model
         * Если любой из элементов не найден и не передан ключ safe, выбрасывается ошибка
         *
         * @param {string|string[]} resourceNames
         * @param {Object} options
         * @param {boolean} [options.safe=false] Проверять на наличие в листинге
         * @param {boolean} [options.trash=false] Удаление производится в корзине
         * @returns {*}
         */
        async _yaDeleteResourcesFast(resourceNames, { safe = false, trash = false }) {
            const resources = await this.fetchResources(100);
            if (!resources) {
                return this.yaDeleteResources(resourceNames, { safe, trash });
            }

            resourceNames = [].concat(resourceNames);
            if (safe) {
                resourceNames = resources.filter((element) => resourceNames.includes(element.name));
            }
            if (!resourceNames.length) {
                return;
            }

            const results = (await Promise.all(resourceNames.map(({ id }) =>
                this.executeAsync((id, done) => {
                    window.rawFetchModel('do-resource-delete', { id })
                        .then(
                            () => done(0),
                            (error) => error && error.id === 'HTTP_423' ? done(0) : done(1)
                        );
                }, id)
            )));

            if (results.some(Boolean)) {
                throw new Error('Не все файлы были удалены');
            }
        },
        /**
         * Вызывает удаление всех элементов в открытой директории.
         * Если в открытой директории нет элементов, функция вернет управление.
         *
         * @returns {Promise<Browser>}
         */
        async yaDeleteAllResources() {
            await this.yaScrollToEnd();

            const isSelected = await this.yaSelectAll();

            if (isSelected) {
                await this.yaDeleteSelected();
                await this.yaAssertProgressBarAppeared();
                await this.yaAssertProgressBarDisappeared();
                await this.yaCloseAllNotifications();
            }
        },
        /**
         * Возвращает true если текущий листинг не пуст
         *
         * @returns {Promise<boolean>}
         */
        async yaListingNotEmpty() {
            return await this.$(listing.common.listingBody.itemsWithoutTrash()).isExisting();
        },
        /**
         * Восстанавливает все ресурсы из корзины если они есть
         *
         * @returns {Promise<void>}
         */
        async yaRestoreAllFromTrash() {
            await this.url('/client/trash');
            await this.yaWaitForHidden(listing.common.listingSpinner());

            if (await this.yaListingNotEmpty()) {
                await this.yaSelectAll();
                await this.yaWaitActionBarDisplayed();
                const isMobile = await this.yaIsMobile();

                if (isMobile) {
                    await this.click(popups.touch.actionBar.restoreFromTrashButton());
                } else {
                    await this.click(popups.common.actionBar.restoreFromTrashButton());
                }

                await this.yaAssertProgressBarAppeared();
                await this.yaAssertProgressBarDisappeared();
            }
        },

        /**
         * Очищает корзину если в ней что-то есть
         *
         * @param {?boolean} [needRedirect=true] - нужно ли делать редирект в корзину
         *
         * @returns {Promise<void>}
         */
        async yaCleanTrash(needRedirect = true) {
            if (needRedirect) {
                await this.url('/client/trash');
            }
            await this.yaWaitForHidden(listing.common.listingSpinner(), 10000);

            if (await this.yaListingNotEmpty()) {
                await this.click(listing.common.listing.cleanTrash());
                await this.yaWaitForVisible(popups.common.cleanTrashPopup(), consts.FILE_OPERATIONS_TIMEOUT);
                await this.click(popups.common.cleanTrashPopup.acceptButton());
                await this.yaWaitNotificationWithText(
                    consts.TEXT_NOTIFICATION_TRASH_CLEAN,
                    consts.FILE_OPERATIONS_TIMEOUT
                );
            }
        },
        /**
         * Вызывает создание новой директории в открытой директории
         *
         * @param {string} folderName
         * @param {?string} [section='disk'] - раздел в котором надо создать папку. Если null,
         *                                     то папка будет создана в текущем разделе
         * @returns {Promise<void>}
         */
        async yaCreateFolder(folderName, section = 'disk') {
            if (section) {
                try {
                    await this.yaAssertSectionOpened(section);
                } catch (error) {
                    await this.yaOpenSection(section);
                }
            }
            // Диалог создания новой папки закрывается, если придет пуш о создании папки из другой вкладки.
            // Параллельные тесты могу друг друга крашить по этой причине.
            // Ретраи решают проблему.
            // Задача - https://st.yandex-team.ru/CHEMODAN-66814
            await retriable(async() => {
                await this.yaOpenCreateDirectoryDialog();
                await this.yaSetResourceNameAndApply(folderName);
                await this.yaWaitNotificationForResource(folderName, consts.TEXT_NOTIFICATION_FOLDER_CREATED);
            }, 10, 1000);
        },
        /**
         * Вызывает создание списка новых директорий в открытой директории
         *
         * @param {string[]} folderNames
         * @param {?string} [section='disk'] - раздел в котором надо создать папку. Если null,
         *                                     то папка будет создана в текущем разделе
         * @returns {Promise<Browser>}
         */
        async yaCreateFolders(folderNames, section = 'disk') {
            for (const name of folderNames) {
                await this.yaCreateFolder(name, section);
            }
        },
        /**
         * Удаляет ресурс насовсем (сначала в корзину, а потом из нее)
         *
         * @param {string|string[]} resource
         * @param {Object} options
         * @param {boolean} options.safe - делать проверку на наличие файла в листинге
         * @returns {Promise<void>}
         */
        async yaDeleteCompletely(resource, { safe = false, fast = false } = {}) {
            await this.yaDeleteResources(resource, { safe, fast });
            await this.yaOpenSection('trash');
            await this.yaDeleteResources(resource, { safe, fast, trash: true });
        },
        /**
         * Выбирает фотографию в фотосрезе по селектору и переименовывает ее
         *
         * @param {string} itemSelector - селектор фотографии
         * @param {string} newFileName - новое имя фотографии
         * @returns {Promise<void>}
         */
        async selectAndRenamePhoto(itemSelector, newFileName) {
            await retriable(async() => {
                await this.yaSelectPhotosliceItem(itemSelector, true, true);

                await this.yaCallActionInActionBar('rename');
                await this.yaSetResourceNameAndApply(newFileName);
                await this.yaAssertProgressBarAppeared();
                await this.yaAssertProgressBarDisappeared();
            }, 10, 1000);
        },
        /**
         * @param {string|string[]} fileNames
         * @param {string} fileInputSelector
         * @param {boolean} uniq
         * @returns {Promise<Browser|void|string>}
         */
        async doUpload(fileNames, fileInputSelector, uniq = false) {
            const uploadFunc = Array.isArray(fileNames) ?
                this._doUploadMultiple.bind(this) :
                this._doUploadSingle.bind(this);

            if (!uniq) {
                return uploadFunc(fileNames, fileInputSelector);
            }

            const uniqFileNames = await makeUniqFiles(fileNames);

            await uploadFunc(uniqFileNames, fileInputSelector);
            if (Array.isArray(uniqFileNames)) {
                await cleanupUniqFiles(...uniqFileNames);
            } else {
                await cleanupUniqFiles(uniqFileNames);
            }

            return uniqFileNames;
        },
        /**
         * Выполняет загрузку одного файла
         * на селениум сервер и дальнейшую загрузку на диск через интерфейс
         *
         * @param {string} fileName
         * @param {string} fileInputSelector
         * @returns {Promise<Browser>}
         * @private
         */
        async _doUploadSingle(fileName, fileInputSelector) {
            const filePath = path.resolve(consts.TEST_FILES_PATH, fileName);
            const isAndroid = await this.yaIsAndroid();
            if (isAndroid) {
                const androidPath = await this._doUploadFileOnAndroid(filePath);
                return this.addValue(fileInputSelector, androidPath);
            }
            return this.chooseFile(fileInputSelector, filePath);
        },

        /**
         * Выполняет загрузку одного файла
         * на девайс и дальнейшую загрузку на диск через интерфейс
         *
         * @param {string} filePath
         * @returns {Promise<string>}
         * @private
         */
        async _doUploadFileOnAndroid(filePath) {
            const fileData = await readFile(filePath, 'base64');
            const androidPath = path.join(consts.ANDROID_FILES_PATH, path.basename(filePath));
            await this.pushFile(androidPath, fileData);
            return androidPath;
        },
        /**
         * Выполняет загрузку одного файла
         * на девайс или Селениум сервер
         *
         * @param {string} filePath
         * @returns {Promise<string|Browser>}
         * @private
         */
        async _doUploadFile(filePath) {
            return await this.yaIsAndroid() ?
                this._doUploadFileOnAndroid(filePath) :
                (await this.uploadFile(filePath));
        },
        /**
         * Выполняет загрузку списка файлов
         * на селениум сервер/девайс и дальнейшую загрузку на диск через интерфейс
         *
         * @param {string} fileNames
         * @param {string} fileInputSelector
         * @returns {Promise<void>}
         * @private
         */
        async _doUploadMultiple(fileNames, fileInputSelector) {
            const uploadResults = await Promise.all(
                fileNames.map((name) => {
                    const filePath = path.resolve(consts.TEST_FILES_PATH, name);
                    return this._doUploadFile(filePath);
                })
            );

            //передача в input[type="file"] значения с несколькими файлами
            //через конкатенацию c '\n' - стандарт webdriver protocol
            //но возможно имплементирован не во всех драйверах
            const filePaths = uploadResults.join('\n');
            const fileInput = await this.$(fileInputSelector);

            await fileInput.setValue(filePaths);
        },
        /**
         * Получает ссылку на расшаренный файл и проверяет его доступность
         *
         * @returns {Promise<void>}
         * @private
         */
        async yaGetPublicUrlAndCloseTab() {
            const publicUrl = await this.getValue(popups.common.shareDialog.textInput());
            await this.newWindow(publicUrl);
            await this.yaWaitForVisible(PageObjects.publicMain());
            await this.yaWaitForHidden(PageObjects.error());
            await this.close();
            await this.yaWaitForVisible(popups.common.shareDialog.textInput());
        },
        /**
         * Функция для перехода в папку и ожидания спинера
         *
         * @param {string} folderName
         * @returns {Promise<void>}
         */
        async yaGoToFolderAndWaitForListingSpinnerHide(folderName) {
            await this.yaOpenListingElement(folderName);
            await this.yaWaitForHidden(listing.common.listingSpinner());
        },
        /**
         * Ищет файлы через поисковую строку по переданному аргументу
         *
         * @param {string} fileName
         * @returns {Promise<void>}
         */
        async yaSearch(fileName) {
            const isMobile = await this.yaIsMobile();
            const searchForm = isMobile ? search.searchForm() : psHeader.suggest();
            await this.yaWaitForVisible(searchForm);
            await this.click(searchForm);
            await this.yaSetValue(isMobile ? search.searchForm.input() : psHeader.suggest.input(), fileName);
            await this.keys('Enter');
        },
        async yaGetDownloadUrlFromAction(downloadAction) {
            await downloadAction();
            await this.pause(500);
            return await this.retrieveUrlFromIframe(false);
        },
        /**
         * @param {string} user
         * @param {string} [folderName]
         * @returns {string}
         */
        async yaLoginAndCreateFolder(user, folderName = 'tmp-' + Date.now()) {
            await this.yaClientLoginFast(user);
            await this.url(consts.NAVIGATION.disk.url);
            await this.yaCreateFolder(folderName);
            await this.yaAssertListingHas(folderName);
            return folderName;
        },
        /**
         * Одобрить удаление файлов (на случай общих папок и т.п.)
         *
         * @returns {Promise<void>}
         */
        async yaAcceptDeletePopup() {
            const hasPopup = await this.isVisible(popups.common.deletePopup());
            if (hasPopup) {
                const canDeleteAll = await this.isVisible(popups.common.deletePopup.deleteAllButton());
                if (canDeleteAll) {
                    await this.click(popups.common.deletePopup.deleteAllButton());
                } else {
                    await this.click(popups.common.deletePopup.acceptButton());
                }
                await this.yaWaitForHidden(popups.common.deletePopup());
            }
        },

        async yaDeletePublicLinkInShareDialog() {
            await this.click(popups.common.shareDialog.trashButton());
            await this.yaWaitForHidden(popups.common.shareDialog());
            await this.yaWaitForVisible(popups.common.confirmDialog());
            await this.click(popups.common.confirmDialog.submitButton());
            await this.yaWaitForHidden(popups.common.confirmDialog());
        }
    }
};

module.exports = exports = actions;
