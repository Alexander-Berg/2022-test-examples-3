BEM.DOM.decl('button', {

    // todo@dima117a выпилить это переопределение после рефакторинга DIRECT-65106

    onSetMod: {
        disabled: function(modName, modValue) {
            // принудительное снятие ховера и потеря фокуса при деактивации
            modValue &&
                this.delMod('hovered') &&
                this.domElem.blur();

            this.__base.apply(this, arguments);
        }
    },

    /**
     * возвращает текст кнопки
     * @returns {String} текст кнопки
     */
    getText: function() {
        return this.elem('text').text();
    },

    /**
     * устанавливает текст кнопки
     * @param {String} text
     */
    setText: function(text) {
        this.elem('text').text(text);
    }

});
