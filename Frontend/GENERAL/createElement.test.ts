import { createElement } from './createElement';

describe('createElement', () => {
    it('Корректно создает ДОМ-ноду по параметрам', () => {
        const node = createElement({
            tag: 'div',
            content: 'content',
            attrs: {
                id: 'myDiv',
            },
        });

        expect(node.tagName).toBe('DIV');
        expect(node.innerHTML).toBe('content');
        expect(node.id).toBe('myDiv');
    });
});
