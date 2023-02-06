import { bold, cursive, inlineCode, code, lineBreak, link, email, mention } from '../templates';

describe('TextFormatter templates', () => {
    describe('#bold', () => {
        it('returns content in bold tag', () => {
            expect(bold('Lorem ipsum dolor sit amet, consectetur adipiscing elit.'))
                .toBe('<b class="text">Lorem ipsum dolor sit amet, consectetur adipiscing elit.</b>');
        });
    });

    describe('#cursive', () => {
        it('returns content in cursive tag', () => {
            expect(cursive('Lorem ipsum dolor sit amet, consectetur adipiscing elit.'))
                .toBe('<i class="text">Lorem ipsum dolor sit amet, consectetur adipiscing elit.</i>');
        });
    });

    describe('#inlineCode', () => {
        it('returns content in pre & code tag with inline classname', () => {
            expect(inlineCode('Lorem ipsum dolor sit amet, consectetur adipiscing elit.'))
                .toBe('<pre class="yamb-code yamb-code_inline"><code class="yamb-code__text text">Lorem ipsum dolor sit amet, consectetur adipiscing elit.</code></pre>');
        });
    });

    describe('#code', () => {
        it('returns content in pre&code tag', () => {
            expect(code('Lorem ipsum dolor sit amet, consectetur adipiscing elit.'))
                .toBe('<pre class="yamb-code"><code class="yamb-code__text text">Lorem ipsum dolor sit amet, consectetur adipiscing elit.</code></pre>');
        });
    });

    describe('#lineBreak', () => {
        it('returns br tag', () => {
            expect(lineBreak('')).toBe('<br />');
        });
    });

    describe('#link', () => {
        it('returns url & content in anchor tag', () => {
            expect(link('Lorem ipsum dolor sit amet, consectetur adipiscing elit.', 'https://yandex.ru'))
                .toBe('<a href="https://yandex.ru" target="_blank" rel="noopener noreferrer" class="link link_md">Lorem ipsum dolor sit amet, consectetur adipiscing elit.</a>');
        });

        it('returns url as is when url with protocol', () => {
            expect(link('Lorem ipsum dolor sit amet, consectetur adipiscing elit.', 'https://yandex.ru'))
                .toBe('<a href="https://yandex.ru" target="_blank" rel="noopener noreferrer" class="link link_md">Lorem ipsum dolor sit amet, consectetur adipiscing elit.</a>');

            expect(link('Lorem ipsum dolor sit amet, consectetur adipiscing elit.', 'http://yandex.ru'))
                .toBe('<a href="http://yandex.ru" target="_blank" rel="noopener noreferrer" class="link link_md">Lorem ipsum dolor sit amet, consectetur adipiscing elit.</a>');
        });

        it('returns url with http when url without protocol', () => {
            expect(link('Lorem ipsum dolor sit amet, consectetur adipiscing elit.', 'www.yandex.ru'))
                .toBe('<a href="http://www.yandex.ru" target="_blank" rel="noopener noreferrer" class="link link_md">Lorem ipsum dolor sit amet, consectetur adipiscing elit.</a>');
        });
    });

    describe('#email', () => {
        it('returns content in anchor tag', () => {
            expect(email('foo@bar.com'))
                .toBe('<a href="mailto:foo@bar.com" target="_blank" rel="noopener noreferrer" class="link">foo@bar.com</a>');
        });
    });

    describe('#mention', () => {
        it('returns content in span tag', () => {
            expect(mention('Тест Проверка', 'foobar')).toBe('<span class="link" role="link" data-guid="foobar">Тест Проверка</span>');
        });
    });
});
