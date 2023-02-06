BEM.DOM.decl('test-popup', {
    onSetMod: {
        js: {
            inited: function() {
                var popup = this.findBlockInside('popup2');

                this.findBlockInside('link').on('click', function(e) {
                    popup.setAnchor(e.block).toggleMod('visible', 'yes', '');
                });
            }
        }
    }
});
