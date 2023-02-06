import { widgets } from '../widgets';
import { destroy } from '../destroy';
import { flush, getWidget } from './shared';

import '../../typings/global.d';

describe('destroy', () => {
  beforeEach(flush);

  test('should destroy the first widget when id is not provided', () => {
    const { widget } = getWidget();

    destroy();

    expect(widgets[widget.id].destroyed).toBeTruthy();
  });

  test('should set destroyed to true for non destroyed widgets', () => {
    const { widget: widget1 } = getWidget();
    const { widget: widget2 } = getWidget();

    destroy(widget1.id);
    expect(widgets[widget1.id].destroyed).toBeTruthy();

    destroy(widget2.id);
    expect(widgets[widget2.id].destroyed).toBeTruthy();
  });

  test('should call widgets destroy function', () => {
    const { widget } = getWidget();

    destroy();

    expect(widgets[widget.id].destroy).toBeCalledTimes(1);
  });
  test('should call widgets destroy function on multiple widgets', () => {
    const { widget: widget1 } = getWidget();
    const { widget: widget2 } = getWidget();

    destroy(widget1.id);
    destroy(widget2.id);

    expect(widgets[widget1.id].destroy).toBeCalledTimes(1);
    expect(widgets[widget2.id].destroy).toBeCalledTimes(1);
  });

  test('should throw error when widget does not exist', () => {
    expect(() => destroy(-1)).toThrow();
  });

  test('should throw error when widget already destroyed', () => {
    const { widget: destroyedWidget } = getWidget();

    destroy();

    expect(() => destroy(destroyedWidget.id)).toThrow();
  });
});
