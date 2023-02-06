import { createHtmlElement } from '../createElement';

describe('Турбо-оверлей', () => {
    describe('Утилиты', () => {
        describe('Создание элементов', () => {
            it('Cоздает простой тэг', () => {
                expect(createHtmlElement('div')).toMatchSnapshot();
            });

            it('Cоздает тэг с аттрибутами', () => {
                expect(createHtmlElement('div', { attr: 'abc' })).toMatchSnapshot();
            });

            it('Cоздает тэг с ребенком-строкой', () => {
                expect(createHtmlElement('div', null, 'hello world')).toMatchSnapshot();
            });

            it('Cоздает тэг с ребенком-элементом', () => {
                expect(createHtmlElement('div', null, [createHtmlElement('div')])).toMatchSnapshot();
            });

            it('Cоздает тэг с ребенком в виде строки html', () => {
                expect(createHtmlElement('div', null, { htmlData: '<hr>' })).toMatchSnapshot();
            });

            it('Cоздает тэг с аттрибутами и ребенком-строкой', () => {
                expect(createHtmlElement('div', { attr: 'abc' }, 'hello world')).toMatchSnapshot();
            });

            it('Cоздает тэг с аттрибутами и ребенком-элементом', () => {
                expect(createHtmlElement('div', { attr: 'abc' }, [createHtmlElement('div')])).toMatchSnapshot();
            });

            it('Cоздает тэг с аттрибутами и ребенком в виде строки html', () => {
                expect(createHtmlElement('div', { attr: 'abc' }, { htmlData: '<hr>' })).toMatchSnapshot();
            });
        });
    });
});
