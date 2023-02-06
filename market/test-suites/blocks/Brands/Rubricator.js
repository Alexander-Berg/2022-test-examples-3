import Rubricator from '@self/platform/spec/page-objects/Rubricator';

export default {
    suiteName: 'Rubricator',
    selector: Rubricator.root,
    ignore: [
        {every: Rubricator.navnode},
    ],
    capture() {
    },
};
