module.exports = {
    checkCounter: function (linkData) {
        const clickCoords = linkData.clickCoords;

        let action = linkData.selector;

        if (clickCoords) {
            action = () => this.leftClick(linkData.selector, clickCoords[0], clickCoords[1]);
        }

        return this.yaCheckCounter(action, linkData.counter, linkData.counterOptions)
            .then((counterData) => {
                if (counterData && counterData[0] && counterData[0].url) {
                    return counterData[0].url;
                }

                throw new Error('Неожиданный результат из yaCheckCounter. Отсуствует \'url\'');
            });
    },
};
