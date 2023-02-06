/**
 * Проверка на наличие загруженного контента в тизере.
 * Eсли контент загружен, то атрибут "src" изображения не должен быть пустым.
 */
module.exports = async function(PO, timeout = 10000) {
    await this.waitUntil(
        async () => {
            const thumbsSrc = await this.getAttribute(PO.Teaser.VideoItemThumb(), 'src');

            return thumbsSrc.every(Boolean);
        },
        timeout,
        `Контент тизера не появился спустя ${timeout / 1000}c`,
    );
};
