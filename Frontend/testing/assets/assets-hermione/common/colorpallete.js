/* globals BEM */
BEM.DOM.decl('colorpalette', {
    _sendAjax: function(data, success, error) {
        var uri = BEM.blocks.uri.parse(window.location.href);

        data.testRunId = (uri.getParam('testRunId') || [])[0];

        this.__base.apply(this, arguments);
    }
});
