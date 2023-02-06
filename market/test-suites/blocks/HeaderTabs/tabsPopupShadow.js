import Header from '@self/platform/spec/page-objects/header2-menu';
import Footer from '@self/platform/spec/page-objects/footer-market';
import {waitForElementVisible} from '@self/platform/spec/gemini/helpers/visible';
import disableAnimations from '@self/project/src/spec/gemini/helpers/disableAnimations';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
import CatalogPopup from '@self/platform/widgets/content/HeaderCatalogEntrypoint/CatalogPopup/__pageObject';

export default {
    suiteName: 'TabsPopupShadow',
    selector: Footer.root, // скриншотим футер, убеждаемся, что он за тенью
    ignore: {every: Footer.stats},
    before(actions) {
        initLazyWidgets(actions, 1500);
    },
    capture(actions, find) {
        disableAnimations(actions);
        actions
            .waitForElementToShow(Header.catalogEntrypoint, 10000)
            .click(find(Header.catalogEntrypoint));
        // наконец, проверяем, что сама паранджа появилась
        waitForElementVisible(actions, CatalogPopup.paranja, 10000);
        actions.wait(1000);
    },
};
