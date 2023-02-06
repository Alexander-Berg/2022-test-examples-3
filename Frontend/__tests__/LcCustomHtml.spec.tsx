import * as React from 'react';
import 'jest';
import { render } from 'enzyme';
import { LcCustomHtml } from '../LcCustomHtml';

const props = {
    anchor: 'lc-custom-html',
    events: [],
    sectionId: '',
    isPreview: true,
};

describe('CustomHtml', () => {
    test('устанавливает innerHtml', () => {
        const html = '<div>Some HTML</div>';
        const customHtml = render(<LcCustomHtml {...props} html={html} />);

        expect(customHtml.html()).toEqual(`<div class="lc-custom-html">${html}</div>`);
    });

    test('вырезает скрипты в режиме превью', () => {
        const html = '<div>Some HTML</div><script>var a;</script><span>Some HTML</span>';
        const customHtml = render(<LcCustomHtml {...props} html={html} />);

        expect(customHtml.html()).toEqual('<div class="lc-custom-html"><div>Some HTML</div><span>Some HTML</span></div>');
    });

    test('оставляет скрипты в продовом окружении', () => {
        const html = '<div>Some HTML</div><script>var a;</script><span>Some HTML</span>';
        const customHtml = render(<LcCustomHtml {...props} html={html} isPreview={false} />);

        expect(customHtml.html()).toEqual(`<div class="lc-custom-html">${html}</div>`);
    });
});
