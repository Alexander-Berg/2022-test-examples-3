import { reset } from '../reset';

import { destroy } from '../destroy';
import { getWidget, flush } from './shared';
import { widgets } from '../widgets';

import '../../typings/global.d';

describe('reset', () => {
  beforeEach(flush);

  test('should call reset function on first widget', () => {
    const { widget } = getWidget();

    reset();

    expect(widgets[widget.id].reset).toBeCalledTimes(1);
  });

  test('should call reset function on specified widget id', () => {
    const { widget: widget1 } = getWidget();
    const { widget: widget2 } = getWidget();

    reset(widget2.id);

    expect(widgets[widget2.id].reset).toBeCalledTimes(1);

    reset(widget1.id);

    expect(widgets[widget1.id].reset).toBeCalledTimes(1);
  });

  test('should throw error when widget id is wrong', () => {
    expect(() => reset(-1)).toThrow();
  });

  test('should throw error when widget is destroyed', () => {
    const { widget: destroyedWidget } = getWidget();

    destroy(destroyedWidget.id);

    expect(() => reset(destroyedWidget.id)).toThrow();
  });
});
