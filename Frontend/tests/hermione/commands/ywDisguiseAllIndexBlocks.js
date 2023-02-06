const selectors = require('../page-objects');

module.exports = async function ywDisguiseAllIndexBlocks() {
    await this.executeAsync((selector, done) => {
        function setHeight(elem, value) {
            elem && elem.style && (elem.style.height = value);
        }

        setHeight(document.querySelector(selector.index.HourlyMain), '297px');
        setHeight(document.querySelector(selector.index.HourlyContainer), '125px');
        setHeight(document.querySelector(selector.index.Forecast), '200px');
        setHeight(document.querySelector(selector.index.MonthLinkSecond), '32px');
        setHeight(document.querySelector(selector.index.MonthCard), '309px');
        setHeight(document.querySelector(selector.index.MapsWidget), '321px');
        setHeight(document.querySelector(selector.index.HistoryCard), '221px');

        document.querySelectorAll(selector.adv.Container).forEach(item => {
            setHeight(item, '200px');
        });

        requestAnimationFrame(done);
    }, selectors);
};
