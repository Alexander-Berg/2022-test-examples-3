import { flush, getInvisibleWidget } from './shared';
import { execute } from '../execute';
import { destroy } from '../destroy';

describe('execute', () => {
  beforeEach(flush);

  test('should call execute on first widget when id is not provided', () => {
    const { props: { execute } } = getInvisibleWidget();

    execute();

    expect(execute).toBeCalled();
  });

  test('should call execute on widget by id', () => {
    const { widget: widget1, props: { execute: execute1 } } = getInvisibleWidget();
    const { widget: widget2, props: { execute: execute2 } } = getInvisibleWidget();

    execute(widget1.id);
    expect(execute1).toBeCalled();

    execute(widget2.id);
    expect(execute2).toBeCalled();
  });

  test('should throw error when id is wrong', () => {
    expect(() => execute(-1)).toThrow();
  });

  test('should throw error when widget is destroyed', () => {
    const { widget: destroyedWidget } = getInvisibleWidget();

    destroy(destroyedWidget.id);

    expect(() => execute(destroyedWidget.id)).toThrow();
  });
});
