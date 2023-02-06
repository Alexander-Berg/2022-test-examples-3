import { widgets } from '../widgets';
import { getResponse } from '../get-response';
import { flush, getWidget } from './shared';
import { destroy } from '../destroy';

import '../../typings/global.d';

describe('get-response', () => {
  beforeEach(flush);

  test('should return first widget response when id is not provided', () => {
    const { widget } = getWidget();

    widget.setToken('token');

    expect(getResponse()).toBe(widgets[widget.id].token);
  });

  test('should return widget response by id', () => {
    const { widget: widget1 } = getWidget();
    const { widget: widget2 } = getWidget();

    widgets[widget1.id].token = 'token1';
    widgets[widget2.id].token = 'token2';

    expect(getResponse(widget1.id)).toBe(widgets[widget1.id].token);
    expect(getResponse(widget2.id)).toBe(widgets[widget2.id].token);
  });

  test('should throw error when id is wrong', () => {
    expect(() => getResponse(-1)).toThrow();
  });

  test('should return empty string when token is not set yet', () => {
    const { widget } = getWidget();

    expect(getResponse(widget.id)).toBe('');
  });

  test('should throw error when widget is destroyed', () => {
    const { widget: destroyedWidget } = getWidget();
    destroy(destroyedWidget.id);

    expect(() => getResponse(destroyedWidget.id)).toThrow();
  });
});
