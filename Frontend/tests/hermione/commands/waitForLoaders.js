/**
 * Функция ждет, что все спиннеры загрузки пропадут
 * @param {*} selector - селектор спиннера загрузки
 * @returns {Object}
 */
 module.exports = function waitForLoaders(selector = '.spin2') {
    return this
        .elements(selector)
        .then(array => {
            array.value.forEach(elem => {
                this.waitForExist(elem.ELEMENT)
                    .waitForHidden(elem.ELEMENT)
            })
        })
};
