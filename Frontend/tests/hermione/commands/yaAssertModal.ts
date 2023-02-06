interface IAssertModal {
    ignoreLastSeen?: boolean,
}

/**
 * Делает скрин модального окна без фона
 *
 * @param {String} name - Название файла со скриншотом
 * @param {String} selector - Селектор для элемента
 *
 * @returns {Promise}
 */

module.exports = async function yaAssertModal(name: string, selector: string, options: IAssertModal) {
    const invisibleElements = ['.yamb-root'];

    if (options && options.ignoreLastSeen) {
        invisibleElements.push('.yamb-last-seen');
    }

    await this.assertView(name, selector, {
        invisibleElements,
        allowViewportOverflow: true,
        ...options,
    });
};
