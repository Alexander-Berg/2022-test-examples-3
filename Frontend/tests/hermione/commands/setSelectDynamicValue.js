/**
 * Выбор значения в селекте
 * @param {Object} options
 * @param {Object} options.block - селектор контрола
 * @param {Object} options.menu - селектор выпадающего меню
 * @param {Object} options.text - текст в опции, которую надо выбрать
 * @returns {Object}
 */
module.exports = function setSelectDynamicValue(options) {
    let {
        block,
        menu = '.popup2.popup2_visible_yes',
        text,
    } = options;

    /*
        У данных из саджеста может меняться порядок, в котором они приходят от сервера
        + у полей нет уникальных классов или id чтобы их можно было как-то прицельно выцепить (поля id не статичны)
        по этому пришлось использовать xpath
        menu__item используется в офферах только в списках офисов и организаций 
        (только при поиске опций в этих саджестах используется эта функция)
    */
    let element = `//*[contains(@class, 'menu__item')]/*[text() = '${text}']`

    return this
        .waitForVisible(block)
        .click(block)
        .waitForVisible(menu)
        .waitForVisible(element)
        .click(element)
        .waitForHidden(menu);
};
