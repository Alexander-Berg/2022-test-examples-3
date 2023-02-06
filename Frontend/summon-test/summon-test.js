BEM.DOM.decl('summon-test', {
    onSetMod: {
        js: {
            inited: function() {
                var popup = this.findBlockInside('popup', 'popup2'),
                    currentAnchor = null;

                this.findBlocksInside('summoner', 'button2').forEach(function(button) {
                    button.on('click', function(e) {
                        var button = e.block;
                        if(currentAnchor === button) {
                            popup.toggleMod('visible', 'yes');
                        } else {
                            popup.setAnchor(button).setMod('visible', 'yes');
                            currentAnchor = button;
                        }
                    });
                });
            }
        }
    }
});
