import MaterialFooter from '@self/platform/spec/page-objects/Journal/MaterialFooter';
import Counter from '@self/platform/spec/page-objects/Journal/Counter';

export default {
    suiteName: 'MaterialFooter',
    selector: MaterialFooter.root,
    ignore: {every: Counter.root},
    capture() {},
};
