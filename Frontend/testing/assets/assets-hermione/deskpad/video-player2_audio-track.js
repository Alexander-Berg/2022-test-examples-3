BEM.DOM.decl({ block: 'video-player2', modName: 'audio-track' }, {
    onSetMod: {
        js: {
            inited: function() {
                this._audioTrackElem = $('#' + this.params.audioTrackId);
                this._audioTrackSelect = this._audioTrackElem.bem('select2');

                this._audioTrackSelect.setOptions([{ text: 'Русский', val: '0' }]);
                this.delMod(this._audioTrackElem, 'hidden');

                return;
            }
        }
    }
});
