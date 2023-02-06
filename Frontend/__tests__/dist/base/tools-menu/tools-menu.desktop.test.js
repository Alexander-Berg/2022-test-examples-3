const toolsMenu = require('../../../../dist/base/tools-menu.desktop');
const ctxs = require('./data');

describe('tools-menu', () => {
    for (const [name, ctx] of Object.entries(ctxs)) {
        test(name, () => {
            const content = toolsMenu.getContent({
                content: 'html',
                ctx,
            });

            expect(content).toMatchSnapshot();
        });
    }
});
