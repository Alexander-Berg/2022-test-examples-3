BEM.DOM.decl('ajax-updater', {
    /**
     * @param {Object} ajaxData - ответ сервера
     */
    _triggerSucceed: function(ajaxData) {
        this.__base.apply(this, arguments);

        window.hermione.meta.lastAjaxUpdatedReqId = ajaxData.reqid;
    },

    _getRequestArguments: function() {
        var params = this.__base.apply(this, arguments);

        // Включается через hermione/client-scripts/ajax-updater.js
        if (this.__self.HERMIONE_GLOBAL_REQUEST_COUNTER >= 0) {
            params['hermione-request-counter'] = String(this.__self.HERMIONE_REQUEST_COUNTER);

            this.__self.HERMIONE_GLOBAL_REQUEST_COUNTER++;
        }

        delete params.spas_ajax;
        delete params.spas_ajax_reload;

        return params;
    }
});
