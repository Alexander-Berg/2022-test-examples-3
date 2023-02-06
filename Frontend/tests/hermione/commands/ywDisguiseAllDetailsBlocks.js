const selectors = require('../page-objects');

module.exports = async function ywDisguiseAllDetailsBlocks() {
    await this.executeAsync((selector, done) => {
        function setHeight(elem, value) {
            elem && elem.style && (elem.style.height = value);
        }

        setHeight(document.querySelector(selector.details.Title), '57px');
        setHeight(document.querySelector(selector.details.Tabs), '57px');
        setHeight(document.querySelector(selector.details.DetailsTemp), '163px');
        setHeight(document.querySelector(selector.details.DetailsWind), '125px');
        setHeight(document.querySelector(selector.details.DetailsHumidity), '90px');
        setHeight(document.querySelector(selector.details.DetailsPressure), '114px');
        setHeight(document.querySelector(selector.details.DetailsSun), '120px');
        setHeight(document.querySelector(selector.details.DetailsOther), '84px');

        document.querySelectorAll(selector.adv.Container).forEach(item => {
            setHeight(item, '200px');
        });

        requestAnimationFrame(done);
    }, selectors);
};
