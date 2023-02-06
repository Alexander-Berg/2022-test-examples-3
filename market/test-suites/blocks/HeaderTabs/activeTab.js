import Header from '@self/platform/spec/page-objects/header2-menu';
import CatalogTab from '@self/platform/widgets/content/HeaderCatalog/CatalogTab/__pageObject/index.js';

const verticalMenuItemSelector = `${CatalogTab.root}:nth-child(2)`;

export default {
    suiteName: 'TabInteractionActiveYes',
    selector: `${CatalogTab.activeTab}`,
    capture(actions, find) {
        actions
            .waitForElementToShow(Header.catalogEntrypoint, 10000)
            .click(find(Header.catalogEntrypoint))
            .waitForElementToShow(verticalMenuItemSelector, 10000)
            .mouseMove(find(verticalMenuItemSelector))
            .wait(1000);
    },
};
