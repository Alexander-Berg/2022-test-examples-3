const Entity = require('bem-page-object').Entity;

const fTabs = new Entity({ block: 'f-tabs' });
fTabs.tabs = new Entity({ block: 'tabs' });
fTabs.firstTab = new Entity({ block: 'tabs', elem: 'tab' }).pseudo('first-child');
fTabs.lastTab = new Entity({ block: 'tabs', elem: 'tab' }).pseudo('last-child');
fTabs.panes = new Entity({ block: 'tabs-panes' });
fTabs.visiblePane = new Entity({ block: 'tabs-panes', elem: 'pane' }).pseudo('visible');

module.exports = fTabs;
