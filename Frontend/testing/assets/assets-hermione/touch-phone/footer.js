'use strict';

/* globals BEM:false */
BEM.DOM.decl('footer', {
    _getFeedbackParams: function() {
        var params = this.__base.apply(this, arguments);

        params.abuseLink = BEM.blocks.uri.parse(params.abuseLink)
            .addParam('exp_flags', 'test_tool=hermione').build();

        return params;
    }
});
