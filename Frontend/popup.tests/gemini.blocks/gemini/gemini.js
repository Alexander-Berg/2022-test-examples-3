BEM.DOM.decl('gemini', {
    onSetMod: {
        js: {
            inited: function() {
                var owner = this.findBlockInside('button'),
                    popup = this
                        .findBlockInside('popup')
                        .show(owner);

                owner.bindTo('pointerclick', function() {
                    popup.toggle(owner);
                });
            }
        }
    }
});
