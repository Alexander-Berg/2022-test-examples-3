const toolsHeaderSuggestItem = require('../../../../dist/base/tools-header-suggest-item.desktop');
const { describeCases } = require('./data');

describe('tools-header-suggest-item', () => {
    let script;
    beforeEach(() => {
        const item = document.createElement('div');
        item.className = 'tools-header-suggest-item';
        item.setAttribute('data-bem', JSON.stringify({
            'tools-header-suggest-item': {
                statuses: {
                    develop: 'Развивается',
                },
            },
        }));
        document.body.appendChild(item);
        script = document.createElement('script');
        script.innerHTML = toolsHeaderSuggestItem.getContent({ content: 'js' });
        document.head.append(script);
    });

    afterEach(() => script.remove());

    describe('gaps', () => {
        test.each(describeCases.gaps.map(item => [item.name, item.item]))('should render item with %s gap', (_, data) => {
            const item = window.Lego.ToolsHeaderSuggestItem.generateHTML(data, 0, '');
            expect(item).toMatchSnapshot();
        });
    });

    describe('highlights', () => {
        test.each(describeCases.highlights.map(i => [i.name, i.item]))('should highlight %s', (_, data) => {
            const item = window.Lego.ToolsHeaderSuggestItem.generateHTML(data, 0, '');
            expect(item).toMatchSnapshot();
        });
    });

    describe('people', () => {
        test.each(describeCases.people.map(i => [i.name, i.item]))('should render %s', (_, data) => {
            const item = window.Lego.ToolsHeaderSuggestItem.generateHTML(data, 0, '');
            expect(item).toMatchSnapshot();
        });
    });

    describe('services', () => {
        test.each(describeCases.services.map(i => [i.name, i.item]))('should highlight %s', (_, data) => {
            const item = window.Lego.ToolsHeaderSuggestItem.generateHTML(data, 0, '');
            expect(item).toMatchSnapshot();
        });
    });
});
