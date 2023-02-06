describeBlock('i-log__initialize', block => {
    let data;
    let nodes;

    beforeEach(() => {
        data = {
            reqdata: {
                reqid: '1540494129299419-1829335483911191362356773-vla1-2964-TCH',
                blockstat: {
                    tree: {
                        event: 'show',
                        tree: {
                            name: '$page',
                            children: [{ name: '$header', children: [{ name: 'child1' }] }]
                        }
                    }
                }
            },
            entry: 'pre-search',
            expFlags: {}
        };
        nodes = [{ name: '$header', children: [{ name: 'child2' }] }];
    });

    describe('mergeHeaders', () => {
        it('should not merge on pre-search', () => {
            block(data);

            assert.deepEqual(data.log.mergeHeaders(nodes), nodes);
            assert.deepEqual(
                data.log.baobabTree.tree.children,
                [{ name: '$header', children: [{ name: 'child1' }] }]
            );
        });

        it('should not merge on ajax request', () => {
            data.entry = 'post-search';
            data.ajax = '{}';

            block(data);

            assert.deepEqual(data.log.mergeHeaders(nodes), nodes);
            assert.deepEqual(
                data.log.baobabTree.tree.children,
                [{ name: '$header', children: [{ name: 'child1' }] }]
            );
        });

        it('should merge header nodes', () => {
            data.entry = 'post-search';

            block(data);

            assert.deepEqual(
                data.log.mergeHeaders(nodes),
                []
            );
            assert.deepEqual(
                data.log.baobabTree.tree.children,
                [{ name: '$header', children: [{ name: 'child1' }, { name: 'child2' }] }]
            );
        });
    });
});
