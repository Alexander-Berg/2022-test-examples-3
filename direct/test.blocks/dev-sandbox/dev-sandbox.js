SANDBOX.addInitializer(function(obj, container){
    var domNode;

    if (obj.bemjson) {
        domNode = $(BEMHTML.apply(obj.bemjson)).appendTo(container);

        BEM.DOM.init(domNode);
    }
});
