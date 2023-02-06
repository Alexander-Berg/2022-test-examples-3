import ProductSnippet from '@self/platform/spec/page-objects/Journal/product-snippet';
import Paranja from '@self/platform/spec/page-objects/paranja';
import ModalFloat from '@self/platform/spec/page-objects/components/ModalFloat';
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import Mooa from '@self/platform/spec/page-objects/mooa';
import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';

export default {
    suiteName: 'ProductSnippet',
    url: '/journal/story/test-suites-product-snippet',
    selector: ProductSnippet.root,
    ignore: {every: ProductSnippet.price},
    before(actions) {
        const selector = [
            ModalFloat.overlay,
            `${Paranja.root}${Paranja.stateOpen}`,
            RegionPopup.content,
            Mooa.root,
        ].join(', ');

        new ClientAction(actions).removeElems(selector);
    },
    capture(actions) {
        actions.wait(1000);
        actions.waitForElementToShow(ProductSnippet.root, 50000);
    },
};
