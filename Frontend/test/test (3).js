BEM.DOM.decl('test', {
    onSetMod: {
        js: {
            inited: function() {
                var content = this.elem('content'),
                    text = this.elem('text'),
                    paragraph = text.html();

                this._modal = this.findBlockInside('modal');

                this.findBlockInside('link').on('click', function() {
                    this._modal.toggleMod('visible', 'yes');
                }, this);

                this._bindToLinkClick('closer', function() {
                        this._modal.delMod('visible');
                    })
                    ._bindToLinkClick('add-text', function() {
                        text.append(paragraph);
                    })
                    ._bindToLinkClick('remove-text', function() {
                        text.find('p:last').remove();
                    })
                    ._bindToLinkClick('wider', function() {
                        content.css('width', parseInt(content.css('width'), 10) + 100);
                    })
                    ._bindToLinkClick('narrower', function() {
                        content.css('width', parseInt(content.css('width'), 10) - 100);
                    })
                    ._bindToLinkClick('toggle-pos', function(e) {
                        var cell = this._modal.elem('cell'),
                            verticalAlign = cell.css('vertical-align');
                        verticalAlign = verticalAlign === 'middle' ? 'top' : 'middle';

                        this._modal.elem('cell').css('vertical-align', verticalAlign);
                        e.block.elem('inner').text(verticalAlign);
                    });
            }
        }
    },

    _bindToLinkClick: function(elem, action) {
        var link = this.findBlockOn(elem, 'link');
        link && link.on('click', action, this);

        return this;
    }
});
