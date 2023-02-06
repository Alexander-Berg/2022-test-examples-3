import SearchProductTile from '@self/project/src/components/SearchProductTile/__PageObject';
import OffersCounter from '@self/project/src/components/OffersCounter/__pageObject';

export default {
    suiteName: 'SearchProductTile',
    selector: SearchProductTile.root,
    ignore: [
        {every: OffersCounter.root},
    ],
    capture() {},
};
