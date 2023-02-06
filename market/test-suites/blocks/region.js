import Region from '@self/platform/spec/page-objects/region';

export default {
    suiteName: 'RegionForm',
    selector: '[data-auto="modal"]',
    before(actions, find) {
        actions
            .click(find(Region.regionEntrypoint))
            .wait(1000);
    },
    after(actions, find) {
        actions
            .click(find(Region.closeButton))
            .wait(1000);
    },
    capture() {},
};

