BEM.DOM.decl('test', {
    onSetMod: {
        js: {
            inited: function() {
                var modal = this.findBlockInside('modal');
                this.bindTo('clicker', 'click', function() {
                    modal.toggleMod('visible', 'yes', '');
                });
            }
        }
    }
});
