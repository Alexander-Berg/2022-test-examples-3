block('user').elem('icon')
    .match(function() { return this._icon; })
    .attrs()(function() {
        return {
            style: 'background-image: url(' +
                '../../../common.blocks/user/user.tests/gemini.blocks/avatar/avatar.png' +
                ');'
        };
    });
