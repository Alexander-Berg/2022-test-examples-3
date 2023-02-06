import SearchSnippetCell from '@self/project/src/components/Search/Snippet/Cell/__pageObject';

export default {
    suiteName: 'SearchSnippetCell',
    selector: SearchSnippetCell.root,
    ignore: [
        SearchSnippetCell.morePrices,
        {every: SearchSnippetCell.mainPrice},
        {every: SearchSnippetCell.image},
        {every: SearchSnippetCell.reasonsToBuy},
    ],
    capture() {},
};
