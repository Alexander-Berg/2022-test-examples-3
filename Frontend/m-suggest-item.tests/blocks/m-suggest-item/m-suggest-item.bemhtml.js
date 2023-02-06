// Если не поставить заглушку, в FF иконка съезжает вниз.
block('m-suggest-item').def()(function() {
    return applyNext({_inSuggestItem: true});
});

block('image').match(function() { return this._inSuggestItem; }).def()(function() {
    this.ctx.url || (this.ctx.url = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg"></svg>');
    return applyNext();
});
