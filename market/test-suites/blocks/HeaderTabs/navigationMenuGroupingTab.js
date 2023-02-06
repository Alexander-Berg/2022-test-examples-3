import Header from '@self/platform/spec/page-objects/header2-menu';
import HeaderCatalog from '@self/platform/widgets/content/HeaderCatalog/__pageObject';

const groupingControlSelector = `${Header.catalogEntrypoint}`;

export default {
    suiteName: 'TabTypeNavigationMenuGrouping',
    selector: groupingControlSelector,
    capture: {
        plain() {},
        opened(actions, find) {
            actions
                .waitForElementToShow(groupingControlSelector, 5000)
                .click(find(groupingControlSelector))
                .waitForElementToShow(HeaderCatalog.root, 5000);
        },
        closed(actions, find) {
            actions
                .click(find(groupingControlSelector))
                .waitForElementToHide(HeaderCatalog.root, 5000);
        },
    },
};
