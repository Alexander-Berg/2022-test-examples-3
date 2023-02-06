/* global describe, test, expect */
import quotesFormatter from '../utils/quotesFormatter';

describe('quotes formatter', () => {
  test('no quotes', () => {
    const html = '<div>text</div>';

    const result = {
      html: '<div><div>text</div></div>',
      ids: [],
    };

    expect(quotesFormatter(html)).toEqual(result);
  });

  test('one quote', () => {
    const html = '<div>text</div><div><blockquote type="cite">blockquote0</blockquote></div>';

    const result = {
      html: '<div><div>text</div><div><div id="mail__blq_0"></div></div></div>',
      ids: [
        { id: 'mail__blq_0', props: { id: 'mail__blq_0', html: 'blockquote0' } },
      ],
    };

    expect(quotesFormatter(html)).toEqual(result);
  });

  test('two quotes', () => {
    const html = '<div>text</div><div><blockquote type="cite">blockquote0</blockquote></div><div><blockquote type="cite">blockquote1</blockquote></div>';

    const result = {
      html: '<div><div>text</div><div><div id="mail__blq_0"></div></div><div><div id="mail__blq_1"></div></div></div>',
      ids: [
        { id: 'mail__blq_0', props: { id: 'mail__blq_0', html: 'blockquote0' } },
        { id: 'mail__blq_1', props: { id: 'mail__blq_1', html: 'blockquote1' } },
      ],
    };

    expect(quotesFormatter(html)).toEqual(result);
  });

  test('self close tag', () => {
    const html = '<div><div style="height: 1px" /><div>content</div></div>';

    const result = {
      html: '<div><div><div style="height: 1px"></div><div>content</div></div></div>',
      ids: [],
    };

    expect(quotesFormatter(html)).toEqual(result);
  });
});
