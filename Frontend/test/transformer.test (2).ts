import ts from 'typescript';
import transformerFactoryCreator from '../src/transformer';

const transformerFactory = transformerFactoryCreator();
// @ts-ignore
const transformer = transformerFactory();
const printer = ts.createPrinter({ removeComments: false });

function transform(source: string) {
    const sourceFile = ts.createSourceFile('any file name', source, 99 /* ESNext */, /*setParentNodes*/ true);
    const transformedSourceFile = transformer(sourceFile);
    return printer.printFile(transformedSourceFile);
}

describe('transformer', () => {
    describe('webpackSafePushModule', () => {
        it('should not transform an expression as a statement', function() {
            expect(transform('webpackSafePushModule(3);'))
                .toEqual('webpackSafePushModule(3);\n');
        });

        it('should transform an if statement', function() {
            expect(transform('if (webpackSafePushModule(1)) console.log(2);'))
                .toEqual('if (1 && __webpack_require__ && __webpack_require__.m && __webpack_require__.m[1])\n' +
                    '    console.log(2);\n');
        });

        it('should transform a variable declaration', function() {
            expect(transform('var x = webpackSafePushModule(2);'))
                .toEqual('var x = 2 && __webpack_require__ && __webpack_require__.m && __webpack_require__.m[2];\n');
        });
    });

    describe('pushModuleRendererImport', () => {
        it('should not transform when no import is specified', function() {
            expect(transform('obj.pushModuleRendererImport();'))
                .toEqual('obj.pushModuleRendererImport();\n');

            expect(transform('obj.pushModuleRendererImport(xxx);'))
                .toEqual('obj.pushModuleRendererImport(xxx);\n');
        });

        it('should transform a property assignment', function() {
            expect(transform('new Registry().fill({ registryComponent: obj.pushModuleRendererImport(import(path)) });'))
                .toEqual(`new Registry().fill({ registryComponent: obj.pushModuleRenderer({ id: typeof window === "object" ? require.resolveWeak(path) : require.resolve(path), client: function () {
            if (typeof window === "object") {
                return import(path);
            }
        }, server: function () {
            if (typeof window === "undefined") {
                return require(path);
            }
        } }) });
`);
        });

        it('should transform a variable declaration', function() {
            expect(transform('const Logo = pm.pushModuleRendererImport(import("./Logo/OrgHeader-Logo@touch-phone"));'))
                .toEqual(`const Logo = pm.pushModuleRenderer({ id: typeof window === "object" ? require.resolveWeak("./Logo/OrgHeader-Logo@touch-phone") : require.resolve("./Logo/OrgHeader-Logo@touch-phone"), client: function () {
        if (typeof window === "object") {
            return import("./Logo/OrgHeader-Logo@touch-phone");
        }
    }, server: function () {
        if (typeof window === "undefined") {
            return require("./Logo/OrgHeader-Logo@touch-phone");
        }
    } });
`);
        });
    });
});
