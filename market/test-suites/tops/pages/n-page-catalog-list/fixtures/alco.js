import {createAlcoOffer, createCategories} from '@self/platform/spec/hermione/fixtures/alco';

const category = createCategories()[0];
const offerId = '456';

export default {
    state: createAlcoOffer(offerId),
    route: {
        nid: category.nid,
        hid: category.id,
        slug: 'vino',
        viewtype: 'list',
    },
};
