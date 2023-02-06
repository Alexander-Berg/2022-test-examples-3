import createWidgetSuite from '../widgetSuiteFabrica';
import widgetSuite from './roll';
import snippetSuite from './snippetInRoll';

/** Объединяет провекри метрик Roll и Snippet */
module.exports = createWidgetSuite('Roll.', widgetSuite, snippetSuite);
