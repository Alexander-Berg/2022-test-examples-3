
block('textinput')(

    js()({live: false}),

    content()(function() {
        return [].concat(
            {elem: 'found', content: this.ctx.found},
            this.ctx.filter ? this.util.mergeBemjson({
                    mix: {block: 'textinput', elem: 'filter'}
                }, this.ctx.filter) : [],
            applyNext()
        );
    })
);
