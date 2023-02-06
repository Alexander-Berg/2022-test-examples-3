import createWidgetSuite from '../widgetSuiteFabrica';
import widgetSuite from './productsGrids';
import snippetSuite from './snippetInProductsGrids';

/** Объединяет провекри метрик ProductsGrids и Snippet */
module.exports = createWidgetSuite('ProductsGrids.', widgetSuite, snippetSuite);
