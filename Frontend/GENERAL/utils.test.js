const isDependenciesChanged = require('./utils.js').isDependenciesChanged;

describe('isDependenciesChanged', () => {
    let originalPackageJsonMock = {
        dependencies: {
            '@bem/sdk.naming.entity.stringify': '1.0.1',
            '@bem/sdk.naming.presets': '0.0.7',
            inherit: '2.2.6',
            lodash: '3.10.0',
        },
        devDependencies: {
            chai: '4.2.0',
            mocha: '5.2.0',
        },
    };

    it('Изменились поля не зависимостей (dependencies, devDependencies)', () => {
        const changed = {
            ...originalPackageJsonMock,
            scripts: {
                lint: '123'
            },
        };

        expect(isDependenciesChanged(originalPackageJsonMock, changed)).toBe(false);
    });

    it('Изменили зависимость в dependencies', () => {
        const changed = {
            ...originalPackageJsonMock,
            dependencies: {
                ...originalPackageJsonMock.dependencies,
                lodash: '3.10.100'
            },
        };

        expect(isDependenciesChanged(originalPackageJsonMock, changed)).toBe(true);
    });

    it('Удалили зависимость в devDependencies', () => {
        const devDependencies = { ...originalPackageJsonMock.devDependencies };
        delete devDependencies.chai;

        const changed = {
            ...originalPackageJsonMock,
            devDependencies,
        };

        expect(isDependenciesChanged(originalPackageJsonMock, changed)).toBe(true);
    });

    it('Добавили зависимость в devDependencies', () => {
        const changed = {
            ...originalPackageJsonMock,
            devDependencies: {
                ...originalPackageJsonMock.devDependencies,
                newDep: '1.2.3',
            },
        };

        expect(isDependenciesChanged(originalPackageJsonMock, changed)).toBe(true);
    });

    it('Удалили devDependencies', () => {
        const changed = {
            ...originalPackageJsonMock,
        };
        delete changed.devDependencies;

        expect(isDependenciesChanged(originalPackageJsonMock, changed)).toBe(true);
    });
});
