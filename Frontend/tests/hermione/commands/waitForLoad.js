/**
 * Функция ждет, что загрузятся иконки и статусы и пропадут спинеры загрузки.
 * @returns {Object}
 */
 module.exports = function waitForLoad() {
    return this
        .waitForLoaders()
        .waitForStatuses()
        //Получаем массив всех иконок на странице
        .elements('.awesome-icon')
        .then(array => {
            array.value.forEach(elem => {
                //Ждем что каждая иконка загрузится
                this.waitForExist(elem.ELEMENT)
                    .waitForPseudoContent(elem.ELEMENT, 'before')
            })
        })
};
