/**
 * Переключает темную тему
 * @param {boolean} isDarkTheme
 * @returns {Promise<void>}
 */
module.exports = async function yaToggleDarkTheme(isDarkTheme) {
    await this.execute(function(isDarkTheme) {
        document.documentElement.classList.toggle('theme_dark', isDarkTheme);
    }, isDarkTheme);
};
