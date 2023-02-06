import AmpersandCollection from 'ampersand-collection';

import ConcatCollection from './ConcatCollection.js';

test('Empty contructor', () => {
    new ConcatCollection();
});

test('Initial arguments', () => {
    const c1 = new AmpersandCollection([{id: 1}, {id: 2}]);
    const c2 = new AmpersandCollection([{id: 3}, {id: 4}]);

    const res = new ConcatCollection(null, {sources: [c1, c2]});

    expect(res.map(m => m.id)).toEqual([1, 2, 3, 4]);
});

test('Track changes', () => {
    const c1 = new AmpersandCollection([{id: 1}, {id: 2}]);
    const c2 = new AmpersandCollection([{id: 3}, {id: 4}]);
    const res = new ConcatCollection(null, {sources: [c1, c2]});

    c1.add({id: 5});
    c2.remove(4);

    expect(res.map(m => m.id)).toEqual([1, 2, 5, 3]);
});

test('Pass models by reference', () => {
    const c1 = new AmpersandCollection([{id: 1}]);

    const res = new ConcatCollection(null, {sources: [c1]});

    expect(res.models[0]).toBe(c1.models[0]);
});

test('Set new sources', () => {
    const c1 = new AmpersandCollection([{id: 1}, {id: 2}]);
    const c2 = new AmpersandCollection([{id: 3}, {id: 4}]);
    const c3 = new AmpersandCollection([{id: 5}, {id: 6}]);

    const res = new ConcatCollection(null, {sources: [c1, c2]});
    res.setSources([c2, c3]);

    expect(res.map(m => m.id)).toEqual([3, 4, 5, 6]);
});
