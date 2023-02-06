import createWidgetSuite from '../widgetSuiteFabrica';
import widgetSuite from './scrollBox';
import snippetSuite from './snippetInScrollBox';

/** Объединяет провекри метрик ScrollBox и Snippet */
module.exports = createWidgetSuite('ScrollBox.', widgetSuite, snippetSuite);
