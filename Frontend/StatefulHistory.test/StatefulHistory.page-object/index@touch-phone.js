const { ReactEntity, Entity } = require('../../../../vendors/hermione');

const elems = {};

elems.statefulHistory = new ReactEntity({ block: 'StatefulHistory' });
elems.statefulHistory.HistoryCollapser = new ReactEntity({ block: 'StatefulHistoryCollapser' });
elems.statefulHistory.HistoryCollapser.Collapser = new ReactEntity({ block: 'Collapser' });
elems.statefulHistory.HistoryCollapser.Collapser.Label = new ReactEntity({ block: 'Collapser', elem: 'Label' });
elems.statefulHistory.HistoryCollapser.CollapserOpened = new ReactEntity({ block: 'Collapser' }).mods({ opened: true });
elems.statefulHistory.HistoryCollapser.CollapserOpened.StatefulSite = new ReactEntity({ block: 'StatefulSite' });
elems.statefulHistory.HistoryCollapser.CollapserOpened.Button = new ReactEntity({ block: 'Button2' });
elems.statefulHistory.statefulRequest = new ReactEntity({ block: 'StatefulRequest' });
elems.statefulHistory.subtitle = new ReactEntity({ block: 'StatefulHistory', elem: 'Subtitle' });

elems.statefulHistory.SecondHistoryCollapser = new ReactEntity({ block: 'StatefulHistoryCollapser' }).nthChild(3);
elems.statefulHistory.SecondHistoryCollapser.Collapser = new ReactEntity({ block: 'Collapser' });
elems.statefulHistory.SecondHistoryCollapser.Collapser.Label = new ReactEntity({ block: 'Collapser', elem: 'Label' });

elems.statefulHistory.settingsIcon = new ReactEntity({ block: 'StatefulHistory', elem: 'SettingsIcon' });

elems.statefulHistory.requestLink = new ReactEntity({ block: 'StatefulRequest', elem: 'RequestLink' });
elems.statefulHistory.moreLink = new ReactEntity({ block: 'StatefulRequest', elem: 'MoreLink' });
elems.statefulHistory.siteLink = new ReactEntity({ block: 'StatefulSite', elem: 'Link' });

elems.statefulSettingPopup = new ReactEntity({ block: 'StatefulHistory', elem: 'Popup' });
elems.statefulSettingPopup.Link = new ReactEntity({ block: 'StatefulHistory', elem: 'ClearHistoryLink' });

elems.statefulHistoryDrawer = new ReactEntity({ block: 'StatefulHistory', elem: 'Drawer' });
elems.statefulHistoryDrawer.buttonConfirm = new ReactEntity({ block: 'StatefulHistory', elem: 'Button' }).nthChild(1);
elems.statefulHistoryDrawer.buttonCancel = new ReactEntity({ block: 'StatefulHistory', elem: 'Button' }).nthChild(2);

elems.header = new Entity({ block: 'HeaderPhone' });
elems.main = new Entity({ block: 'main' });

module.exports = elems;
