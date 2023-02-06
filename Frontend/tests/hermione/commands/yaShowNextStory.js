const STORY_WRAPPER_SELECTOR = '.story-modal_visible .stories-modal__item-wrapper_active .story__story-item-wrapper_active .story-item-wrapper_visible';
const STORY_CONTENT_SELECTOR = '.story-modal_visible .stories-modal__item-wrapper_active .story__story-item-wrapper_active .story-item-wrapper_visible .story-item-content_active .story-item-content__main-content';

/**
 * Переходит к просмотру следующей стори (клик по центру экрана)
 */
module.exports = async function yaShowNextStory() {
    const storyWrapper = await this.$(STORY_WRAPPER_SELECTOR);

    const storySize = await storyWrapper.getSize();

    await storyWrapper.moveTo({ xOffset: Math.floor(storySize.width / 2), yOffset: Math.floor(storySize.height / 2) });
    await storyWrapper.click();

    // Ожидание анимации переключения стори
    await this.waitUntil(async() => {
        const storyContent = await this.$(STORY_CONTENT_SELECTOR);
        const opacity = await storyContent.getCSSProperty('opacity');
        const text = await storyContent.getText();

        return text && opacity.value === 1;
    }, {
        timeout: 2_000,
        timeoutMsg: 'Анимация переключения стори не завершилась',
        interval: 500,
    });
};
