block('b-block-page')(
    tag()('html'),
    content()(function() {
        return [{
            elem: 'head',
            content: [{
                elem: 'meta',
                attrs: {
                    charset: 'utf-8'
                }
            }, {
                elem: 'title',
                content: this.ctx.blockName
            }, {
                elem: 'css',
                url: '_' + this.ctx.blockName + '.css'
            }]
        }, {
            elem: 'body',
            content: [{
                elem: 'readme',
                content: this.ctx.mdContent
            }, {
                elem: 'header2',
                content: 'Test results'
            }, {
                elem: 'iframe',
                url: this.ctx.blockName + '.test.html'
            }, {
                elem: 'header2',
                content: 'Code coverage'
            }, {
                elem: 'iframe',
                url: this.ctx.blockName + '.coverage.html'
            }]
        }];
    }))



block('b-block-page').elem('head').tag()('head')


block('b-block-page').elem('body').tag()('body')


block('b-block-page').elem('title').tag()('title')


block('b-block-page').elem('meta').tag()('meta')


block('b-block-page').elem('header2').tag()('h2')


block('b-block-page').elem('css')(
    tag()('link'),
    attrs()(function() {
        return {
            rel: 'stylesheet',
            href: this.ctx.url
        };
    }))



block('b-block-page').elem('js')(
    tag()('script'),
    attrs()(function() {
        return {
            src: this.ctx.url
        };
    }))



block('b-block-page').elem('iframe')(
    tag()('iframe'),
    attrs()(function() {
        return {
            src: this.ctx.url
        };
    }))
