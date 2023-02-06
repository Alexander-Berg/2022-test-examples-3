/**
 * Пробегает скролом по странице, чтобы проинициализировать все lazy-load картинки
 * @returns {Promise<void>}
 */
module.exports = async function ywInitLazyLoading() {
    const browser = this;

    return browser.executeAsync(async done => {
        function timeout(ms) {
            return new Promise(resolve => setTimeout(resolve, ms));
        }

        const screenHeight = window.outerHeight;
        const imageOffsets = [];
        const lazyImages = document.querySelectorAll('[class^="Image"]');
        const IMAGE_LOAD_DELAY = 500;

        lazyImages.forEach(image => {
            const y = image.getBoundingClientRect().top;
            const screenOffset = Math.floor(y / screenHeight) * screenHeight;

            /**
             * Как минимум, необходимо исключить случаи, когда для кучи иконок, находящихся на одной высоте (в карусели)
             * Выполняется скролл и ожидание
             *
             * Как максимум - не скролить экран по пикселю
             */
            if (!(imageOffsets.includes(screenOffset))) {
                imageOffsets.push(screenOffset);
            }
        });

        async function scrollAndWait(i) {
            window.scroll(0, imageOffsets[i]);
            await timeout(IMAGE_LOAD_DELAY);

            if (i) {
                await scrollAndWait(--i);
            }
        }

        if (imageOffsets.length > 0) {
            await scrollAndWait(imageOffsets.length - 1);
        }

        done();
    });
};
