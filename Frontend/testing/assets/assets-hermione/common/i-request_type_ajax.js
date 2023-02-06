BEM.decl({ block: 'i-request_type_ajax' }, {
    getDefaultParams: function() {
        var params = this.__base();
        // Для hermione оставляем 0 ретраев, что бы не увеличивать время выполнения тестов.
        params.retryCount = 0;
        return params;
    }
});
