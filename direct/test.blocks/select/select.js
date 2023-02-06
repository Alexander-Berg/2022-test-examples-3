BEM.DOM.decl('select', {

    // todo@dima117a выпилить это переопределение после рефакторинга DIRECT-65106

    /**
     * Возвращает текст выбранного элемента селекта
     * @returns {String}
     */
    getSelectedOptionText: function() {

        return this.elem('control').find('option').eq(this._getSelectedIndex()).text();

    }
});
