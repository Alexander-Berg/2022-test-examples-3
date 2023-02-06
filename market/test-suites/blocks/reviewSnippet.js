import ReviewSnippet from '@self/platform/components/Search/Snippet/Review/__pageObject';
import SnippetCell from '@self/project/src/components/Search/Snippet/Cell/__pageObject';

export default {
    suiteName: 'ReviewSnippet',
    selector: ReviewSnippet.root,
    ignore: [
        {every: SnippetCell.price},
        {every: SnippetCell.morePrices},
    ],
    capture() {},
};
