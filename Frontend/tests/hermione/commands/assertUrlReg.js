/**
 * Сравнивает фактический url с заданным регуляным выражением
 * @param {RegExp} urlReg - регулярка урла
 * @returns {Object}
 */
module.exports = function assertUrlReg(urlReg, timeout = 5000) {
    return this
        .waitUntil(() => {
            return this
                .getUrl()
                .then(url => Boolean(url.match(urlReg)))
        }, timeout, `URL не соответствует паттерну '${urlReg.toString()}'`, 500)
};
