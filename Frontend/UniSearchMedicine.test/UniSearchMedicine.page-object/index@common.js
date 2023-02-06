const { ReactEntity } = require('../../../../../../vendors/hermione');
const { Layout } = require('../../../../UniSearch.components/Layout/Layout.test/Layout.page-object');
const { UniSearchFilter } = require('../../../../UniSearch.components/Filter/Filter.test/Filter.page-object');
const { List } = require('../../../../UniSearch.components/List/List.test/List.page-object');
const { More } = require('../../../../UniSearch.components/More/More.test/More.page-object');
const { UniSearchPreview } = require('../../../../UniSearch.components/Preview/Preview.test/Preview.page-object/index@common');
const { UniSearchMedicinePreview } = require('../../UniSearchMedicine.components/PreviewContent/PreviewContent.test/PreviewContent.page-object');
const { OrderSelect } = require('../../../../../../experiments/unisearch_sort_feature/features/UniSearch/UniSearch.components/OrderSelect/OrderSelect.test/OrderSelect.page-object');
const { UniSearchPopup } = require('../../../../UniSearch.components/Popup/Popup.test/Popup.page-object');

const UniSearchMedicine = new ReactEntity({ block: 'UniSearchMedicine' }).mix(Layout);

UniSearchMedicine.Header.FilterScroller.Filter = UniSearchFilter.copy();
UniSearchMedicine.Header.FilterScroller.FirstFilter =
    UniSearchMedicine.Header.FilterScroller.Filter.copy().withIndex(0);
UniSearchMedicine.Header.FilterScroller.SecondFilter =
    UniSearchMedicine.Header.FilterScroller.Filter.copy().withIndex(1);
UniSearchMedicine.Header.OrderSelect = OrderSelect.copy();
UniSearchMedicine.Content.List = List.copy();
UniSearchMedicine.Footer.More = More.copy();

UniSearchMedicine.Content.List.ItemFirst.PriceValue = new ReactEntity({ block: 'PriceValue' });

module.exports = {
    UniSearchMedicine,
    UniSearchMedicinePreview,
    UniSearchPreview,
    UniSearchPopup,
};
