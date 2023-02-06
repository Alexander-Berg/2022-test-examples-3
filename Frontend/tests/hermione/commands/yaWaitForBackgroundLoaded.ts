/**
 * Проверяет загрузилось ли фоновое изображение
 *
 * @param selector - Селектор элемента с фоновым изображением
 */
export async function yaWaitForBackgroundLoaded(
    this: WebdriverIO.Browser,
    selector: string,
    options: { message?: string; timeout?: number; } = {},
) {
    const {
        message = 'Не загрузились фоновые изображения',
        timeout = this.options.waitforTimeout,
    } = options;

    await this.yaShouldExist(selector, message);

    return this.waitUntil(async function(
        this: WebdriverIO.Browser,
    ) {
        const result = await this.execute(function(selector) {
            const testImg = document.createElement('img');
            const elems = document.querySelectorAll(selector);

            return Array.prototype.slice.call(elems)
                .every(function(node: HTMLElement) {
                    const imageUrl = getBackgroundImageUrl(node);

                    return imageUrl ? isImageLoaded(imageUrl) : true;
                });

            function getBackgroundImageUrl(node: HTMLElement) {
                if (!node || !node.style) return;

                // Вырезаем путь из URL фоновой картинки
                const backgroundImgParts = /url\(['"]?([^'")]+)['"]?\)/.exec(node.style.backgroundImage);

                return backgroundImgParts && backgroundImgParts[1];
            }

            function isImageLoaded(imageUrl: string) {
                // Если фоновая картинка уже загрузилась, то она подставится из кеша в img без задержек
                testImg.src = imageUrl;

                // Поэтому просто проверяем, что картинка загружена
                return testImg.complete;
            }
        }, selector);

        return result && result === true;
    }, {
        timeout,
        timeoutMsg: message,
    });
}
