BEM.DOM.decl('show-feedback', {
    getPopupParams: function() {
        var params = this.__base.apply(this, arguments);

        params.abuseLink = BEM.blocks.uri
            .parse(params.abuseLink)
            .addParam('exp_flags', 'test_tool=generic')
            .build();

        return params;
    }
});
