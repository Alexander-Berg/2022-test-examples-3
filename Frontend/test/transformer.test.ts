import { createProjectSync, ts } from '@ts-morph/bootstrap';
import transformer = require('..');

const transform = (source: string): string => {
    const printer = ts.createPrinter();
    const project = createProjectSync({ useInMemoryFileSystem: true });
    const sourceFile = project.createSourceFile('index.ts', source);
    const transformResult = ts.transform(sourceFile, [transformer]);
    return printer.printFile(transformResult.transformed[0]);
};

const transpile = (source: string): string => {
    return ts.transpileModule(source, {
        transformers: {
            before: [transformer],
        },
        compilerOptions: {
            target: ts.ScriptTarget.ES5,
        },
    }).outputText;
};

describe('transformer', () => {
    it('should replace push module import with a normal one when it is the simplest case', function () {
        expect(
            transform(`
                PushModuleRenderer.pushModuleRendererImport(import("module"));
            `),
        ).toMatchSnapshot();
    });

    it('should replace push module import with a normal one when it is variable declaration', function () {
        expect(
            transform(`
                const x = PushModuleRenderer.pushModuleRendererImport(import("module"));
            `),
        ).toMatchSnapshot();
    });

    it('should replace push module import with a normal one when it is property assignment', function () {
        expect(
            transform(`
                { prop: PushModuleRenderer.pushModuleRendererImport(import("module")) };
            `),
        ).toMatchSnapshot();
    });

    it('should replace push module import with a normal one when there is multiple occurrences of it', function () {
        expect(
            transform(`
                const x = PushModuleRenderer.pushModuleRendererImport(import("module1"));
                { prop: PushModuleRenderer.pushModuleRendererImport(import("module2")) };
            `),
        ).toMatchSnapshot();
    });

    it('should not transform when it is not pushModuleRendererImport call', function () {
        expect(
            transform(`
                PushModuleRenderer.something();
            `),
        ).toMatchSnapshot();
    });

    it('should not transform when zero arguments are passed to pushModuleRendererImport', function () {
        expect(
            transform(`
                PushModuleRenderer.pushModuleRendererImport();
            `),
        ).toMatchSnapshot();
    });

    it('should not transform when more than one argument is passed to pushModuleRendererImport', function () {
        expect(
            transform(`
                PushModuleRenderer.pushModuleRendererImport(import("module"), 42);
            `),
        ).toMatchSnapshot();
    });

    it('should not transform when the thing passed to pushModuleRendererImport as an argument is not a dynamic import', function () {
        expect(
            transform(`
                PushModuleRenderer.pushModuleRendererImport(true);
            `),
        ).toMatchSnapshot();

        expect(
            transform(`
                PushModuleRenderer.pushModuleRendererImport(f("module"));
            `),
        ).toMatchSnapshot();

        expect(
            transform(`
                PushModuleRenderer.pushModuleRendererImport(import());
            `),
        ).toMatchSnapshot();

        expect(
            transform(`
                PushModuleRenderer.pushModuleRendererImport(import(identifier));
            `),
        ).toMatchSnapshot();
    });

    it('should work with transpiling', function () {
        expect(
            transpile(`
                PushModuleRenderer.pushModuleRendererImport(import("module"));
            `),
        ).toMatchSnapshot();
    });
});
