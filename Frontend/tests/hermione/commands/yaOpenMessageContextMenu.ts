/**
 * Открывает контекстное меню у сообщения
 *
 * @param {String} selector - Селектор для элемента, появление которого нужно ждать
 * @param {String} platform - Платформа
 *
 * @returns {Promise}
 */

module.exports = async function yaOpenMessageContextMenu(selector: string, platform: string) {
    // На тачах делаем clickTo, иначе на сообщениях со стикерами мы попадаем в стикер
    return platform === 'desktop' ? this.rightClick(selector) : this.clickTo(selector, 10, 10);
};
