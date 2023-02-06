import FeedSnippet from '@self/platform/spec/page-objects/FeedSnippet';

export default {
    suiteName: 'FeedSnippet',
    selector: FeedSnippet.root,
    ignore: [{every: FeedSnippet.price}],
    capture() {},
};
