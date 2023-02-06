({
        block: 'b-page',
        title: 'm-editor-buttons',
        head: [{
            elem: 'css',
            url: '_gemini.css',
            ie: false
        }],
        content: [
        {
            block: 'gemini',
            cls: 'buttons-group',
            content: {
                block: 'm-editor-buttons',
                content: [
                    {
                        block: 'm-editor-button',
                        mods: {type: 'bold', side: 'left'}
                    },
                    {
                        block: 'm-editor-button',
                        mods: {type: 'strikethrough', round: 'none'}
                    },
                    {
                        block: 'm-editor-button',
                        mods: {type: 'italic', round: 'none'}
                    },
                    {
                        block: 'm-editor-button',
                        mods: {type: 'ul', side: 'right'}
                    }
                ]
            }
        },
        {
            block: 'gemini',
            cls: 'single-buttons',
            content: {
                block: 'm-editor-buttons',
                content: {
                        block: 'm-editor-buttons',
                        elem: 'group',
                        content: [(function() {
                            var sides = ['left', 'right'];

                            return sides.map(function(side) {
                                return {
                                    block: 'm-editor-button',
                                    cls: 'single',
                                    mods: {type: 'italic', side: side}
                                };
                            });
                        })(),
                        {
                            block: 'm-editor-button',
                            cls: 'single',
                            mods: {type: 'italic', round: 'none'}
                        }]
                    }
            }
        },
        {
            elem: 'cc',
            condition: 'IE 8',
            content: {
                elem: 'js',
                url: 'https://yastatic.net/es5-shims/0.0.1/es5-shims.min.js'
            }
        },
        {
            elem: 'cc',
            condition: 'IE 8',
            content: {block: 'i-jquery', mods: {version: '1.8.3'}}
        },
        {
            elem: 'cc',
            condition: 'gt IE 8',
            others: true,
            content: {block: 'i-jquery', mods: {version: 'default'}}
        },
        {elem: 'js', url: '_gemini.js'}
    ]
});
