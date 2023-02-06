BEM.DOM.decl({ block: 'metrika-host' }, {
    _hasGdpr: function() {
        /** browser.setCookie не успевает сработать до вызова отпарвки метрики show */
        return true;
    }
});
