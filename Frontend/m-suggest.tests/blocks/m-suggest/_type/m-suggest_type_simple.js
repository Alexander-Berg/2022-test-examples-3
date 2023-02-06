BEM.DOM.decl({block: 'm-suggest', modName: 'type', modVal: 'simple'}, {
    _sendRequest: function() {
        this._processResult([
                {
                    type: 'GET',
                    url: 'url/?%text%',
                    dataType: 'jsonp',
                    timeout: 20000,
                    text: 'Lego'
                }
        ]);
    }
});

