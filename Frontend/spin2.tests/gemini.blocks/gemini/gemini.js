BEM.DOM.decl('gemini', {
    onSetMod: {
        js: {
            inited: function() {
                var button = this.findBlockInside('button2'),
                    spin = this.findBlockInside('spin2');

                button && button.on('click', function() {
                    spin.toggleMod('progress', 'yes');
                });
            }
        }
    }
});
