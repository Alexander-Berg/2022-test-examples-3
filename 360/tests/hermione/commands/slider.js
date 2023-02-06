const slider = require('../page-objects/slider');

/**
 * @typedef { import('@ps-int/ufo-hermione/types').Browser } Browser
 */

/**
 * @this Browser
 * @returns {Promise<string>}
 */
const yaGetActiveSliderImageName = async function() {
    return (await this.execute((itemSelector) => {
        const src = document.querySelector(itemSelector).getAttribute('src');
        return decodeURIComponent(src.match(/&filename=([^&]+)/)[1]).replace(/\+/g, ' ');
    }, slider.common.contentSlider.activePreview.image()));
};

/**
 * Вызывает операцию в менюшке, которая выскакивает после нажатия кнопки "More" (три точки) в слайдере
 *
 * @param {('copy'|'move'|'rename'|'delete'|'showFullsize'|'createAlbum'|'addToAlbum'|'versions'|'goToFile')} action - тип опреации
 * @param {boolean} [waitForHiddenMorePopup=true]
 * @returns {Promise<void>}
 */
const yaCallActionInMoreButtonPopup = async function(action, waitForHiddenMorePopup = true) {
    await this.yaWaitForVisible(slider.common.sliderMoreButtonPopup[`${action}Button`]());
    await this.pause(500);
    await this.click(slider.common.sliderMoreButtonPopup[`${action}Button`]());

    if (waitForHiddenMorePopup) {
        await this.yaWaitForHidden(slider.common.sliderMoreButtonPopup());
    }
};

/**
 * Вызывает операцию в тулбаре слайдера
 *
 * @param {('info'|'share'|'download'|'delete'|'more'|'close')} action - тип опреации
 * @returns {Promise<void>}
 */
const yaCallActionInSliderToolbar = async function(action) {
    await this.yaWaitForVisible(slider.common.sliderButtons[`${action}Button`]());
    await this.yaExecuteClick(slider.common.sliderButtons[`${action}Button`]());
};

/**
 * Перелистывает слайдер
 *
 * @param {number} times - сколько раз надо перелистнуть слайдер
 * @param {('left'|'right')} direction - в каком направлении листать слайдер
 * @returns {Promise<void>}
 */
const yaChangeSliderActiveImage = async function(times = 1, direction = 'right') {
    for (let i = 0; i < times; i++) {
        if (await this.yaIsMobile()) {
            await this.yaPointerPanX(slider.common.contentSlider.items(), direction === 'right' ? -1 : 1);
        } else {
            await this.click(direction === 'right' ?
                slider.common.contentSlider.nextImage() :
                slider.common.contentSlider.previousImage());
        }
    }
};

/**
 * Зумит изображение в слайдере
 *
 * @param {Object} browser
 * @param {number} pixels - на сколько пикселей нужно прокрутить колесико мыши
 * @returns {Promise<void>}
 */
const yaSliderZoomImage = async(browser, pixels) => {
    if (await browser.yaIsMobile()) {
        const activeItem = await browser.$(slider.common.contentSlider.activeItem());
        await activeItem.doubleClick();
    } else {
        await browser.execute((selector, pixels) => {
            const event = new WheelEvent('wheel', { bubbles: true, cancelable: true, deltaY: pixels });
            document.querySelector(selector).dispatchEvent(event);
        }, slider.common.contentSlider.activeItem(), pixels);
    }
    await browser.pause(1000);
};

/**
 * Зумит (приближает) изображение в слайдере
 *
 * @returns {Promise<void>}
 */
const yaZoomIn = async function() {
    await yaSliderZoomImage(this, 500);
};

/**
 * Зумит (удаляет) изображение в слайдере
 *
 * @returns {Promise<void>}
 */
const yaZoomOut = async function() {
    await yaSliderZoomImage(this, -500);
};

module.exports = {
    common: {
        yaGetActiveSliderImageName,
        yaChangeSliderActiveImage,
        yaCallActionInMoreButtonPopup,
        yaCallActionInSliderToolbar,
        yaZoomIn,
        yaZoomOut
    }
};
