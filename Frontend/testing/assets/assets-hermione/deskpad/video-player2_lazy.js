BEM.DOM.decl({ block: 'video-player2', modName: 'lazy' }, {
    onSetMod: {
        js: function() {
            var video2 = this.findBlockInside('video2');
            this._triggerElement = video2 && video2.elem('thumb');
        }
    }
});
