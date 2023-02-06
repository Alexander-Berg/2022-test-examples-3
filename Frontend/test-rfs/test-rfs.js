(function() {
    var doc = document,
        fullscreenElement = 'fullscreenElement' in doc
            ? 'fullscreenElement' : 'webkitFullscreenElement' in doc
                ? 'webkitFullscreenElement' : 'mozFullScreenElement' in doc
                    ? 'mozFullScreenElement' : 'msFullscreenElement' in doc
                        ? 'msFullscreenElement' : '',
        exitFullscreen = doc.exitFullscreen
            ? 'exitFullscreen' : doc.webkitExitFullscreen
                ? 'webkitExitFullscreen' : doc.mozCancelFullScreen
                    ? 'mozCancelFullScreen' : doc.msExitFullscreen
                        ? 'msExitFullscreen' : '',
        fullscreenchange = 'onfullscreenchange' in doc
            ? 'fullscreenchange' : 'onwebkitfullscreenchange' in doc
                ? 'webkitfullscreenchange' : 'onmozfullscreenchange' in doc
                    ? 'mozfullscreenchange' : 'onmsfullscreenchange' in doc
                        ? 'MSFullscreenChange' : '';

BEM.DOM.decl('test-rfs', {
    onSetMod: {
        js: {
            inited: function() {
                this._buttona = this.findBlockOn('buttona', 'button2');
                this._buttonb = this.findBlockOn('buttonb', 'button2');
                this._buttonc = this.findBlockOn('buttonc', 'button2');
                this._buttond = this.findBlockOn('buttond', 'button2');
                this._popupa = this.findBlockOn('popupa', 'popup2');
                this._popupb = this.findBlockOn('popupb', 'popup');
                this._popupc = this.findBlockOn('popupc', 'popup2');
                this._popupd = this.findBlockOn('popupd', 'popup');
                this._scope = this.findBlockOn('x-area').domElem;
                this._fullscreenButton = this.findBlockOn('fullscreen-button', 'button2');

                this.bindToDoc(fullscreenchange, this._onFullscreenChange);
            }
        }
    },

    _onFullscreenButtonClick: function() {
        var scope = this.domElem,
            scope0 = scope[0],
            requestFullscreen;

        if(fullscreenElement && document[fullscreenElement]) {
            document[exitFullscreen]();
            return;
        }

        requestFullscreen = scope0.requestFullscreen
            ? 'requestFullscreen' : scope0.webkitRequestFullscreen
                ? 'webkitRequestFullscreen' : scope0.mozRequestFullScreen
                    ? 'mozRequestFullScreen' : scope0.msRequestFullscreen
                        ? 'msRequestFullscreen' : '';

        if(requestFullscreen) {
            scope0[requestFullscreen]();
        }
    },

    _onFullscreenChange: function() {
        var scope = document[fullscreenElement] ? this._scope : BEM.DOM.scope;
        this._popupa.setScope(scope).delMod('visible');
        this._popupb.setScope(scope).hide();
        this._popupc.setScope(scope).delMod('visible');
        this._popupd.setScope(scope).hide();
    },

    _onButtonAClick: function() {
        this._popupa.setAnchor(this._buttona).toggleMod('visible', 'yes');
    },

    _onButtonBClick: function() {
        this._popupb.toggle(this._buttonb);
    },

    _onButtonCClick: function() {
        this._popupc.setAnchor(this._buttonc).toggleMod('visible', 'yes');
    },

    _onButtonDClick: function() {
        this._popupd.toggle(this._buttond);
    }
}, {
    live: function() {
        this
            .liveBindTo('fullscreen-button', 'pointerclick', function() {
                return this._onFullscreenButtonClick();
            })
            .liveBindTo('buttona', 'pointerclick', function() {
                return this._onButtonAClick();
            })
            .liveBindTo('buttonb', 'pointerclick', function() {
                return this._onButtonBClick();
            })
            .liveBindTo('buttonc', 'pointerclick', function() {
                return this._onButtonCClick();
            })
            .liveBindTo('buttond', 'pointerclick', function() {
                return this._onButtonDClick();
            });
    }
});
})();
