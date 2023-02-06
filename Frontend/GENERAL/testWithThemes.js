const themeColors = ['dark'];

module.exports = function(description, suite) {
    it(description, suite);

    themeColors.forEach(function(themeColor) {
        it(`${description} | _theme-color_${themeColor}`, async function() {
            await this.browser.execute(themeColor => {
                const instance = MBEM.getBlock(document.querySelector('.mini-suggest'), 'mini-suggest');
                instance.setThemeColor(themeColor);
            }, themeColor);

            await suite.apply(this, arguments);
        });
    });
};
