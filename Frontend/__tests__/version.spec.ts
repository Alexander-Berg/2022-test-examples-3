import { SeverityEnum, Version } from '..';
import { Lerna } from '../..';
import { Affected } from '../../affected';
import { FakeVcsEmpty, root, FakeVcsOnlyAffected, packages, ChoreVcsService, PatchVcsService, MinorVcsService, MajorVcsService } from '../../__mocks__';

let lerna: Lerna;

describe('version: Правильно вычисляет измененные версии', () => {
    beforeEach(() => {
        lerna = require('../../__mocks__').lerna;
        jest.mock('/root/packages/ui/package.json', () => { return packages['@yandex/ui'] }, { virtual: true });
        jest.mock('/root/services/service/package.json', () => { return packages.service }, { virtual: true });
        jest.mock('/root/packages/emptydeps/package.json', () => { return packages.empty }, { virtual: true });
        jest.mock('/root/packages/dependant/package.json', () => { return packages.dependant }, { virtual: true });

        jest.resetModules();
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('Не меняет версию пакета', () => {
        test('TRIVIAL', async() => {
            const service = new FakeVcsEmpty();
            const affected = await new Affected({
                vcs: { service },
                lerna,
                root,
            }).execute();
            const cmd = new Version(affected);
            const versions = await cmd.getVersions();

            expect(versions.size).toEqual(0);
        });

        test('В пакете указан autobumpversion === false', async() => {
            const service = new FakeVcsOnlyAffected();

            const affected = await new Affected({
                vcs: { service },
                lerna,
                root,
            }).execute();
            const cmd = new Version(affected);
            const versions = await cmd.getVersions();

            expect(versions.size).toEqual(1);
            const version = versions.get('empty');
            expect(version).not.toBeNull();
            expect(version?.severity).toEqual(SeverityEnum.NONE);
        });

        test('chore: коммиты', async() => {
            const service = new ChoreVcsService();

            const affected = await new Affected({
                vcs: { service },
                lerna,
                root,
            }).execute();
            const cmd = new Version(affected);
            const versions = await cmd.getVersions();

            expect(versions.size).toEqual(Object.keys(packages).length);
            const version = versions.get('@yandex/ui');
            expect(version).not.toBeNull();
            expect(version?.severity).toEqual(SeverityEnum.NONE);
        });

        test('Обновилась только зависимость сервиса', async() => {
            const vcs = new PatchVcsService();

            const affected = await new Affected({
                vcs: { service: vcs },
                lerna,
                root,
            }).execute();
            const cmd = new Version(affected, { dryRun: true });
            cmd.strategy.exec = () => { return Promise.resolve('["1.0.0"]') };
            const versions = await cmd.getVersions();

            expect(versions.size).toEqual(Object.keys(packages).length);

            const service = versions.get('service');
            expect(service).not.toBeNull();
            expect(service?.severity).toEqual(SeverityEnum.NONE);
            expect(service?.deps.size).toEqual(1);

            expect(service?.deps).toMatchSnapshot('service deps ui');
        });
    });

    describe('Меняет версию пакета', () => {
        test('patch: коммиты', async() => {
            const service = new PatchVcsService();

            const affected = await new Affected({
                vcs: { service },
                lerna,
                root,
            }).execute();
            const cmd = new Version(affected, { dryRun: true });
            cmd.strategy.exec = () => { return Promise.resolve('["1.0.0"]') };
            const versions = await cmd.getVersions();

            expect(versions.size).toEqual(Object.keys(packages).length);

            const ui = versions.get('@yandex/ui');
            expect(ui).not.toBeNull();
            expect(ui?.severity).toEqual(SeverityEnum.PATCH);
            expect(ui?.version).toEqual('1.0.1');
        });

        test('minor: коммиты', async() => {
            const service = new MinorVcsService();

            const affected = await new Affected({
                vcs: { service },
                lerna,
                root,
            }).execute();
            const cmd = new Version(affected, { dryRun: true });
            cmd.strategy.exec = () => { return Promise.resolve('["1.0.0"]') };
            const versions = await cmd.getVersions();

            expect(versions.size).toEqual(Object.keys(packages).length);

            const ui = versions.get('@yandex/ui');
            expect(ui).not.toBeNull();
            expect(ui?.severity).toEqual(SeverityEnum.MINOR);
            expect(ui?.version).toEqual('1.1.0');
        });

        test('major: коммиты', async() => {
            const service = new MajorVcsService();

            const affected = await new Affected({
                vcs: { service },
                lerna,
                root,
            }).execute();
            const cmd = new Version(affected, { dryRun: true });
            cmd.strategy.exec = () => { return Promise.resolve('["1.0.0"]') };
            const versions = await cmd.getVersions();

            expect(versions.size).toEqual(Object.keys(packages).length);

            const ui = versions.get('@yandex/ui');
            expect(ui).not.toBeNull();
            expect(ui?.severity).toEqual(SeverityEnum.MAJOR);
            expect(ui?.version).toEqual('2.0.0');
        });

        test('Обновилась только зависимость пакета', async() => {
            const vcs = new PatchVcsService();

            const affected = await new Affected({
                vcs: { service: vcs },
                lerna,
                root,
            }).execute();
            const cmd = new Version(affected, { dryRun: true });
            cmd.strategy.exec = () => { return Promise.resolve('["1.0.0"]') };
            const versions = await cmd.getVersions();

            expect(versions.size).toEqual(Object.keys(packages).length);

            const dependant = versions.get('dependant');

            expect(dependant).not.toBeNull();
            expect(dependant?.severity).toEqual(SeverityEnum.PATCH);
            expect(dependant?.deps.size).toEqual(1);
            expect(dependant?.version).toEqual('1.1.1');

            expect(dependant?.deps).toMatchSnapshot('dependant deps ui');
        });
    });
});
