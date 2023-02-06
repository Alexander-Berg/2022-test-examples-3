import { IGranularPermissions } from '~/src/common/redux/common.types';
import { Person } from '~/src/features/Duty/redux/DutyShifts.types';
import { Service } from '~/src/common/types/Service';
import { IUser, RawUser } from '~/src/features/ServiceCreation/redux/types/requests';
import { RawService } from '~/src/features/Service/redux/types/requests';
import { AbcContextShape } from '~/src/abc/react/context/types';

export const getOwnOnlyViewerPermissions = (): IGranularPermissions => ({
    can_view: true,
    view_description: true,
    view_own_services: true,
    view_team: true,
});

export const getServicesViewerPermissions = (): IGranularPermissions => ({
    ...getOwnOnlyViewerPermissions(),
    can_filter_and_click: true,
    view_all_services: true,
    view_duty: true,
    view_hierarchy: true,
});

export const getFullAccessPermissions = (): IGranularPermissions => ({
    ...getServicesViewerPermissions(),
    can_edit: true,
    can_export: true,
    view_activity: true,
    view_contacts: true,
    view_department: true,
    view_details: true,
    view_hardware: true,
    view_kpi: true,
    view_resources: true,
    view_tags: true,
    view_traffic_light: true,
});

// @ts-expect-error ABC-11164
export const getPersonMock = (data?: Partial<Person>): Person => ({
    id: 1,
    login: 'johndoe',
    name: {
        ru: 'Вася Пупкин',
        en: 'John Doe',
    },
    vteams: [],
    ...data,
});

export const getUserMock = (data?: Partial<IUser>): IUser => ({
    id: 1,
    uid: '1234567890',
    login: 'johndoe',
    name: {
        ru: 'Вася Пупкин',
        en: 'John Doe',
    },
    affiliation: 'yandex',
    department: 3593,
    isDismissed: false,
    isRobot: false,
    firstName: {
        ru: 'Вася',
        en: 'John',
    },
    lastName: {
        ru: 'Пупкин',
        en: 'Doe',
    },
    ...data,
});

export const getRawUserMock = (data?: Partial<RawUser>): RawUser => ({
    id: 1,
    uid: '1234567890',
    login: 'johndoe',
    name: {
        ru: 'Вася Пупкин',
        en: 'John Doe',
    },
    affiliation: 'yandex',
    department: 3593,
    is_dismissed: false,
    is_robot: false,
    first_name: {
        ru: 'Вася',
        en: 'John',
    },
    last_name: {
        ru: 'Пупкин',
        en: 'Doe',
    },
    ...data,
});

export const getServiceMock = (id: number, data?: Partial<Service>): Service => ({
    id,
    slug: 'mockservice',
    name: {
        ru: 'Моковый сервис',
        en: 'Mock Service',
    },
    ...data,
});

export const getRawServiceMock = (id: number, data?: Partial<RawService>): RawService => ({
    id,
    slug: 'mockservice',
    name: {
        ru: 'Моковый сервис',
        en: 'Mock Service',
    },
    ...data,
});

export const getAbcContextMock = (data?: Partial<AbcContextShape>): AbcContextShape => ({
    configs: {
        hosts: {
            tracker: { protocol: 'https:', hostname: 'tracker.mock' },
        }
    },
    user: {
        id: 1,
        uid: '1234567890',
        login: 'johndoe',
        name: {
            ru: 'Вася Пупкин',
            en: 'John Doe',
        },
        department: {
            id: 3593,
            url: 'department.url',
            name: {
                ru: 'Тестовый департамент',
                en: 'Test department',
            },
        },
        is_dismissed: false,
        first_name: {
            ru: 'Вася',
            en: 'John',
        },
        last_name: {
            ru: 'Пупкин',
            en: 'Doe',
        },
    },
    ...data,
});
