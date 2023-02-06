import { expect } from 'chai';
import { describe, it } from 'mocha';
import postcss from 'postcss';

import postcssScopePlugin, { PostcssScopePluginOptions } from '../../src/plugins/postcss-scope-plugin';

function run(input: string, output: string, filename: string, options?: PostcssScopePluginOptions) {
    return postcss([postcssScopePlugin(options)])
        .process(input, { from: filename })
        .then(result => {
            expect(result.css).equal(output);
            expect(result.warnings()).lengthOf(0);
        });
}

describe('postcss-scope-plugin', () => {
    describe('react naming', () => {
        it('should add common platform scope for selectors', () => {
            return run(
                '.a { a: 1 } .b { b: 1 }',
                '.sb-root-scope__common .a { a: 1 } .sb-root-scope__common .b { b: 1 }',
                'a.css',
            );
        });

        it('should add desktop platform scope for selectors', () => {
            return run(
                '.a { a: 1 } .b { b: 1 }',
                '.sb-root-scope__desktop .a { a: 1 } .sb-root-scope__desktop .b { b: 1 }',
                'a@desktop.css',
            );
        });

        it('should add project and platform scope for selectors', () => {
            return run(
                '.a { a: 1 } .b { b: 1 }',
                '.sb-root-scope__common.sb-root-scope__a .a { a: 1 } .sb-root-scope__common.sb-root-scope__a .b { b: 1 }',
                'a.css',
                { scope: 'a' },
            );
        });
    });

    describe('origin naming', () => {
        it('should add common platform scope for selectors', () => {
            return run(
                '.a { a: 1 } .b { b: 1 }',
                '.sb-root-scope__common .a { a: 1 } .sb-root-scope__common .b { b: 1 }',
                'common.blocks/a/a.css',
            );
        });

        it('should add desktop platform scope for selectors', () => {
            return run(
                '.a { a: 1 } .b { b: 1 }',
                '.sb-root-scope__desktop .a { a: 1 } .sb-root-scope__desktop .b { b: 1 }',
                'desktop.blocks/a/a.css',
            );
        });

        it('should add project and platform scope for selectors', () => {
            return run(
                '.a { a: 1 } .b { b: 1 }',
                '.sb-root-scope__common.sb-root-scope__a .a { a: 1 } .sb-root-scope__common.sb-root-scope__a .b { b: 1 }',
                'common.blocks/a/a.css',
                { scope: 'a' },
            );
        });
    });

    describe('global classnames', () => {
        it('should insert scope with global classnames', () => {
            return run(
                '.a { a: 1 } .i-ua_skin_dark .b { b: 1 }',
                '.sb-root-scope__common .a { a: 1 } .i-ua_skin_dark .sb-root-scope__common .b { b: 1 }',
                'a.css',
                {
                    globalClassNames: ['i-ua_skin_dark'],
                }
            );
        });

        it('should insert scope as usual if globals are not found', () => {
            return run(
                '.a { a: 1 } .i-ua_skin_dark .b { b: 1 }',
                '.sb-root-scope__common .a { a: 1 } .sb-root-scope__common .i-ua_skin_dark .b { b: 1 }',
                'a.css',
                {
                    globalClassNames: ['i-ua_skin_dark2'],
                }
            );
        });
    });
});
