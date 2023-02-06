BEM.DOM.decl('gemini-button-disabled', {
    onSetMod: {
        js: {
            inited: function() {
                var switcher = this.findBlockInside({
                        block: 'button',
                        modName: 'arrow',
                        modVal: 'down'
                    });

                this.findBlockInside({
                        block: 'button',
                        modName: 'theme',
                        modVal: 'action'
                    })
                    .bindTo('pointerclick', function() {
                        switcher.toggleMod('disabled', 'yes');
                    });
            }
        }
    }
});
