import HeaderTabs from '@self/platform/widgets/content/HeaderTabs/__pageObject';

export default {
    suiteName: 'HorizontalMenuItems',
    selector: HeaderTabs.root,
    capture(actions) {
        actions.waitForElementToShow(HeaderTabs.root, 5000);
    },
};
