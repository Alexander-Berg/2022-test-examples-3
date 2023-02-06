BEM.DOM.decl('pushbundle-test-view', {
    onAjaxSuccess: function(data) {
        this.setMod('ajax', 'yes');

        if (data.html) {
            BEM.DOM.update(this.domElem, $(data.html).html());
        }
    },

    _changeQuery: function() {
        var location = BEM.blocks.location.getInstance(),
            state = location.getState(),
            text = 'amazing horse';

        delete state.url;
        delete state.reqid;
        state.trigger = true;
        state.history = true;

        // дропаем кеш для ajax перезапросов в ie11
        // для ./hermione/test-suites/common/pushbundle/pushbundle.hermione.js
        switch (state.params.text[0].split(' ')[0]) {
            case 'Nirvana':
                text = 'удивительная лошадь';
                break;
            case 'Rammstein':
                text = 'amazing horse';
                break;
        }

        state.params.text = [text];

        location.change(state);
    }
}, {
    live: function() {
        this.liveBindTo('image', 'click', function() {
            this._changeQuery();
        });
    }
});
