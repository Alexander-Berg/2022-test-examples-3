SANDBOX.addInitializer(function(obj){
    var domNode;
    
    if (typeof obj.beforeInit === 'function') { obj.beforeInit(); }

    if (obj.bemjson) {
        domNode = $(BEMHTML.apply(obj.bemjson)).appendTo('#sandbox-container');

        BEM.DOM.init(domNode);
    }
});
