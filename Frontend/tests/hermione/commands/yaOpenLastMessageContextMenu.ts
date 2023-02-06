/**
 * Открывает контекстное меню у последнего сообщения
 *
 * @param {String} platform - Платформа
 *
 * @returns {Promise}
 */

module.exports = async function yaOpenLastMessageContextMenu(platform: string) {
    // На тачах используем другой селектор, иначе на сообщениях со стикерами мы попадаем в стикер
    return platform === 'desktop' ? this.rightClick(PO.lastMessage.balloonContent()) : this.click(PO.lastMessage.balloonInfo());
};
