const { ReactEntity } = require('../../../../../../vendors/hermione');
const { Layout } = require('../../../../UniSearch.components/Layout/Layout.test/Layout.page-object');
const { FilterAppendix } = require('../../../../UniSearch.components/FilterAppendix/FilterAppendix.test/FilterAppendix.page-object');
const {
    UniSearchFilter,
    UniSearchFilterMain,
    UniSearchPopupFilterTitle,
    UniSearchPopupCancelButton,
} = require('../../../../UniSearch.components/Filter/Filter.test/Filter.page-object');
const { QuickFilter } = require('../../../../UniSearch.components/QuickFilter/QuickFilter.test/QuickFilter.page-object');
const { OrderSelect } = require('../../../../../../experiments/unisearch_sort_feature/features/UniSearch/UniSearch.components/OrderSelect/OrderSelect.test/OrderSelect.page-object');
const { More } = require('../../../../UniSearch.components/More/More.test/More.page-object');
const { List } = require('../../../../UniSearch.components/List/List.test/List.page-object');
const { UniSearchPreview } = require('../../../../UniSearch.components/Preview/Preview.test/Preview.page-object/index@common');
const { UniSearchEducationPreview } = require('../../UniSearchEducation.components/Preview/Preview.page-objects');
const { UniSearchPopup } = require('../../../../UniSearch.components/Popup/Popup.test/Popup.page-object');

const UniSearchEducation = new ReactEntity({ block: 'UniSearchEducation' }).mix(Layout);

UniSearchEducation.Header.FilterAppendix = FilterAppendix.copy();
UniSearchEducation.Header.MainFilter = UniSearchFilterMain.copy();
UniSearchEducation.Header.FilterScroller.Filter = UniSearchFilter.copy();
UniSearchEducation.Header.FilterScroller.FirstFilter =
    UniSearchEducation.Header.FilterScroller.Filter.copy().withIndex(0);
UniSearchEducation.Header.FilterScroller.SecondFilter =
    UniSearchEducation.Header.FilterScroller.Filter.copy().withIndex(1);
UniSearchEducation.Header.FilterScroller.ThirdFilter =
    UniSearchEducation.Header.FilterScroller.Filter.copy().withIndex(2);
UniSearchEducation.Header.FilterScroller.QuickFilter = QuickFilter.copy();
UniSearchEducation.Header.OrderSelect = OrderSelect.copy();
UniSearchEducation.Content.List = List.copy();
UniSearchEducation.Footer.More = More.copy();

UniSearchEducation.Content.List.Item.Info = new ReactEntity({ block: 'UniSearchEducation', elem: 'ItemInfo' });
UniSearchEducation.Content.List.Item.Price = new ReactEntity({ block: 'UniSearchEducation', elem: 'ItemPrice' });
UniSearchEducation.Content.List.Item.DefinedPrice = new ReactEntity({
    block: 'UniSearchEducation',
    elem: 'ItemDefinedPrice',
});
UniSearchEducation.Content.List.Item.Title = new ReactEntity({ block: 'UniSearchEducation', elem: 'ItemTitle' });
UniSearchEducation.Content.List.Item.Org = new ReactEntity({ block: 'UniSearchEducation', elem: 'ItemOrg' });

UniSearchPopup.FilterTitle = UniSearchPopupFilterTitle.copy();
UniSearchPopup.FirstFilterTitle = UniSearchPopup.FilterTitle.copy().withIndex(0);
UniSearchPopup.SecondFilterTitle = UniSearchPopup.FilterTitle.copy().withIndex(1);
UniSearchPopup.ThirdFilterTitle = UniSearchPopup.FilterTitle.copy().withIndex(2);
UniSearchPopup.CancelButton = UniSearchPopupCancelButton.copy();

module.exports = {
    UniSearchEducation,
    UniSearchEducationPreview,
    UniSearchPreview,
    UniSearchPopup,
};
