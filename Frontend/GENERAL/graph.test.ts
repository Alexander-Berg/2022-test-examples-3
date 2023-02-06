/* eslint-disable no-use-before-define */
// Unit tests for reactive-property.
import assert from 'assert';

// If using from the NPM package, this line would be
import { Graph, Package, createNode, IDGSerialized, IDGNodeType, IDGNode, IDGPackage } from './graph';

const pkg = ([name]: TemplateStringsArray): Package => createNode({ type: IDGNodeType.Package, name } as IDGPackage);

function withWeight(nodeList: string[], weight: number): IDGPackage[] & { weight: number } {
    const result = nodeList.map(node => Object.assign(pkg([node] as unknown as TemplateStringsArray), { _id: node }));
    return Object.assign(result, { weight });
}

describe('Graph', () => {
    describe('Data structure', () => {
        it('Should add nodes and list them.', () => {
            const graph = new Graph();
            graph.addNode(pkg`a`);
            graph.addNode(pkg`b`);
            assert.equal(Array.from(graph.nodes()).length, 2);
            assert(contains(graph.nodes(), 'a'));
            assert(contains(graph.nodes(), 'b'));
        });

        it('Should chain addNode.', () => {
            const graph = new Graph().addNode(pkg`a`).addNode(pkg`b`);
            assert.equal(Array.from(graph.nodes()).length, 2);
            assert(contains(graph.nodes(), 'a'));
            assert(contains(graph.nodes(), 'b'));
        });

        it('Should remove nodes.', () => {
            const graph = new Graph();
            graph.addNode(pkg`a`);
            graph.addNode(pkg`b`);
            graph.removeNode('a');
            graph.removeNode('b');
            assert.equal(Array.from(graph.nodes()).length, 0);
        });

        it('Should chain removeNode.', () => {
            const graph = new Graph()
                .addNode(pkg`a`)
                .addNode(pkg`b`)
                .removeNode('a')
                .removeNode('b');
            assert.equal(Array.from(graph.nodes()).length, 0);
        });

        it('Should add edges and query for adjacent nodes.', () => {
            const graph = new Graph();
            graph.addNode(pkg`a`);
            graph.addNode(pkg`b`);
            graph.addEdge('a', 'b');
            assert.equal(graph.adjacent('a').length, 1);
            assert.equal(graph.adjacent('a')[0].id, 'b');
        });

        it('Should implicitly add nodes when edges are added.', () => {
            const graph = new Graph();
            graph.addEdge(pkg`a`, pkg`b`);
            assert.equal(graph.adjacent('a').length, 1);
            assert.equal(graph.adjacent('a')[0].id, 'b');
            assert.equal(Array.from(graph.nodes()).length, 2);
            assert(contains(graph.nodes(), 'a'));
            assert(contains(graph.nodes(), 'b'));
        });

        it('Should chain addEdge.', () => {
            const graph = new Graph().addEdge(pkg`a`, pkg`b`);
            assert.equal(graph.adjacent('a').length, 1);
            assert.equal(graph.adjacent('a')[0].id, 'b');
        });

        it('Should remove edges.', () => {
            const graph = new Graph();
            graph.addEdge(pkg`a`, pkg`b`);
            graph.removeEdge('a', 'b');
            assert.equal(graph.adjacent('a').length, 0);
        });

        it('Should chain removeEdge.', () => {
            const graph = new Graph()
                .addEdge(pkg`a`, pkg`b`)
                .removeEdge('a', 'b');
            assert.equal(graph.adjacent('a').length, 0);
        });

        it('Should not remove nodes when edges are removed.', () => {
            const graph = new Graph();
            graph.addEdge(pkg`a`, pkg`b`);
            graph.removeEdge('a', 'b');
            assert.equal(Array.from(graph.nodes()).length, 2);
            assert(contains(graph.nodes(), 'a'));
            assert(contains(graph.nodes(), 'b'));
        });

        it('Should remove outgoing edges when a node is removed.', () => {
            const graph = new Graph();
            graph.addEdge(pkg`a`, pkg`b`);
            graph.removeNode('a');
            assert.equal(graph.adjacent('a').length, 0);
        });

        it('Should remove incoming edges when a node is removed.', () => {
            const graph = new Graph();
            graph.addEdge(pkg`a`, pkg`b`);
            graph.removeNode('b');
            assert.equal(graph.adjacent('a').length, 0);
        });

        it('Should compute indegree.', () => {
            const graph = new Graph();
            graph.addEdge(pkg`a`, pkg`b`);
            assert.equal(graph.indegree('a'), 0);
            assert.equal(graph.indegree('b'), 1);

            graph.addEdge(pkg`c`, pkg`b`);
            assert.equal(graph.indegree('b'), 2);
        });

        it('Should compute outdegree.', () => {
            const graph = new Graph();
            graph.addEdge(pkg`a`, pkg`b`);
            assert.equal(graph.outdegree('a'), 1);
            assert.equal(graph.outdegree('b'), 0);

            graph.addEdge(pkg`a`, pkg`c`);
            assert.equal(graph.outdegree('a'), 2);
        });
    });

    describe('Algorithms', () => {
        // This example is from Cormen et al. 'Introduction to Algorithms' page 550
        it('Should compute topological sort.', () => {
            const graph = new Graph();

            // Shoes depend on socks.
            // Socks need to be put on before shoes.
            graph.addEdge(pkg`socks`, pkg`shoes`);

            graph.addEdge(pkg`shirt`, pkg`belt`);
            graph.addEdge(pkg`shirt`, pkg`tie`);
            graph.addEdge(pkg`tie`, pkg`jacket`);
            graph.addEdge(pkg`belt`, pkg`jacket`);
            graph.addEdge(pkg`pants`, pkg`shoes`);
            graph.addEdge(pkg`underpants`, pkg`pants`);
            graph.addEdge(pkg`pants`, pkg`belt`);

            const sorted = graph.topologicalSort();

            assert(comesBefore(sorted, 'pants', 'shoes'));
            assert(comesBefore(sorted, 'underpants', 'pants'));
            assert(comesBefore(sorted, 'underpants', 'shoes'));
            assert(comesBefore(sorted, 'shirt', 'jacket'));
            assert(comesBefore(sorted, 'shirt', 'belt'));
            assert(comesBefore(sorted, 'belt', 'jacket'));

            assert.equal(sorted.length, 8);
        });

        it('Should compute topological sort, excluding source nodes.', () => {
            const graph = new Graph();
            graph.addEdge(pkg`a`, pkg`b`);
            graph.addEdge(pkg`b`, pkg`c`);

            const sorted = graph.topologicalSort(['a'], false);
            assert.equal(sorted.length, 2);
            assert.equal(sorted[0].id, 'b');
            assert.equal(sorted[1].id, 'c');
        });

        it('Should compute topological sort tricky case.', () => {
            const graph = new Graph(); //          a
            //                                    / \
            graph.addEdge(pkg`a`, pkg`b`); //    b   |
            graph.addEdge(pkg`a`, pkg`d`); //    |   d
            graph.addEdge(pkg`b`, pkg`c`); //    c   |
            graph.addEdge(pkg`d`, pkg`e`); //     \ /
            graph.addEdge(pkg`c`, pkg`e`); //      e

            const sorted = graph.topologicalSort(['a'], false);
            assert.equal(sorted.length, 4);
            assert(contains(sorted, 'b'));
            assert(contains(sorted, 'c'));
            assert(contains(sorted, 'd'));
            assert.equal(sorted[sorted.length - 1].id, 'e');

            assert(comesBefore(sorted, 'b', 'c'));
            assert(comesBefore(sorted, 'b', 'e'));
            assert(comesBefore(sorted, 'c', 'e'));
            assert(comesBefore(sorted, 'd', 'e'));
        });

        it('Should exclude source nodes with a cycle.', () => {
            const graph = new Graph()
                .addEdge(pkg`a`, pkg`b`)
                .addEdge(pkg`b`, pkg`c`)
                .addEdge(pkg`c`, pkg`a`);
            const sorted = graph.topologicalSort(['a'], false);
            assert.equal(sorted.length, 2);
            assert.equal(sorted[0].id, 'b');
            assert.equal(sorted[1].id, 'c');
        });

        it('Should exclude source nodes with multiple cycles.', () => {
            const graph = new Graph()

                .addEdge(pkg`a`, pkg`b`)
                .addEdge(pkg`b`, pkg`a`)

                .addEdge(pkg`b`, pkg`c`)
                .addEdge(pkg`c`, pkg`b`)

                .addEdge(pkg`a`, pkg`c`)
                .addEdge(pkg`c`, pkg`a`);

            const sorted = graph.topologicalSort(['a', 'b'], false);
            assert(!contains(sorted, 'b'));
        });

        it('Should compute lowest common ancestors.', () => {
            const graph = new Graph()
                .addEdge(pkg`a`, pkg`b`)
                .addEdge(pkg`b`, pkg`d`)
                .addEdge(pkg`c`, pkg`d`)
                .addEdge(pkg`b`, pkg`e`)
                .addEdge(pkg`c`, pkg`e`)
                .addEdge(pkg`d`, pkg`g`)
                .addEdge(pkg`e`, pkg`g`)
                .addNode(pkg`f`);

            assert.deepStrictEqual(graph.lowestCommonAncestors('a', 'a').map(n => n.id), ['a']);
            assert.deepStrictEqual(graph.lowestCommonAncestors('a', 'b').map(n => n.id), ['b']);
            assert.deepStrictEqual(graph.lowestCommonAncestors('a', 'c').map(n => n.id), ['d', 'e']);
            assert.deepStrictEqual(graph.lowestCommonAncestors('a', 'f').map(n => n.id), []);
        });
    });

    describe('Edge cases and error handling', () => {
        it('Should return empty array of adjacent nodes for unknown nodes.', () => {
            const graph = new Graph();
            assert.equal(graph.adjacent('a').length, 0);
            assert.notStrictEqual(Array.from(graph.nodes()), []);
        });

        it('Should do nothing if removing an edge that does not exist.', () => {
            assert.doesNotThrow(() => {
                const graph = new Graph();
                graph.removeEdge('a', 'b');
            });
        });

        it('Should return indegree of 0 for unknown nodes.', () => {
            const graph = new Graph();
            assert.equal(graph.indegree('z'), 0);
        });

        it('Should return outdegree of 0 for unknown nodes.', () => {
            const graph = new Graph();
            assert.equal(graph.outdegree('z'), 0);
        });
    });

    describe('Serialization', () => {
        let serialized: IDGSerialized;

        before(() => {
            const graph = new Graph()
                .addEdge(pkg`a`, pkg`b`)
                .addEdge(pkg`b`, pkg`c`);
            serialized = graph.serialize();
        });

        const checkSerialized = (serialized: IDGSerialized) => {
            assert.equal(serialized.nodes.length, 3);
            assert.equal(serialized.links.length, 2);

            assert.equal(serialized.nodes[0].id, 'a');
            assert.equal(serialized.nodes[1].id, 'b');
            assert.equal(serialized.nodes[2].id, 'c');

            assert.equal(serialized.links[0].source, 'a');
            assert.equal(serialized.links[0].target, 'b');
            assert.equal(serialized.links[1].source, 'b');
            assert.equal(serialized.links[1].target, 'c');
        };

        it('Should serialize a graph.', () => {
            checkSerialized(serialized);
        });

        it('Should deserialize a graph passed to constructor.', () => {
            const graph = new Graph(serialized);
            checkSerialized(graph.serialize());
        });
    });

    describe('Edge Weights', () => {
        it('Should set and get an edge weight.', () => {
            const graph = new Graph().addEdge(pkg`a`, pkg`b`, { weight: 5 });
            assert.equal(graph.getEdgeMeta('a', 'b').weight, 5);
        });

        it('Should set edge weight via EdgeMeta object.', () => {
            const graph = new Graph()
                .addEdge(pkg`a`, pkg`b`);
            graph.getEdgeMeta('a', 'b').weight = 5;
            assert.equal(graph.getEdgeMeta('a', 'b').weight, 5);
        });

        it('Should return weight of 1 if no weight set.', () => {
            const graph = new Graph().addEdge(pkg`a`, pkg`b`);
            assert.equal(graph.getEdgeMeta('a', 'b').weight, 1);
        });

        it('Should set and get an edge rank.', () => {
            const graph = new Graph()
                .addEdge(pkg`a`, pkg`b`, { rank: 5 });
            assert.equal(graph.getEdgeMeta('a', 'b').rank, 5);
        });

        it('Should return rank of 1 if no rank set.', () => {
            const graph = new Graph().addEdge(pkg`a`, pkg`b`);
            assert.equal(graph.getEdgeMeta('a', 'b').rank, 1);
        });
    });

    describe('Dijkstra\'s Shortest Path Algorithm', () => {
        it('Should compute shortest path on a single edge.', () => {
            const graph = new Graph().addEdge(pkg`a`, pkg`b`);
            assert.deepEqual(graph.shortestPath('a', 'b'), withWeight(['a', 'b'], 1));
        });

        it('Should compute shortest path on two edges.', () => {
            const graph = new Graph()
                .addEdge(pkg`a`, pkg`b`)
                .addEdge(pkg`b`, pkg`c`);
            assert.deepEqual(graph.shortestPath('a', 'c'), withWeight(['a', 'b', 'c'], 2));
        });

        it('Should compute shortest path on example from Cormen text (p. 659).', () => {
            const graph = new Graph()
                .addEdge(pkg`s`, pkg`t`, { weight: 10 })
                .addEdge(pkg`s`, pkg`y`, { weight: 5 })
                .addEdge(pkg`t`, pkg`y`, { weight: 2 })
                .addEdge(pkg`y`, pkg`t`, { weight: 3 })
                .addEdge(pkg`t`, pkg`x`, { weight: 1 })
                .addEdge(pkg`y`, pkg`x`, { weight: 9 })
                .addEdge(pkg`y`, pkg`z`, { weight: 2 })
                .addEdge(pkg`x`, pkg`z`, { weight: 4 })
                .addEdge(pkg`z`, pkg`x`, { weight: 6 });

            assert.deepEqual(graph.shortestPath('s', 'z'), withWeight(['s', 'y', 'z'], 5 + 2));
            assert.deepEqual(graph.shortestPath('s', 'x'), withWeight(['s', 'y', 't', 'x'], 5 + 3 + 1));
        });

        it('Should throw error if source node not in graph.', () => {
            const graph = new Graph().addEdge(pkg`b`, pkg`c`);
            assert.throws(() => graph.shortestPath('a', 'c'), /Node/);
        });

        it('Should throw error if dest node not in graph.', () => {
            const graph = new Graph().addEdge(pkg`b`, pkg`c`);
            assert.throws(() => graph.shortestPath('b', 'g'), /Node/);
        });

        it('Should throw error if no path exists.', () => {
            const graph = new Graph()
                .addEdge(pkg`a`, pkg`b`)
                .addEdge(pkg`d`, pkg`e`);
            assert.throws(() => graph.shortestPath('a', 'e'), /No path/);
        });

        it('Should be robust to disconnected subgraphs.', () => {
            const graph = new Graph()
                .addEdge(pkg`a`, pkg`b`)
                .addEdge(pkg`b`, pkg`c`)
                .addEdge(pkg`d`, pkg`e`);
            assert.deepEqual(graph.shortestPath('a', 'c'), withWeight(['a', 'b', 'c'], 2));
        });
    });
});

function contains(arr: Iterable<IDGNode>, item: string) {
    return Array.from(arr).find(node => node.id === item) !== undefined;
}

function comesBefore(arr: IDGNode[], a: string, b: string) {
    let aIndex: number = Infinity;
    let bIndex: number = 0;
    arr.forEach((node, i) => {
        if (node.id === a) { aIndex = i }
        if (node.id === b) { bIndex = i }
    });
    return aIndex < bIndex;
}
