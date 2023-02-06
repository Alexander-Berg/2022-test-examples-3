import { showError } from '../show-error';
import { getWidget, flush } from './shared';
import { destroy } from '../destroy';
import { widgets } from '../widgets';

import '../../typings/global.d';

describe('show-error', () => {
  beforeEach(flush);

  test('should call showError on first widgets when id is not defined', () => {
    const { widget } = getWidget();

    showError();

    expect(widgets[widget.id].showError).toBeCalledTimes(1);
  });

  test('should call showError when id is specified', () => {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    getWidget();
    const { widget: widget2 } = getWidget();

    showError(widget2.id);

    expect(widgets[widget2.id].showError).toBeCalledTimes(1);
  });

  test('should throw error when id is wrong', () => {
    expect(() => showError(-1)).toThrow();
  });

  test('should throw error when widget is destroyed', () => {
    const { widget: destroyedWidget } = getWidget();
    destroy(destroyedWidget.id);

    expect(() => showError(destroyedWidget.id)).toThrow();
  });
});
