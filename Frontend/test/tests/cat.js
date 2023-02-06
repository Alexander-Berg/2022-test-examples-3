import test from 'ava';
import { cat } from '../..';

test.cb('should cat', t => {
    const node = {};

    cat.bind({
        cat: function() {
            return new Promise(resolve => {
                resolve('{}');
            });
        },
    })(node, function(err) {
        if (err) {
            return t.end(err);
        }

        t.is(node.content, '{}');
        t.end();
    });
});
