import { join, resolve } from 'path';

import { TSAdapter } from './ts-adapter';

describe('TSAdapter', () => {
    const src = 'src';
    const filePath = resolve(src, 'Button/Button.tsx');

    // eslint-disable-next-line
    let instance:any;

    beforeEach(() => {
        instance = new TSAdapter({ src });
        instance.getFilesList = () => [
            'Button/Button.tsx',
            'Link/Link.css',
            'Link/Link@desktop.tsx',
            'utils/util.js',
        ].map(name => resolve(src, name));
        instance.matcher._existsSync = () => true;
        instance.whiteList = ['package.json'];
    });

    describe('normalizePath()', () => {
        it('should normalize file path', () => {
            expect(instance.normalizePath(src, filePath)).toBe('Button/Button.tsx');
        });
    });

    describe('getImports()', () => {
        // Возможно, это не является ожидаемым поведением,
        // но тесты не помешает для прозрачости.
        it('should ignore require', () => {
            const content = 'const fs = require("fs");\nconsole.log("Hello world");';
            const filesData = [{ content, filePath }];
            const expected = [{ imports: [], filePath }];
            const actual = instance.getImports(filesData);

            expect(actual).toEqual(expected);
        });

        it('should resolve local imports', () => {
            const content = 'import { Button } from "../Button";\n' +
                'import Link from "@yandex-lego/components/Link";';
            const filesData = [{ content, filePath }];
            const expected = [{
                imports: ['src/Button', '@yandex-lego/components/Link'],
                filePath,
            }];
            const actual = instance.getImports(filesData);

            expect(actual).toEqual(expected);
        });
    });

    describe('getAllComponents()', () => {
        it('should get all unique components list', async() => {
            const allComponents = await instance.getAllComponents('', src);
            const expected = [
                ['Button@common.tsx', {
                    entity: { block: 'Button' },
                    layer: 'common',
                    tech: 'tsx',
                }],
                ['Link@common.css', {
                    entity: { block: 'Link' },
                    layer: 'common',
                    tech: 'css',
                }],
                ['Link@desktop.tsx', {
                    entity: { block: 'Link' },
                    layer: 'desktop',
                    tech: 'tsx',
                }]];
            const actual = [...allComponents.entries()].map(([key, value]) => [key, value.valueOf()]);

            expect(actual).toEqual(expected);
        });
    });

    describe('run', () => {
        beforeEach(() => {
            const filesData = [
                { filePath: 'Button/Button.bundle/desktop.ts', content: 'import { Button } from "../Button"' },
                { filePath: 'Button/Button.tsx', content: '' },
                { filePath: 'Button/Button.css', content: '' },
                { filePath: 'Link/Link.css', content: '' },
                { filePath: 'Link/Link.tsx', content: 'import "../Link.css"' },
                { filePath: 'utils/util.js', content: '' },
                { filePath: 'Select/Select@desktop.tsx', content: 'import { Button } from "../Button/Button.bundle/desktop"' },
            ].map(fileData => {
                const { filePath, content } = fileData;
                return { filePath: join(src, filePath), content: content };
            });
            instance.getFilesList = () => filesData.map(fileData => fileData.filePath);
            instance.getContents = () => filesData;
        });

        it('white list', async() => {
            instance.getChangedFiles = () => ['package.json', 'src/Button/Button.tsx'];

            const expected = {
                directlyAffected: [
                    { entity: { block: 'Button' }, layer: 'common', tech: 'bundle' },
                    { entity: { block: 'Button' }, layer: 'common', tech: 'tsx' },
                    { entity: { block: 'Button' }, layer: 'common', tech: 'css' },
                    { entity: { block: 'Link' }, layer: 'common', tech: 'css' },
                    { entity: { block: 'Link' }, layer: 'common', tech: 'tsx' },
                    { entity: { block: 'Select' }, layer: 'desktop', tech: 'tsx' },
                ],
                plainBlocksList: ['Button', 'Link', 'Select'],
                affectedDependents: [],
            };
            const actual = await instance.run();

            expect(actual).toEqual(expected);
        });

        it('ordinary changes', async() => {
            instance.getChangedFiles = () => ['src/Button/Button.tsx'];
            const expected = {
                directlyAffected: [{ entity: { block: 'Button' }, layer: 'common', tech: 'tsx' }],
                plainBlocksList: ['Select', 'Button'],
                affectedDependents: [
                    { entity: { block: 'Select' }, layer: 'desktop', tech: 'tsx' },
                    { entity: { block: 'Button' }, layer: 'common', tech: 'bundle' },
                ],
            };
            const actual = await instance.run();

            expect(actual).toEqual(expected);
        });

        it('no dependents', async() => {
            instance.getChangedFiles = () => ['src/Link/Link.css'];
            const expected = {
                directlyAffected: [
                    { entity: { block: 'Link' }, layer: 'common', tech: 'css' },
                ],
                affectedDependents: [],
                plainBlocksList: ['Link'],
            };
            const actual = await instance.run();

            expect(actual).toEqual(expected);
        });
    });

    describe('getExtention', () => {
        beforeEach(() => {
            const filesData = [
                { filePath: 'Button/Button.tsx', content: '' },
            ].map(fileData => {
                const { filePath, content } = fileData;
                return { filePath: join(src, filePath), content: content };
            });
            instance.getFilesList = () => filesData.map(fileData => fileData.filePath);
            instance.getContents = () => filesData;
        });

        it('should get correct extention for existing files', () => {
            const extention = instance.getExtention(instance.getFilesList(), 'src/Button/Button', ['js', 'ts', 'tsx']);
            expect(extention).toEqual('tsx');
        });

        it('should skip non-existent files', () => {
            const extention = instance.getExtention(instance.getFilesList(), 'src/Link/Link', ['js', 'ts', 'tsx']);
            expect(extention).toEqual(undefined);
        });

        it('should skip external dependencies', () => {
            const extention = instance.getExtention(instance.getFilesList(), 'react', ['js', 'ts', 'tsx']);
            expect(extention).toEqual(undefined);
        });
    });
});
