BEM.DOM.decl('m-calendar', {
    _buildPopup: function() {
        this.__base.apply(this, arguments);

        var cls = this.domElem[0].className.split(/\s+/).filter(function(name) {
            return /^test-/.test(name);
        }).shift();

        this._datePickerPopup.domElem.addClass(cls + '-popup');
    }
});
