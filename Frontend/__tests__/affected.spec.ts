import { Affected } from '..';
import { lerna, FakeVcsEmpty, FakeVcsOnlyAffected, FakeVcsAffected, FakeVcsService, root, packages } from '../../__mocks__';

describe('affected: Возвращает корректный список измененных пакетов', () => {
    beforeEach(() => {
        jest.resetModules();
    });

    test('Возвращает пустые directs, dependent, если ничего не изменилось', async() => {
        const service = new FakeVcsEmpty();
        const { directs, sortedAffectedPackages } = await new Affected({
            vcs: { service },
            lerna,
            root,
        }).execute();
        expect(directs.size).toEqual(0);
        expect(sortedAffectedPackages.length).toEqual(0);
    });

    describe('npm пакет', () => {
        test('Пакет нигде не используется – Возвращаем только он сам', async() => {
            const service = new FakeVcsOnlyAffected();
            jest.mock('/root/packages/emptydeps/package.json', () => { return { name: 'empty' } }, { virtual: true });
            const { directs, sortedAffectedPackages } = await new Affected({
                vcs: { service },
                lerna,
                root,
            }).execute();
            expect(directs.size).toEqual(1);
            expect(directs).toMatchSnapshot('empty deps');
            expect(sortedAffectedPackages.length).toEqual(1);
        });
        test('Пакет используется в других местах – Возвращаем пакет и того, кто он него зависит', async() => {
            const service = new FakeVcsAffected();
            jest.mock('/root/packages/ui/package.json', () => { return packages['@yandex/ui'] }, { virtual: true });
            jest.mock('/root/services/service/package.json', () => { return packages.service }, { virtual: true });
            jest.mock('/root/packages/emptydeps/package.json', () => { return packages.empty }, { virtual: true });
            jest.mock('/root/packages/dependant/package.json', () => { return packages.empty }, { virtual: true });
            const { directs, sortedAffectedPackages } = await new Affected({
                vcs: { service },
                lerna,
                root,
            }).execute();
            expect(directs.size).toEqual(1);
            expect(directs).toMatchSnapshot('ui');
            expect(sortedAffectedPackages.length).toEqual(Object.keys(packages).length);
            expect(sortedAffectedPackages).toMatchSnapshot('ui deps');
        });
    });

    describe('Приватный пакет', () => {
        test('Возвращает только один пакет', async() => {
            const service = new FakeVcsService();
            jest.mock('/root/services/service/package.json', () => { return packages.service }, { virtual: true });
            const { directs, sortedAffectedPackages } = await new Affected({
                vcs: { service },
                lerna,
                root,
            }).execute();
            expect(directs.size).toEqual(1);
            expect(directs).toMatchSnapshot('service');
            expect(sortedAffectedPackages.length).toEqual(1);
        });

        test('Возвращает только один пакет --noPrivate ', async() => {
            const service = new FakeVcsService();
            jest.mock('/root/services/service/package.json', () => { return packages.service }, { virtual: true });
            const { directs, sortedAffectedPackages } = await new Affected({
                vcs: { service },
                lerna,
                root,
                noPrivate: true,
            }).execute();
            expect(directs.size).toEqual(1);
            expect(directs).toMatchSnapshot('service noPrivate');
            expect(sortedAffectedPackages.length).toEqual(1);
        });
    });
});
