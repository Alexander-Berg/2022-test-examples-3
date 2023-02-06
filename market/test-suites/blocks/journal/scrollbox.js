import ScrollBox from '@self/platform/spec/page-objects/ScrollBox';
import Counter from '@self/platform/spec/page-objects/Journal/Counter';

export default {
    suiteName: 'ScrollBox',
    selector: ScrollBox.root,
    ignore: [
        {every: Counter.root},
        {every: `${ScrollBox.root} picture`},
    ],
    capture() {},
};
