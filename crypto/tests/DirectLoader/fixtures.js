import * as _ from "lodash";
import { APIStatusEnum, APIStatusMessage, WindowsEnum } from "../../src/DirectLoader/actions/utils";

// These consts are functions for immutability

export const STATUS_DRAFT_STATE = () => ({
    status: APIStatusEnum.DRAFT,
    message: APIStatusMessage.DRAFT,
    errors: [],
    code: 0,
});

export const STATUS_LOADING_STATE = () => ({
    status: APIStatusEnum.LOADING,
    message: APIStatusMessage.LOADING,
    errors: [],
    code: 0,
});

export const STATUS_FAILED_4xx_STATE = () => ({
    status: APIStatusEnum.FAILED,
    message: "Ошибка",
    errors: ["Все плохо"],
    code: expect.any(Number),
});

export const STATUS_FAILED_5xx_STATE = () => ({
    status: APIStatusEnum.FAILED,
    message: "Внутренняя ошибка сервера(500)",
    errors: [],
    code: expect.any(Number),
});

export const STATUS_SUCCESS_STATE = () => ({
    status: APIStatusEnum.SUCCESS,
    message: APIStatusMessage.SUCCESS,
    errors: [],
    code: expect.any(Number),
});

export const INITIAL_STATE = () => ({
    activeClientId: null,
    catalogPanel: {
        selectedTemplate: null,
        selectedTemplateStatus: STATUS_DRAFT_STATE(),
        templates: [],
        templatesStatus: STATUS_DRAFT_STATE(),
    },
    clients: [],
    clientsStatus: STATUS_DRAFT_STATE(),
    countries: [],
    countriesStatus: STATUS_DRAFT_STATE(),
    initializationStatus: STATUS_DRAFT_STATE(),
    previewPanel: {
        adGroups: {
            active: null,
            values: [],
        },
        banners: [],
        campaigns: {
            active: null,
            values: [],
        },
        isLoaded: false,
        phrases: [],
    },
    window: WindowsEnum.LOADING,
});

export const CLIENT_INITIAL_STATE = () => ({
    id: null,
    name: null,
    images: [],
    infoViewImageId: null,
    adGroups: [],
    banners: [],
    geo: {
        countryName: null,
        expandTypeName: null,
        regionIds: null,
    },
    campaign: {
        name: "",
        bid: "",
    },
    savingStatus: {
        status: APIStatusEnum.DRAFT,
        message: APIStatusMessage.DRAFT,
        errors: [],
        code: 0,
        visible: false,
    },
    uploadingStatus: {
        status: APIStatusEnum.DRAFT,
        message: APIStatusMessage.DRAFT,
        errors: [],
        code: 0,
        visible: false,
    },
    template: null,
    validation: null,
});

export const BANNER_INITIAL_STATE = () => ({
    id: expect.anything(),
    titles: [],
    texts: [],
    hrefs: [],
    imageIds: [],
    href_params: {
        utm_source: "",
        utm_medium: "",
        utm_campaign: "",
        utm_term: "",
    },
});

export const ADGROUP_INITIAL_STATE = () => ({
    id: expect.anything(),
    name: "",
    phrases: [],
});

export const API_CLIENTS = () => [
    {
        direct_id: 9845107,
        name: "Avia",
    },
];

export const CLIENTS_STATE = () =>
    API_CLIENTS().map((client) => _.merge(CLIENT_INITIAL_STATE(), { id: client.direct_id, name: client.name }));

export const API_COUNTRIES = () => [
    {
        regions: [
            {
                direct_id: 10,
                name: "Test region",
            },
        ],
        expand_types: [
            {
                available: true,
                name: "Test expand type",
            },
        ],
        name: "Test country",
    },
];

export const API_TEMPLATES = () => [
    {
        id: 1,
        name: "Шаблон 1",
        was_uploaded: false,
        last_task_state: {
            status_name: "Не выполнено",
            status_id: 2,
        },
        creation_time: "2017-11-08T21:27:46.605898",
        last_task_name: "Загрузка кампании",
    },
    {
        id: 2,
        name: "Шаблон 2",
        was_uploaded: false,
        last_task_state: {
            status_name: "Черновик",
            status_id: 0,
        },
        creation_time: "2017-11-08T21:27:47.605898",
        last_task_name: null,
    },
];

export const TEMPLATES_STATE = () => [
    {
        id: 1,
        name: "Шаблон 1",
        creationTime: "2017-11-08T21:27:46.605898",
        wasUploaded: false,
        taskStatus: {
            statusName: "Не выполнено",
            statusId: 2,
        },
        task: "Загрузка кампании",
        moderationStatus: STATUS_DRAFT_STATE(),
        biddingStatus: STATUS_DRAFT_STATE(),
    },
    {
        id: 2,
        name: "Шаблон 2",
        creationTime: "2017-11-08T21:27:47.605898",
        wasUploaded: false,
        taskStatus: {
            statusName: "Черновик",
            statusId: 0,
        },
        task: null,
        moderationStatus: STATUS_DRAFT_STATE(),
        biddingStatus: STATUS_DRAFT_STATE(),
    },
];

export const API_4xx_ERROR = () => ({
    obj: {
        message: "Ошибка",
        errors: ["Все плохо"],
    },
    status: 400,
});

export const API_403_ERROR = () => ({
    obj: {
        message: "Ошибка",
        errors: ["Все плохо"],
    },
    status: 403,
});

export const API_5xx_ERROR = () => ({
    obj: {
        message: "Ошибка",
        errors: ["Все плохо"],
    },
    status: 500,
});

export default class PhfAPIMock {
    constructor() {
        this.getClients = jest.fn();
        this.getRegions = jest.fn();
        this.getTemplates = jest.fn();
        this.bidTemplate = jest.fn();
        this.moderateTemplate = jest.fn();
    }

    setResolveGetClients(clients) {
        this.getClients.mockReturnValue(new Promise((resolve, reject) => resolve(clients)));
    }

    setRejectGetClients(error) {
        this.getClients.mockReturnValue(new Promise((resolve, reject) => reject(error)));
    }

    setResolveGetRegions(regions) {
        this.getRegions.mockReturnValue(new Promise((resolve, reject) => resolve(regions)));
    }

    setRejectGetRegions(error) {
        this.getRegions.mockReturnValue(new Promise((resolve, reject) => reject(error)));
    }

    setResolveGetTemplates(templates) {
        this.getTemplates.mockReturnValue(new Promise((resolve, reject) => resolve(templates)));
    }

    setRejectGetTemplates(error) {
        this.getTemplates.mockReturnValue(new Promise((resolve, reject) => reject(error)));
    }

    setResolveBidTemplate() {
        this.bidTemplate.mockReturnValue(new Promise((resolve, reject) => resolve()));
    }

    setRejectBidTemplate(error) {
        this.bidTemplate.mockReturnValue(new Promise((resolve, reject) => reject(error)));
    }

    setResolveModerateTemplate() {
        this.moderateTemplate.mockReturnValue(new Promise((resolve, reject) => resolve()));
    }

    setRejectModerateTemplate(error) {
        this.moderateTemplate.mockReturnValue(new Promise((resolve, reject) => reject(error)));
    }
}
