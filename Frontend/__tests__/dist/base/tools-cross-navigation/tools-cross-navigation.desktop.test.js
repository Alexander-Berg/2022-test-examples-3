const toolsCrossNav = require('../../../../dist/base/tools-cross-navigation.desktop');
const ctxs = require('./data');
const langs = ['ru', 'en'];

describe('tools-cross-navigation', () => {
    describe('language', () => {
        langs.forEach(lang => {
            ctxs.forEach((ctx, i) => {
                test(`lang ${lang}, ${i}`, () => {
                    let content = toolsCrossNav.getContent({
                        lang,
                        content: 'html',
                        ctx,
                    });

                    expect(content).toMatchSnapshot();
                });
            });
        });
    });
});
