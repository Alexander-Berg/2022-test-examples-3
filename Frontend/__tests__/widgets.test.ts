import { widgets } from '../widgets';
import { getInvisibleWidget, getWidget } from './shared';

describe('WidgetManager', () => {
  test('should set `invisible` when execute is present in props', () => {
    const { widget } = getWidget();
    const { widget: invisibleWidget } = getInvisibleWidget();

    expect(widgets[widget.id].invisible).toBeFalsy();
    expect(widgets[invisibleWidget.id].invisible).toBeTruthy();
  });
});
