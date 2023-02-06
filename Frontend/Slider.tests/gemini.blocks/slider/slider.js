(function(BEM) {
BEM.DOM.decl('slider', {

    /**
     * Валидация `val` в области допустимых значений
     *
     * @param {Number} idx
     * @param {Number} val
     * @returns {Number}
     */
    toAllowedRange: function(idx, val) {
        val = this.__base.apply(this, arguments);

        var min = this.min(),
            max = this.max();

        // Выходит ли значение из области возможных значение (min/max)
        return val < min ? min : (val > max ? max : val);
    }

});
})(BEM);
