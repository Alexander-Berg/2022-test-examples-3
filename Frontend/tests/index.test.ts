import fs from 'fs';
import { generatePackageJson } from '../packageJson';
import { generateDevexp } from '../devexp';

jest.mock('fs');

const mockFs = fs as jest.Mocked<typeof fs>;

describe('generateFile', () => {
    afterEach(() => jest.clearAllMocks());

    describe('packageJson', () => {
        it('default', () => {
            mockFs.readFileSync.mockReturnValue('{}');

            generatePackageJson(
                'pathToLeaf',
                'nameOfLeaf',
                { description: 'description', owners: 'owner1, owner2' },
            );

            expect(mockFs.readFileSync).toHaveBeenCalledWith('pathToLeaf/package.json', { encoding: 'utf-8' });

            expect(mockFs.writeFileSync.mock.calls[0]).toMatchObject([
                'pathToLeaf/package.json',
                `{
    \"name\": \"nameOfLeaf\",
    \"version\": \"1.0.0\",
    \"description\": \"description\",
    \"owners\": [
        \"owner1\",
        \"owner2\"
    ]
}`,
                {
                    encoding: 'utf-8',
                },
            ]);
        });
    });

    describe('devexp', () => {
        it('default', () => {
            mockFs.readFileSync.mockReturnValue('{}');
            mockFs.existsSync.mockReturnValue(true);

            generateDevexp('pathToLeaf');

            expect(mockFs.readFileSync).toHaveBeenCalledWith('pathToLeaf/.devexp.json', { encoding: 'utf-8' });

            expect(mockFs.writeFileSync.mock.calls[0]).toMatchObject([
                'pathToLeaf/.devexp.json',
                `{
    \"abc_members\": \"!! Заполните поле согласно документации https://github.yandex-team.ru/devexp/devexp#abc_members !!\",
    \"startrek\": \"!! Заполните поле согласно документации https://github.yandex-team.ru/devexp/devexp#startrek !!\"
}`,
                {
                    encoding: 'utf-8',
                },
            ]);
        });
    });
});
