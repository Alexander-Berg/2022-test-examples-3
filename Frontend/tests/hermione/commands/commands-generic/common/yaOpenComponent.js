const STORY_BOOK_URL = '/static/storybook';

/**
 * Открытие компонента в Storybook
 *
 * @param {String} id - идентификатор компонента
 *
 * @returns {Promise}
 */
module.exports = function(id) {
    const storyUrl = `${STORY_BOOK_URL}/iframe.html?id=${id}`;

    return this.yaOpenPage(storyUrl);
};
