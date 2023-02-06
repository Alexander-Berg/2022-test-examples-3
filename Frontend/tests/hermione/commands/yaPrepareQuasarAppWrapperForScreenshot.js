/**
 * Команда, которая подготавливает части интерфейса для скриншотов.
 * Убирает внутренний скролл, передавая ответственность body, для скришота во всю высоту.
 * При наличии футера переносит его из корня модалок в блок приложения.
 */
module.exports = async function yaPrepareQuasarAppWrapperForScreenshot() {
    return await this.execute(function() {
        const body = document.querySelector('body');
        const appWrapper = document.querySelector('.app__wrapper');
        const footer = document.querySelector('.footer');

        body.classList.remove('full-height');

        if (footer) {
            footer.style.position = 'absolute';
            appWrapper.style.position = 'relative';

            appWrapper.appendChild(footer);
        }
    });
};
