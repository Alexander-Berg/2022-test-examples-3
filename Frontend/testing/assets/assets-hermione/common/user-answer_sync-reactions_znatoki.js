BEM.DOM.decl({
    block: 'user-answer',
    modName: 'sync-reactions',
    modVal: 'znatoki'
}, {
    _loadReactions: function() {
        this._onReactionsLoaded({
            negativeVotes: -1,
            liked: null,
            positiveVotes: 71
        });
    },

    _onReactionsLoaded: function(data, textStatus, jqXHR) {
        this.trigger('reactions-loaded', data);
    }
});
