BEM.DOM.decl('gemini-suggest-in-popup', {
    onSetMod: {
        js: {
            inited: function() {
                var owner = this.findBlockInside('button'),
                    popup = this
                        .findBlockInside('popup');
                owner.on('click', function() {
                    popup.toggle(owner);
                });
            }
        }
    }
});
