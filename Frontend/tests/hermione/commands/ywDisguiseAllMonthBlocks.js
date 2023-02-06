const selectors = require('../page-objects');

module.exports = async function ywDisguiseAllMonthBlocks() {
    await this.executeAsync((selector, done) => {
        function setHeight(elem, value) {
            elem && elem.style && (elem.style.height = value);
        }

        document.querySelectorAll(selector.month.Calendar).forEach(item => {
            setHeight(item, '347px');
        });
        document.querySelectorAll(selector.adv.Container).forEach(item => {
            setHeight(item, '200px');
        });

        requestAnimationFrame(done);
    }, selectors);
};
