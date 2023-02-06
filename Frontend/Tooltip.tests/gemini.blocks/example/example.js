BEM.DOM.decl('example', {
    onSetMod: {
        js: {
            inited: function() {
                var button = this.findBlockInside('button2'),
                    tooltip = this.findBlockInside('tooltip');

                tooltip.setOwner(button).setMod('shown', 'yes');

                button.on('click', function() {
                    tooltip.toggleMod('shown', 'yes', '');
                });
            }
        }
    }
});
