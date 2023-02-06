// Тестовый блок для генерации сообщений с помощью mi-notifier
BEM.DOM.decl({block: 'instant-notifier'}, {
    onSetMod: {
        js: {
            inited: function() {
                var params = this.params,
                    notifier = BEM.create('mi-notifier', params.options);

                params.messages && params.messages.forEach(function(message) {
                    notifier.info(message);
                });
            }
        }
    }
});
