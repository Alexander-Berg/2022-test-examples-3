import { namedTypes } from 'ast-types';
import * as recast from 'recast';

export function mkAST(): namedTypes.Program {
    const b = recast.types.builders;
    const firstItCallExpression = b.callExpression(
        b.memberExpression(b.identifier('h'), b.identifier('it')),
        [b.literal('P-5'), b.functionExpression(null, [], b.blockStatement([]))],
    );
    const secondItCallExpression = b.callExpression(
        b.memberExpression(b.identifier('h'), b.identifier('it')),
        [b.literal('P-8'), b.functionExpression(null, [], b.blockStatement([]))],
    );
    const describeCallExpression = b.callExpression(b.identifier('describe'), [
        b.literal('P-4'),
        b.functionExpression(
            null,
            [],
            b.blockStatement([
                b.expressionStatement(firstItCallExpression),
                b.expressionStatement(secondItCallExpression),
            ]),
        ),
    ]);
    return b.program([
        b.expressionStatement(
            b.callExpression(b.identifier('specs'), [
                b.objectExpression([
                    b.property('init', b.identifier('feature'), b.literal('F-1')),
                    b.property('init', b.identifier('experiment'), b.literal('E-3')),
                ]),
                b.functionExpression(
                    null,
                    [],
                    b.blockStatement([b.expressionStatement(describeCallExpression)]),
                ),
            ]),
        ),
    ]);
}
