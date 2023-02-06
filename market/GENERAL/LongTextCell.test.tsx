import React from 'react';
import { fireEvent, render } from '@testing-library/react';
import { LongTextCell } from './LongTextCell';
import { act } from 'react-dom/test-utils';

const shortText = `ru.yandex.market.gutgin.tms.manager.TaskException`;

const longText = `ru.yandex.market.gutgin.tms.manager.TaskException
Request failed 3 times. Host: http://cs-markup-worker01h.market.yandex.net:34535/
ru.yandex.market.gutgin.tms.manager.TaskException: Request failed 3 times. Host: http://cs-markup-worker01h.market.yandex.net:34535/
at ru.yandex.market.gutgin.tms.utils.TaskUtils.wrapServiceException(TaskUtils.java:22)
at ru.yandex.market.gutgin.tms.utils.MarkupHelper.getResult(MarkupHelper.java:64)
`;

describe('LongTextCell', () => {
  test('render', () => {
    const app = render(<LongTextCell maxLength={shortText.length} value={longText} />);
    app.getByText(shortText);

    act(() => {
      fireEvent.click(app.getByRole('button'));
    });

    app.getByText(new RegExp('TaskUtils.java:22', 'i'));

    act(() => {
      fireEvent.click(app.getByRole('button'));
    });

    app.getByText(shortText);
  });
});
