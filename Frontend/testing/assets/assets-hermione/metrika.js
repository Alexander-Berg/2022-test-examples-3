modules.decl('metrika', ['i-bem-dom'], function(provide, bemDom) {
    var Metrika = bemDom.declBlock(this.name, {
        _getScriptUrl: function() {
            return '/static/turbo/hermione/stubs/metrika.js';
        },
    });

    provide(Metrika);
});
