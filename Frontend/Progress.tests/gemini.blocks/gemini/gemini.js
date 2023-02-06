BEM.DOM.decl('gemini', {
    onSetMod: {
        js: {
            inited: function() {
                this._count = 0;
                this._progress = this.findBlockInside('progress');
                this.bindToDoc('click', this._updateProgress);
            }
        }
    },

    _updateProgress: function() {
        this._count = (this._count + 1) % 3;
        this._progress.update(this._count / 2, 'linear');
    }
});
