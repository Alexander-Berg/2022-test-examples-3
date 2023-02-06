import { render } from '@testing-library/react';

import { textRender, dateColumnRenderer } from './utils';

const shortText = `ru.yandex`;

const longText = `ru.yandex.market.gutgin.tms.manager.TaskException
Request failed 3 times. Host: http://cs-markup-worker01h.market.yandex.net:34535/
ru.yandex.market.gutgin.tms.manager.TaskException: Request failed 3 times. Host: http://cs-markup-worker01h.market.yandex.net:34535/
`;

const textRenderCases = [
  {
    title: 'small text',
    value: 'some text',
    expect: 'some text',
  },
  {
    title: 'array text',
    value: ['some text 1', 'some text 2'],
    expect: ['some text 1', 'some text 2'].join(', '),
  },
  {
    title: 'long text',
    value: longText,
    expect: new RegExp(shortText),
  },
  {
    title: 'invalid value',
    value: { props: true },
    expect: 'Invalid value type!',
  },
];

describe('utils', () => {
  textRenderCases.forEach(el => {
    test(`textRender ${el.title}`, () => {
      const renderCell = textRender('text');
      const app = render(renderCell({ text: el.value }));
      app.getByText(el.expect);
    });
  });

  test('dateColumnRenderer', () => {
    const date = new Date(1995, 8, 22);
    const renderCell = dateColumnRenderer('date');
    const app = render(renderCell({ date: +date }));
    app.getByText('22.09.1995 00:00');
  });
});
