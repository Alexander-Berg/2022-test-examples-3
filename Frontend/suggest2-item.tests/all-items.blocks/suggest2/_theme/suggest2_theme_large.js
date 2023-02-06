/**
 * # Модификатор `_theme_large`
 * @class suggest2
 *
 * Добавляет особенности отображается саджеста на всю ширину страницы.
 */
BEM.DOM.decl({block: 'suggest2', modName: 'theme', modVal: 'large'}, {

    /**
     * Возвращает ширину для контейнера подсказок.
     *
     * @protected
     * @returns {Number} Ширина контейнера
     */
    getContainerWidth: function() {
        return 720;
    }
});
