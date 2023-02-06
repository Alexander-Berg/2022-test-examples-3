import assert from 'assert';

import sinon from 'sinon';

import h from '../h';
import patch from '../patch';

describe('patch', () => {
    describe('Text node', () => {
        test('initial new node patch', () => {
            const node = patch(null, h({text: 'test'}));
            assert.strictEqual(node.textContent, 'test');
        });

        test('initial existing node patch', () => {
            const node = document.createTextNode('test1');
            assert.strictEqual(patch(node, h({text: 'test2'})), node);
            assert.strictEqual(node.textContent, 'test2');
        });

        test('sequential patch', () => {
            const vnode1 = h({text: 'test1'});
            const vnode2 = h({text: 'test2'});
            const node = patch(null, vnode1);

            assert.strictEqual(patch(vnode1, vnode2), node);
            assert.strictEqual(node.textContent, 'test2');
        });
    });

    describe('Element node', () => {
        test('initial new node patch', () => {
            const vnode = h({tagName: 'div', id: 'id', children: 'test'});
            const div = patch(null, vnode);
            assert.strictEqual(div.outerHTML, '<div id="id">test</div>');
        });

        test('initial new node patch with key', () => {
            const vnode = h({tagName: 'div', key: 'key', children: 'test'});
            const div = patch(null, vnode);
            assert.strictEqual(div.outerHTML, '<div>test</div>');
        });

        test('initial existing node patch', () => {
            const div = document.createElement('div');
            const vnode = h({
                tagName: 'div',
                id: 'id',
                title: 'test title',
                class: {class1: true, class2: true},
                style: {minHeight: '100%'},
                children: 'test',
            });
            assert.strictEqual(patch(div, vnode), div);
            assert.strictEqual(
                div.outerHTML,
                '<div class="class1 class2" id="id" title="test title"' +
                    ' style="min-height: 100%;">test</div>',
            );
        });

        test('sequential node patch', () => {
            const vnode1 = h({
                tagName: 'div', // not changed
                id: 'id1', // not changed
                title: 'test title', // emptied
                class: {class1: true, class2: true}, // updated
                style: {color: 'red', minHeight: '100%'}, // updated
                tabIndex: 25, // removed
                children: 'test', // not changed
            });
            const vnode2 = h({
                tagName: 'div',
                id: 'id2',
                lang: 'en',
                title: '',
                class: {class2: true, class3: true},
                style: {minHeight: '100%', maxHeight: '200%'},
                children: 'test',
            });
            const div = patch(null, vnode1);
            patch(vnode1, vnode2);
            assert.strictEqual(
                div.outerHTML,
                '<div class="class2 class3" id="id2" title=""' +
                    ' tabindex="-1"' +
                    ' style="min-height: 100%; max-height: 200%;"' +
                    ' lang="en">test</div>',
            );
        });
    });

    describe('Node replacement', () => {
        test('replace element with text', () => {
            const vdiv = h({tagName: 'div'});
            const div = patch(null, vdiv);
            const text = patch(vdiv, h({text: 'test'}));
            assert.notStrictEqual(div, text);
        });

        test('replace element with different key', () => {
            const node1 = h({tagName: 'div', key: 1});
            const node2 = h({tagName: 'div', key: 2});
            const div = patch(null, node1);
            assert.notStrictEqual(patch(node1, node2), div);
        });

        test('replace element with cached element', () => {
            const node1 = h({tagName: 'div', key: 1});
            const node2 = h({tagName: 'div', key: 2});
            const div1 = patch(null, node1);
            patch(node1, node2);
            assert.strictEqual(patch(node2, node1), div1);
        });
    });

    describe('Node children', () => {
        let div;
        beforeEach(() => {
            div = document.createElement('div');
        });

        test('Add children', () => {
            const vnode1 = h.div({children: []});
            const vnode2 = h.div({
                children: [
                    h.span({children: 'span'}),
                    'text',
                    h.div({children: 'div'}),
                ],
            });
            patch(div, vnode1);

            patch(vnode1, vnode2);
            assert.strictEqual(
                div.innerHTML,
                '<span>span</span>text<div>div</div>',
            );
        });

        test('Remove children', () => {
            const vnode1 = h.div({
                children: [
                    h.span({children: 'span'}),
                    'text',
                    h.div({children: 'div'}),
                ],
            });
            const vnode2 = h.div({children: []});
            patch(div, vnode1);

            patch(vnode1, vnode2);
            assert.strictEqual(div.innerHTML, '');
        });

        test('Remove cached children', () => {
            const [c1, c2, c3] = [
                h.span({key: 1, children: '1'}),
                h.span({key: 2, children: '2'}),
                h.span({key: 3, children: '3'}),
            ];
            const vnode1 = h.div({children: [c1, c2, c3]});
            const vnode2 = h.div({children: [c2, c3]});
            patch(div, vnode1);

            patch(vnode1, vnode2);
            assert.strictEqual(div.innerHTML, '<span>2</span><span>3</span>');
        });

        test('Replace multiple children', () => {
            const vnode1 = h.div({
                children: [
                    'text',
                    h.div({children: 'div'}),
                    h.span({children: 'span'}),
                    h.div({children: 'div', id: 1}),
                ],
            });
            const vnode2 = h.div({
                children: [
                    h.span({children: 'span'}),
                    'text',
                    h.div({children: 'div'}),
                    h.div({children: 'div', id: 2}),
                ],
            });
            patch(div, vnode1);

            patch(vnode1, vnode2);
            assert.strictEqual(
                div.innerHTML,
                '<span>span</span>text<div>div</div><div id="2">div</div>',
            );
        });

        test('Replace cached children', () => {
            const cachedChild1 = h.div({id: 'div1'});
            const vnode1 = h.div({children: ['text', cachedChild1]});
            patch(div, vnode1);

            const cachedChild2 = h.div({id: 'div2'});
            const vnode2 = h.div({children: ['text', cachedChild2]});
            patch(vnode1, vnode2);

            const vnode3 = h.div({children: ['text', cachedChild1]});

            assert.throws(() => patch(vnode2, vnode3), /can't reuse/i);
        });

        test('Rearrange cached children', () => {
            const children = [];
            for (let i = 0; i < 5; i++) children.push(h.div({children: i}));
            const [child0, child1, child2, child3, child4] = children;
            const vnode1 = h.div({children: [child0, child1, child2, child3]});
            const vnode2 = h.div({children: [child4, child3, child1, child2]});
            patch(div, vnode1);
            const [, el1, el2, el3] = div.children;

            patch(vnode1, vnode2);
            const [, newEl3, newEl1, newEl2] = div.children;
            assert.strictEqual(
                div.innerHTML,
                '<div>4</div><div>3</div><div>1</div><div>2</div>',
            );

            assert.strictEqual(el1, newEl1);
            assert.strictEqual(el2, newEl2);
            assert.strictEqual(el3, newEl3);
        });

        test('Rearrange across hierachy (replace)', () => {
            const child = h.p({children: 'p'});
            const vnode1 = h.div({children: child});
            const vnode2 = h.div({children: h.span({children: child})});

            patch(div, vnode1);
            const el = div.children[0];
            patch(vnode1, vnode2);
            const newEl = div.children[0].children[0];

            assert.strictEqual(el, newEl);
        });

        test('Rearrange across hierachy (insert)', () => {
            const child = h.p({children: 'p'});
            const vnode1 = h.div({
                children: [h.span({}), child, h.span({})],
            });
            const vnode2 = h.div({
                children: [h.span({}), h.span({}), h.span({children: child})],
            });

            patch(div, vnode1);
            const el = div.children[1];
            patch(vnode1, vnode2);
            const newEl = div.children[2].children[0];

            assert.strictEqual(el, newEl);
        });

        test('Rearrange across hierachy (remove)', () => {
            const child = h.p({children: 'p'});
            const vnode1 = h.div({
                children: [h.span({}), child],
            });
            const vnode2 = h.div({
                children: [h.span({children: child})],
            });

            patch(div, vnode1);
            const el = div.children[1];
            patch(vnode1, vnode2);
            const newEl = div.children[0].children[0];

            assert.strictEqual(el, newEl);
        });

        test('Duplicated children', () => {
            const ch = h.div({children: 'div'});
            assert.throws(() => {
                const vnode1 = h.div({children: [ch, 'text', ch]});
                patch(div, vnode1);
            }, /Child VHTMLElement used more than once/);
        });

        test('Duplicated siblings', () => {
            const ch = h.div({children: 'div'});
            const vnode1 = h.div({
                children: [
                    h.div({children: [ch]}),
                    'text',
                    h.div({children: [ch]}),
                ],
            });
            assert.throws(
                () => patch(div, vnode1),
                /Can't patch same VHTMLElement more than once/,
            );
        });
    });

    describe('Hooks', () => {
        let div;
        beforeEach(() => {
            div = document.createElement('div');
        });

        test('Patch hook', () => {
            const afterUpdate = sinon.spy();
            const vnode1 = h({
                tagName: 'div',
                children: 'test1',
            });
            const vnode2 = h({
                tagName: 'div',
                children: 'test2',
                hooks: {afterUpdate},
            });
            patch(null, vnode1);
            patch(vnode1, vnode2);
            assert.equal(afterUpdate.callCount, 1);
            assert.strictEqual(afterUpdate.args[0][0], vnode2);
            assert.strictEqual(afterUpdate.args[0][1], vnode1);
        });

        test('Attach hook', () => {
            const afterAttach = sinon.spy();
            const child = h.div({
                children: 'div',
                hooks: {afterAttach},
            });
            const vnode1 = h.div({children: []});
            const vnode2 = h.div({children: child});
            patch(div, vnode1);

            patch(vnode1, vnode2);

            assert.equal(afterAttach.callCount, 1);
            assert.strictEqual(afterAttach.args[0][0], child.elm);
            assert.deepEqual(afterAttach.args[0][1], {nested: false});
        });

        test('Attach hook (nested)', () => {
            const afterAttach = sinon.spy();
            const child = h.div({
                id: 'outer',
                children: h.div({
                    id: 'inner',
                    hooks: {afterAttach},
                }),
            });
            patch(null, child);
            afterAttach.reset();

            const vnode1 = h.div({children: []});
            const vnode2 = h.div({children: child});
            patch(div, vnode1);

            patch(vnode1, vnode2);

            assert.equal(afterAttach.callCount, 1);
            assert.strictEqual(afterAttach.args[0][0].parentElement, child.elm);
            assert.deepEqual(afterAttach.args[0][1], {nested: true});
        });

        test('Move hook', () => {
            const beforeDetach = sinon.spy();
            const afterAttach = sinon.spy();
            const child = h.p({
                children: 'p',
                hooks: {afterAttach, beforeDetach},
            });
            const vnode1 = h.div({children: child});
            const vnode2 = h.div({children: h.span({children: child})});
            patch(div, vnode1);

            afterAttach.reset();
            patch(vnode1, vnode2);

            assert.equal(beforeDetach.callCount, 1);
            assert.equal(afterAttach.callCount, 1);
            assert.strictEqual(afterAttach.args[0][0], child.elm);
            assert.deepEqual(afterAttach.args[0][1], {nested: true});
        });

        test('Move with detach hook', () => {
            const beforeDetach = sinon.spy();
            const afterAttach = sinon.spy();
            const child = h.p({
                children: 'p',
                hooks: {afterAttach, beforeDetach},
            });
            const vnode1 = h.div({children: child});
            const vnode2 = h.div({
                children: [h.span({}), h.span({children: child})],
            });
            patch(div, vnode1);

            afterAttach.reset();
            patch(vnode1, vnode2);

            assert.equal(beforeDetach.callCount, 1);
            assert.equal(afterAttach.callCount, 1);
            assert.strictEqual(afterAttach.args[0][0], child.elm);
            assert.deepEqual(afterAttach.args[0][1], {nested: true});
        });

        test('Replace hook', () => {
            const beforeDetach = sinon.spy();
            const afterAttach = sinon.spy();
            const child = h.div({
                children: 'div',
                hooks: {beforeDetach},
            });
            const newChild = h.span({
                hooks: {afterAttach},
            });
            const vnode1 = h.div({children: child});
            const vnode2 = h.div({children: newChild});

            patch(div, vnode1);

            patch(vnode1, vnode2);

            assert.equal(beforeDetach.callCount, 1);
            assert.strictEqual(beforeDetach.args[0][0], child.elm);
            assert.strictEqual(
                beforeDetach.args[0][1].replacedWithElm,
                newChild.elm,
            );
            assert.equal(afterAttach.callCount, 1);
            assert.strictEqual(afterAttach.args[0][0], newChild.elm);
            assert.deepEqual(afterAttach.args[0][1], {nested: false});
        });

        test('Detach hook', () => {
            const beforeDetach = sinon.spy(elm =>
                assert.strictEqual(elm.parentNode, div),
            );
            const child = h.div({
                children: 'div',
                hooks: {beforeDetach},
            });
            const vnode1 = h.div({children: child});
            const vnode2 = h.div({children: []});
            patch(div, vnode1);

            patch(vnode1, vnode2);

            assert.equal(beforeDetach.callCount, 1);
            assert.strictEqual(beforeDetach.args[0][0], child.elm);
            assert.equal(beforeDetach.args[0][1].replacedWithElm, null);
        });

        test('Detach hook (nested)', () => {
            const beforeDetach = sinon.spy();
            const child = h.div({
                children: h.div({
                    hooks: {beforeDetach},
                }),
            });
            const vnode1 = h.div({children: child});
            const vnode2 = h.div({children: []});
            patch(div, vnode1);

            patch(vnode1, vnode2);

            assert.equal(beforeDetach.callCount, 1);
            assert.strictEqual(
                beforeDetach.args[0][0].parentElement,
                child.elm,
            );
            assert.equal(beforeDetach.args[0][1].replacedWithElm, null);
            assert.strictEqual(beforeDetach.args[0][1].nested, true);
        });

        test('Exceptions in hooks do not break patch', () => {
            const boom = () => {
                throw new Error('Error!');
            };
            const child = h.div({
                children: 'div',
                hooks: {
                    afterAttach: boom,
                    beforeDetach: boom,
                    afterUpdate: boom,
                },
            });

            const vnode1 = h.div({children: child});
            const vnode2 = h.div({children: ['ok']});
            patch(div, vnode1);

            patch(vnode1, vnode2);
            assert.strictEqual(div.innerHTML, 'ok');
        });
    });

    describe('Cloned VNodes', () => {
        let div;
        beforeEach(() => {
            div = document.createElement('div');
        });

        test('Nested', () => {
            const vnode = h.div({
                tagName: 'div',
                id: 'id',
                title: 't',
                children: [h.span({children: 'span'}), 'text'],
            });
            const vnodeClone = vnode.clone();
            patch(div, h.div({children: [vnode, vnodeClone]}));

            assert.strictEqual(
                div.innerHTML,
                '<div id="id" title="t"><span>span</span>text</div>' +
                    '<div id="id" title="t"><span>span</span>text</div>',
            );
        });
    });
});
