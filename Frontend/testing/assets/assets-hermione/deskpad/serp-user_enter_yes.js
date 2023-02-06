BEM.DOM.decl({ block: 'serp-user', modName: 'enter', modVal: 'yes' }, {
    _initPopups: function() {
        this._tld = BEM.blocks['i-global'].param('tld');

        if (this._resolvePopups) {
            this._resolvePopups({
                accounts: [
                    {
                        display_name: 'username',
                        uid: 418857497,
                        avatar: {
                            default: '31078/418857496-1537543522',
                            empty: false
                        }
                    }
                ]
            });
        } else {
            this.__base.apply(this, arguments);
        }
    }
});
