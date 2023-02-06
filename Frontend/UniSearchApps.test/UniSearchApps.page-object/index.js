const { ReactEntity } = require('../../../../../../vendors/hermione');
const { Layout } = require('../../../../UniSearch.components/Layout/Layout.test/Layout.page-object');
const { QuickFilter } = require('../../../../UniSearch.components/QuickFilter/QuickFilter.test/QuickFilter.page-object');
const { More } = require('../../../../UniSearch.components/More/More.test/More.page-object');
const { List } = require('../../../../UniSearch.components/List/List.test/List.page-object');
const { UniSearchPreview } = require('../../../../UniSearch.components/Preview/Preview.test/Preview.page-object/index@touch-phone');
const { UniSearchAppsPreview } = require('../../UniSearchApps.components/Preview/Preview.page-objects');

const UniSearchApps = new ReactEntity({ block: 'UniSearchApps' }).mix(Layout);

UniSearchApps.Header.FilterScroller.QuickFilter = QuickFilter.copy();
UniSearchApps.Content.List = List.copy();
UniSearchApps.Footer.More = More.copy();

UniSearchApps.Content.List.Item.Container = new ReactEntity({ block: 'UniSearchAppsItem', elem: 'Container' });
UniSearchApps.Content.List.Item.MainLink = new ReactEntity({ block: 'UniSearchAppsItem', elem: 'MainLink' });

module.exports = {
    UniSearchApps,
    UniSearchPreview,
    UniSearchAppsPreview,
};
