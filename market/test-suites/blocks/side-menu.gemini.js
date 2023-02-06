import SideMenu from '@self/platform/spec/page-objects/widgets/core/SideMenuRedesign/SideMenu';

export default {
    suiteName: 'SideMenu',
    selector: SideMenu.root,
    capture(actions) {
        actions.waitForElementToShow(SideMenu.root, 5000);
    },
};
