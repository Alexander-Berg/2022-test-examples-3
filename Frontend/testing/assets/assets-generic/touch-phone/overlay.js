/* globals BEM:false */
'use strict';

BEM.DOM.decl('overlay', {
    _initOverlay: function() {
        this.__base.apply(this, arguments);

        this.setMod('anim-off');
    },

    // Хак _ios-scroll-bug_fix не нужен для тестов
    _onWinScroll: $.noop
});
