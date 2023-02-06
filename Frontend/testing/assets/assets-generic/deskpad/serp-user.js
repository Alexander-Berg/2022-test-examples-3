// Переопределяем, чтобы _processPopupData не портил base64-картинку
BEM.DOM.decl({ block: 'serp-user', modName: 'enter', modVal: 'yes' }, {
    _processPopupData: function() {
        this.__base.apply(this, arguments);

        var style = this.elem('icon').attr('style');

        this.elem('icon').attr('style', style.replace(/=\d+\/islands-retina-small/, ''));
    }
});
