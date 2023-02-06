BEM.DOM.decl('test', {
   onSetMod: {
       js: {
           inited: function() {
               this._anchor = this.domElem.children(this.buildSelector('anchor'));
               this._popup = this.domElem.children(this.buildSelector('popup'));
           }
       }
   }
}, {
    live: function() {
        this.liveInitOnEvent('anchor', 'pointerclick', function() {
            this.findBlocksOn(this._popup, 'popup2').forEach(function(popup) {
                popup.setAnchor(this._anchor).setMod('visible', 'yes');
            }, this);
        });
    }
});
