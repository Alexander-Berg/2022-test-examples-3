/**
 * Функция ждет, что загрузятся статусы (теги и кружочки).
 * @returns {Object}
 */
 module.exports = function waitForStatuses(status = '.Status .Tag', dotStatus = '.Badge-Dot') {
    return this
        //Получаем массив всех статусов в виде больших тегов
        .elements(status)
        .then(array => {
            array.value.forEach(elem => {
                this.waitForExist(elem.ELEMENT)
                    .waitForVisible(elem.ELEMENT)
            })
        })
        //Получаем массив всех статусов в виде кружочков
        .elements(dotStatus)
        .then(array => {
            array.value.forEach(elem => {
                this.waitForExist(elem.ELEMENT)
                    .waitForVisible(elem.ELEMENT)
            })
        })
};
