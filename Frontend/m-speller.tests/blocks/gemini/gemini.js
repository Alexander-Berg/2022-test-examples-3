BEM.DOM.decl('gemini', {
    onSetMod: {
        js: function() {
            this.findBlockInside({block: 'button2', modName: 'role', modVal: 'check'})
                .on('click', this.startCheck, this);
            this.textarea = this.findBlockInside({block: 'textarea', modName: 'role', modVal: 'text'});
            this.speller = this.findBlockInside('m-speller').on('change', this.change, this);
        }
    },

    startCheck: function() {
        this.speller.check(this.textarea.getText());
    },

    change: function(e, data) {
        this.textarea.setText(data);
    }
});
