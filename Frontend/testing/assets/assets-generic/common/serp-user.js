// Переопределяем, чтобы _getIconPath не портил base64-картинку
BEM.DOM.decl('user', {
    _getIconPath: function(avatarId) {
        return this.params.avatarHost;
    }
});
