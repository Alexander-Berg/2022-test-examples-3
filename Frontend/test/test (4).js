BEM.DOM.decl('test', {
    onSetMod: {
        js: {
            inited: function() {
                this._popup = this.findBlockInside('popup2');
                this._destructor = this.findBlockOn('destructor', 'button2');
                this._button = this.findBlockInside('button2');

                if(this._destructor) {
                    this._destructor.on('click', function() {
                        BEM.DOM.destruct(this.domElem);
                    }, this);
                }

                if(this.params.position) {
                    this._popup.setPosition.apply(this._popup, this.params.position);
                } else {
                    this._popup.setAnchor(this._button);
                }
            }
        }
    },

    _onButtonClick: function() {
        this._popup.toggleMod('visible', 'yes');
    }
}, {
    live: function() {
        this.liveInitOnBlockInsideEvent('click', 'button2', this.prototype._onButtonClick);
        return false;
    }
});


BEM.DOM.decl({block: 'test', modName: 'hides', modVal: 'onscroll'}, {
    onSetMod: {
        js: {
            inited: function() {
                this.__base.apply(this, arguments);
                this.bindToWin('scroll', this._onPageScroll);
            }
        }
    },

    _onPageScroll: function() {
        this._popup.delMod('visible');
    }
});
