import {
    getExpandedAccountMock,
    getExpandedAccountsMock,
    getExpandedFolderMock,
    getExpandedProviderMock,
    getExpandedResourceMock,
    getExpandedResourcesMock,
    getExpandedResourceTypeMock,
    getExpandedResourceTypesMock,
    getFoldersResponseMock,
} from '../Folders.mock';
import {
    updateAccounts,
    updateResources,
    updateResourceTypes,
    updateProvider,
    updateFolders,
} from './Folders.reducer';

describe('Folders.reducer', () => {
    it('should add resource', () => {
        // для начала берём рандомные моки ресурсов
        const origin = getExpandedResourcesMock();
        const upstream = getExpandedResourcesMock();

        const updated = updateResources(origin, upstream);

        // при обновлени ресурс должен быть добавлен
        // проверяем ссылки на соответствующие ресурсы
        expect(updated[0]).toBe(origin[0]);
        expect(updated[1]).toBe(upstream[0]);
    });

    it('should update resources', () => {
        // для начала берём рандомные моки ресурсов
        // в origin - два ресурса
        const origin = getExpandedResourcesMock(2);
        const upstream = getExpandedResourcesMock();

        // чтобы проверить логику обновления,
        // выставляем одинаковый id ресурса
        upstream[0].resourceId = origin[0].resourceId;

        const updated = updateResources(origin, upstream);

        // проверяем ссылки на соответствующие ресурсы
        expect(updated[1]).toBe(origin[1]);
        expect(updated[0]).toBe(upstream[0]);
    });

    it('should update accounts', () => {
        // для начала берём рандомные моки аккаунтов
        // в origin - два аккаунта
        const origin = getExpandedAccountsMock(2);
        const upstream = getExpandedAccountsMock();

        // выставляем одинаковый id аккаунта, ресурса в аккаунте
        upstream[0].account.id = origin[0].account.id;
        upstream[0].resources[0].resourceId = origin[0].resources[0].resourceId;

        const updated = updateAccounts(origin, upstream);

        // второй аккаунт остался не тронутым
        expect(updated[1]).toBe(origin[1]);
        // обновляется ресурс в первом аккаунте
        // выставлен в соответствии
        expect(updated[0].resources[0]).toBe(upstream[0].resources[0]);
    });

    it('should add resource types', () => {
        // для начала берём рандомные моки типов ресурсов
        const origin = getExpandedResourceTypesMock();
        const upstream = getExpandedResourceTypesMock();

        const updated = updateResourceTypes(origin, upstream);

        // тип ресурса должен быть добавлен
        expect(updated[0]).toBe(origin[0]);
        expect(updated[1]).toBe(upstream[0]);
    });

    it('should update resource types', () => {
        // для начала берём рандомные моки типов ресурсов
        // в origin - два типа ресурсов
        const origin = getExpandedResourceTypesMock(2);
        const upstream = getExpandedResourceTypesMock();

        // выставляем одинаковый id типа, ресурса в этом типе
        upstream[0].resourceTypeId = origin[0].resourceTypeId;
        upstream[0].resources[0].resourceId = origin[0].resources[0].resourceId;

        const updated = updateResourceTypes(origin, upstream);

        // второй тип остался не тронутым
        expect(updated[1]).toBe(origin[1]);

        // в первом типе ресурсов, обновился первый ресурс
        expect(updated[0].resources[0]).toBe(upstream[0].resources[0]);
        // в первом типе ресурсов, обновилась информация о суммах ресурсов в типе
        expect(updated[0].sums).toBe(upstream[0].sums);
    });

    it('should add provider to folder', () => {
        // для начала берём рандомный мок фолдера и мок провайдера
        const origin = getFoldersResponseMock();
        const upstream = getExpandedProviderMock();

        const folder = origin.folders[0];

        const meta = {
            serviceId: origin.folders[0].folder.serviceId,
            folderId: origin.folders[0].folder.id,
            id: '0',
            needDictionary: true,
        };

        const updated = updateFolders(origin, upstream, meta);

        // проверям что данные остаться при мердже
        expect(updated.folders[0].providers[0]).toBe(folder.providers[0]);

        // проверяем что провайдер был добавлен
        expect(updated.folders[0].providers[1]).toBe(upstream);
    });

    it('should update provider', () => {
        // для начала берём рандомные моки провайдеров
        // в origin - два провайдера
        const origin = getExpandedProviderMock();
        const upstream = getExpandedProviderMock();

        // чтобы проверить правильность мерджа,
        // в оригинальные данные добавим ещё ресурсов, типов ресурсов
        origin.accounts[0].resources.push(getExpandedResourceMock());
        origin.resourceTypes.push(getExpandedResourceTypeMock());

        // в данных для обновления выставляем в соответствие
        // id провайдера, id аккаунта, id ресурса
        upstream.providerId = origin.providerId;
        upstream.accounts[0].account.id = origin.accounts[0].account.id;
        upstream.accounts[0].resources[0].resourceId = origin.accounts[0].resources[0].resourceId;
        // а также id типа ресурса, id ресурса в этом типе
        upstream.resourceTypes[0].resourceTypeId = origin.resourceTypes[0].resourceTypeId;
        upstream.resourceTypes[0].resources[0].resourceId = origin.resourceTypes[0].resources[0].resourceId;

        const updated = updateProvider(origin, upstream);

        // второй аккаунт остался не тронутым
        expect(updated.accounts[1]).toBe(origin.accounts[1]);
        // второй ресурс в первом аккаунте остался не тронутым
        expect(updated.accounts[0].resources[1]).toBe(origin.accounts[0].resources[1]);
        // второй тип ресурса остался не тронутым
        expect(updated.resourceTypes[1]).toBe(origin.resourceTypes[1]);

        // ресурс в первом аккаунте обновился
        expect(updated.accounts[0].resources[0]).toBe(upstream.accounts[0].resources[0]);
        // первый тип ресурса обновился
        expect(updated.resourceTypes[0].resources[0]).toBe(upstream.resourceTypes[0].resources[0]);
    });

    it('should update folders', () => {
        // для начала берём рандомный мок фолдера и мок провайдера
        const origin = getFoldersResponseMock();
        const upstream = getExpandedProviderMock();

        const folder = origin.folders[0];
        const provider = folder.providers[0];
        const account = provider.accounts[0];
        const resourceType = provider.resourceTypes[0];

        // чтобы проверить правильность обновления,
        // в оригинальные данные добавим ещё
        // фолдер, провайдера, аккаунт, тип ресурса, ресурс в аккаунт
        origin.folders.push(getExpandedFolderMock());
        folder.providers.push(getExpandedProviderMock());
        provider.accounts.push(getExpandedAccountMock());
        provider.resourceTypes.push(getExpandedResourceTypeMock());
        account.resources.push(getExpandedResourceMock());

        // выставим соостветствующие данные для обновления
        upstream.providerId = provider.providerId;
        upstream.accounts[0].account.id = account.account.id;
        upstream.accounts[0].resources[0].resourceId = account.resources[0].resourceId;
        upstream.resourceTypes[0].resourceTypeId = resourceType.resourceTypeId;
        upstream.resourceTypes[0].resources[0].resourceId = resourceType.resources[0].resourceId;

        const meta = {
            serviceId: folder.folder.serviceId,
            folderId: folder.folder.id,
            id: '0',
            needDictionary: true,
        };

        const updated = updateFolders(origin, upstream, meta);

        // проверям что данные, которые не должны обновиться - остались не тронуты
        expect(updated.folders[1]).toBe(origin.folders[1]);
        expect(updated.folders[0].providers[1]).toBe(folder.providers[1]);
        expect(updated.folders[0].providers[0].accounts[1]).toBe(provider.accounts[1]);
        expect(updated.folders[0].providers[0].accounts[0].resources[1]).toBe(account.resources[1]);
        expect(updated.folders[0].providers[0].resourceTypes[1]).toBe(provider.resourceTypes[1]);

        // а то что должно было обновиться - обновилось
        expect(
            updated.folders[0].providers[0].accounts[0].resources[0],
        ).toBe(
            upstream.accounts[0].resources[0],
        );

        expect(
            updated.folders[0].providers[0].resourceTypes[0].resources[0],
        ).toBe(
            upstream.resourceTypes[0].resources[0],
        );

        expect(
            updated.folders[0].providers[0].resourceTypes[0].sums,
        ).toBe(
            upstream.resourceTypes[0].sums,
        );
    });
});
