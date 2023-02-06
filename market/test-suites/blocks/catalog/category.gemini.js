import ClarifyCategory from '@self/platform/spec/page-objects/ClarifyingCategories';
import ClarifyCategoryCard from '@self/platform/components/ClarifyCategoryCard/__pageObject';

export default {
    suiteName: 'ClarifyCategory',
    selector: ClarifyCategory.root,
    ignore: [{every: ClarifyCategoryCard.root}],
    capture() {},
};
