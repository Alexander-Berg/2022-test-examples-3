import ProductSnippet from '@self/platform/spec/page-objects/VertProductSnippet';
import DealSticker from '@self/platform/spec/page-objects/DealsSticker';
import ScrollBox from '@self/platform/spec/page-objects/ScrollBox';


export default {
    suiteName: 'CategoryScrollboxSnippet',
    selector: ScrollBox.snippet,
    ignore: [
        ProductSnippet.price,
        DealSticker.root,
    ],
    capture() {},
};
