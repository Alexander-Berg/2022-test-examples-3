import configureMockStore from "redux-mock-store";
import thunk from "redux-thunk";

const middlewares = [thunk];
const mockStore = configureMockStore(middlewares);

import * as actions from "../../src/DirectLoader/actions";
import * as fixtures from "./fixtures";
import PhfApiMock from "./fixtures";

describe("non-api actions", () => {
    test("select client should dispatch selection", () => {
        const expectedAction = {
            type: actions.SELECT_CLIENT,
            clientId: fixtures.API_CLIENTS()[0].direct_id,
        };

        expect(actions.selectClient(fixtures.API_CLIENTS()[0].direct_id)).toEqual(expectedAction);
    });

    test("startInitialization() should dispatch set status", () => {
        const expectedAction = {
            type: actions.START_INITIALIZATION,
            initializationStatus: fixtures.STATUS_LOADING_STATE(),
        };

        expect(actions.startInitialization()).toEqual(expectedAction);
    });

    test("endInitializationSuccess() should dispatch set success status", () => {
        const expectedAction = {
            type: actions.END_INITIALIZATION_SUCCESS,
            initializationStatus: fixtures.STATUS_SUCCESS_STATE(),
        };

        expect(actions.endInitializationSuccess()).toEqual(expectedAction);
    });

    test("endInitializationFailed() should dispatch set failed status", () => {
        const expectedAction = {
            type: actions.END_INITIALIZATION_FAILED,
            initializationStatus: fixtures.STATUS_FAILED_4xx_STATE(),
        };

        expect(actions.endInitializationFailed(fixtures.API_4xx_ERROR())).toEqual(expectedAction);
    });
});

// TODO: Find appropriate method to aggregate repeated suite methods.
// Like checks about 200, 400, 500 which repeated from suite to suite with the same interfaсe

describe("api getRegions() actions", () => {
    let api;
    let store;

    beforeEach(() => {
        api = new PhfApiMock();
        store = mockStore(fixtures.INITIAL_STATE());
    });

    test("GET/200 regions should dispatch initialization", () => {
        api.setResolveGetRegions(fixtures.API_COUNTRIES());

        const expectedActions = [
            { type: actions.REQUEST_REGIONS, countriesStatus: fixtures.STATUS_LOADING_STATE() },
            {
                type: actions.RECEIVE_REGIONS_SUCCESS,
                countries: fixtures.API_COUNTRIES(),
                countriesStatus: fixtures.STATUS_SUCCESS_STATE(),
            },
        ];

        return expect(store.dispatch(actions.fetchRegions(api)))
            .resolves.toBeUndefined()
            .then(() => expect(store.getActions()).toEqual(expectedActions));
    });

    test("GET/400 regions should dispatch status with concrete message and errors", () => {
        api.setRejectGetRegions(fixtures.API_4xx_ERROR());

        const expectedActions = [
            { type: actions.REQUEST_REGIONS, countriesStatus: fixtures.STATUS_LOADING_STATE() },
            { type: actions.RECEIVE_REGIONS_FAILED, countriesStatus: fixtures.STATUS_FAILED_4xx_STATE() },
        ];

        return expect(store.dispatch(actions.fetchRegions(api)))
            .rejects.toBeUndefined()
            .catch(() => expect(store.getActions()).toEqual(expectedActions));
    });

    test("GET/500 regions should dispatch status with server fail message", () => {
        api.setRejectGetRegions(fixtures.API_5xx_ERROR());

        const expectedActions = [
            { type: actions.REQUEST_REGIONS, countriesStatus: fixtures.STATUS_LOADING_STATE() },
            { type: actions.RECEIVE_REGIONS_FAILED, countriesStatus: fixtures.STATUS_FAILED_5xx_STATE() },
        ];

        return expect(store.dispatch(actions.fetchRegions(api)))
            .rejects.toBeUndefined()
            .catch(() => expect(store.getActions()).toEqual(expectedActions));
    });
});

describe("api getClients() actions", () => {
    let api;
    let store;

    beforeEach(() => {
        api = new PhfApiMock();
        let state = fixtures.INITIAL_STATE();
        state.countries = fixtures.API_COUNTRIES();
        store = mockStore(state);
    });

    test("GET/200 should dispatch initialization", () => {
        api.setResolveGetClients(fixtures.API_CLIENTS());

        const clientId = fixtures.API_CLIENTS()[0].direct_id;
        const regionIds = fixtures.API_COUNTRIES()[0].regions.map((region) => region.direct_id);
        const countryName = fixtures.API_COUNTRIES()[0].name;
        const expandTypeName = fixtures.API_COUNTRIES()[0].expand_types[0].name;

        const expectedActions = [
            { type: actions.REQUEST_CLIENTS, clientsStatus: fixtures.STATUS_LOADING_STATE() },
            {
                type: actions.RECEIVE_CLIENTS_SUCCESS,
                clients: fixtures.API_CLIENTS(),
                clientsStatus: fixtures.STATUS_SUCCESS_STATE(),
            },
            { type: actions.SELECT_CLIENT, clientId },
            { type: actions.Upload.EDIT_CAMPAIGN_NAME, clientId, name: "" },
            { type: actions.Upload.EDIT_CAMPAIGN_BID, bid: "", clientId },
            { type: actions.Upload.SELECT_COUNTRY, clientId, countryName },
            { type: actions.Upload.SELECT_EXPAND_TYPE, clientId, expandTypeName },
            { type: actions.Upload.SELECT_REGIONS, clientId, regionIds },
            { type: actions.Upload.ADD_BANNER, clientId, bannerId: expect.anything() },
            { type: actions.Upload.EDIT_BANNER_TITLES, clientId, bannerId: expect.anything(), titles: [] },
            { type: actions.Upload.EDIT_BANNER_TEXTS, clientId, bannerId: expect.anything(), texts: [] },
            { type: actions.Upload.EDIT_BANNER_HREFS, clientId, bannerId: expect.anything(), hrefs: [] },
            {
                type: actions.Upload.EDIT_BANNER_PARAMS,
                clientId,
                bannerId: expect.anything(),
                paramKey: "utm_campaign",
                paramValue: "",
            },
            {
                type: actions.Upload.EDIT_BANNER_PARAMS,
                clientId,
                bannerId: expect.anything(),
                paramKey: "utm_medium",
                paramValue: "",
            },
            {
                type: actions.Upload.EDIT_BANNER_PARAMS,
                clientId,
                bannerId: expect.anything(),
                paramKey: "utm_source",
                paramValue: "",
            },
            {
                type: actions.Upload.EDIT_BANNER_PARAMS,
                clientId,
                bannerId: expect.anything(),
                paramKey: "utm_term",
                paramValue: "",
            },
            { type: actions.Upload.SELECT_IMAGES, clientId, bannerId: expect.anything(), imageIds: [] },
            { type: actions.Upload.ADD_ADGROUP, clientId, adGroupId: expect.anything() },
            { type: actions.Upload.EDIT_ADGROUP_NAME, clientId, adGroupId: expect.anything(), name: "" },
            { type: actions.Upload.EDIT_ADGROUP_PHRASES, clientId, adGroupId: expect.anything(), phrases: [] },
        ];

        return expect(store.dispatch(actions.fetchClients(api)))
            .resolves.toBeUndefined()
            .then(() => expect(store.getActions()).toEqual(expectedActions));
    });

    test("GET/400 should dispatch status with concrete message and errors", () => {
        api.setRejectGetClients(fixtures.API_4xx_ERROR());

        const expectedActions = [
            { type: actions.REQUEST_CLIENTS, clientsStatus: fixtures.STATUS_LOADING_STATE() },
            { type: actions.RECEIVE_CLIENTS_FAILED, clientsStatus: fixtures.STATUS_FAILED_4xx_STATE() },
        ];

        return expect(store.dispatch(actions.fetchClients(api)))
            .rejects.toBeUndefined()
            .catch(() => expect(store.getActions()).toEqual(expectedActions));
    });

    test("GET/500 should dispatch status with server fail message", () => {
        api.setRejectGetClients(fixtures.API_5xx_ERROR());

        const expectedActions = [
            { type: actions.REQUEST_CLIENTS, clientsStatus: fixtures.STATUS_LOADING_STATE() },
            { type: actions.RECEIVE_CLIENTS_FAILED, clientsStatus: fixtures.STATUS_FAILED_5xx_STATE() },
        ];

        return expect(store.dispatch(actions.fetchClients(api)))
            .rejects.toBeUndefined()
            .catch(() => expect(store.getActions()).toEqual(expectedActions));
    });
});

describe("api getTemplates() actions", () => {
    let clientId;
    let api;
    let store;

    beforeEach(() => {
        api = new PhfApiMock();
        clientId = fixtures.API_CLIENTS()[0].direct_id;
        store = mockStore(fixtures.INITIAL_STATE());
    });

    test("GET/200 templates should dispatch initialization", () => {
        api.setResolveGetTemplates(fixtures.API_TEMPLATES());

        const expectedActions = [
            { type: actions.Catalog.REQUEST_TEMPLATES, clientId, templatesStatus: fixtures.STATUS_LOADING_STATE() },
            {
                type: actions.Catalog.RECEIVE_TEMPLATES_SUCCESS,
                clientId,
                templates: fixtures.API_TEMPLATES(),
                templatesStatus: fixtures.STATUS_SUCCESS_STATE(),
            },
        ];

        return expect(store.dispatch(actions.Catalog.fetchTemplates(api, clientId)))
            .resolves.toBeUndefined()
            .then(() => expect(store.getActions()).toEqual(expectedActions));
    });

    test("GET/200 with empty templates should dispatch initialization", () => {
        api.setResolveGetTemplates([]);

        const expectedActions = [
            { type: actions.Catalog.REQUEST_TEMPLATES, clientId, templatesStatus: fixtures.STATUS_LOADING_STATE() },
            {
                type: actions.Catalog.RECEIVE_TEMPLATES_SUCCESS,
                clientId,
                templates: [],
                templatesStatus: fixtures.STATUS_SUCCESS_STATE(),
            },
        ];

        return expect(store.dispatch(actions.Catalog.fetchTemplates(api, clientId)))
            .resolves.toBeUndefined()
            .then(() => expect(store.getActions()).toEqual(expectedActions));
    });

    test("GET/400 templates should dispatch status with concrete message and errors", () => {
        api.setRejectGetTemplates(fixtures.API_4xx_ERROR());

        const expectedActions = [
            { type: actions.Catalog.REQUEST_TEMPLATES, clientId, templatesStatus: fixtures.STATUS_LOADING_STATE() },
            {
                type: actions.Catalog.RECEIVE_TEMPLATES_FAILED,
                clientId,
                templatesStatus: fixtures.STATUS_FAILED_4xx_STATE(),
            },
        ];

        return expect(store.dispatch(actions.Catalog.fetchTemplates(api, clientId)))
            .rejects.toBeUndefined()
            .catch(() => expect(store.getActions()).toEqual(expectedActions));
    });

    test("GET/500 templates should dispatch status with server fail message", () => {
        api.setRejectGetTemplates(fixtures.API_5xx_ERROR());

        const expectedActions = [
            { type: actions.Catalog.REQUEST_TEMPLATES, clientId, templatesStatus: fixtures.STATUS_LOADING_STATE() },
            {
                type: actions.Catalog.RECEIVE_TEMPLATES_FAILED,
                clientId,
                templatesStatus: fixtures.STATUS_FAILED_5xx_STATE(),
            },
        ];

        return expect(store.dispatch(actions.Catalog.fetchTemplates(api, clientId)))
            .rejects.toBeUndefined()
            .catch(() => expect(store.getActions()).toEqual(expectedActions));
    });
});

describe("api bidTemplate() actions", () => {
    let clientId;
    let templateId;
    let api;
    let store;

    beforeEach(() => {
        api = new PhfApiMock();
        clientId = fixtures.API_CLIENTS()[0].direct_id;
        templateId = fixtures.API_TEMPLATES()[0].id;
        store = mockStore(fixtures.INITIAL_STATE());
    });

    test("GET/200 should dispatch set success status", () => {
        api.setResolveBidTemplate();

        const expectedActions = [
            {
                type: actions.Catalog.REQUEST_BIDDING,
                clientId,
                templateId,
                biddingStatus: fixtures.STATUS_LOADING_STATE(),
            },
            {
                type: actions.Catalog.RECEIVE_BIDDING_SUCCESS,
                clientId,
                templateId,
                biddingStatus: fixtures.STATUS_SUCCESS_STATE(),
            },
        ];

        return expect(store.dispatch(actions.Catalog.bidTemplate(api, clientId, templateId, 10.0)))
            .resolves.toBeUndefined()
            .then(() => expect(store.getActions()).toEqual(expectedActions));
    });

    test("GET/400 should dispatch status with concrete message and errors", () => {
        api.setRejectBidTemplate(fixtures.API_4xx_ERROR());

        const expectedActions = [
            {
                type: actions.Catalog.REQUEST_BIDDING,
                clientId,
                templateId,
                biddingStatus: fixtures.STATUS_LOADING_STATE(),
            },
            {
                type: actions.Catalog.RECEIVE_BIDDING_FAILED,
                clientId,
                templateId,
                biddingStatus: fixtures.STATUS_FAILED_4xx_STATE(),
            },
        ];

        return expect(store.dispatch(actions.Catalog.bidTemplate(api, clientId, templateId, 10.0)))
            .rejects.toBeUndefined()
            .catch(() => expect(store.getActions()).toEqual(expectedActions));
    });

    test("GET/500 should dispatch status with server fail message", () => {
        api.setRejectBidTemplate(fixtures.API_5xx_ERROR());

        const expectedActions = [
            {
                type: actions.Catalog.REQUEST_BIDDING,
                clientId,
                templateId,
                biddingStatus: fixtures.STATUS_LOADING_STATE(),
            },
            {
                type: actions.Catalog.RECEIVE_BIDDING_FAILED,
                clientId,
                templateId,
                biddingStatus: fixtures.STATUS_FAILED_5xx_STATE(),
            },
        ];

        return expect(store.dispatch(actions.Catalog.bidTemplate(api, clientId, templateId, 10.0)))
            .rejects.toBeUndefined()
            .catch(() => expect(store.getActions()).toEqual(expectedActions));
    });
});

describe("api moderateTemplate() actions", () => {
    let clientId;
    let templateId;
    let api;
    let store;

    beforeEach(() => {
        api = new PhfApiMock();
        clientId = fixtures.API_CLIENTS()[0].direct_id;
        templateId = fixtures.API_TEMPLATES()[0].id;
        store = mockStore(fixtures.INITIAL_STATE());
    });

    test("GET/200 should dispatch set success status", () => {
        api.setResolveModerateTemplate();

        const expectedActions = [
            {
                type: actions.Catalog.REQUEST_MODERATION,
                clientId,
                templateId,
                moderationStatus: fixtures.STATUS_LOADING_STATE(),
            },
            {
                type: actions.Catalog.RECEIVE_MODERATION_SUCCESS,
                clientId,
                templateId,
                moderationStatus: fixtures.STATUS_SUCCESS_STATE(),
            },
        ];

        return expect(store.dispatch(actions.Catalog.moderateTemplate(api, clientId, templateId)))
            .resolves.toBeUndefined()
            .then(() => expect(store.getActions()).toEqual(expectedActions));
    });

    test("GET/400 should dispatch status with concrete message and errors", () => {
        api.setRejectModerateTemplate(fixtures.API_4xx_ERROR());

        const expectedActions = [
            {
                type: actions.Catalog.REQUEST_MODERATION,
                clientId,
                templateId,
                moderationStatus: fixtures.STATUS_LOADING_STATE(),
            },
            {
                type: actions.Catalog.RECEIVE_MODERATION_FAILED,
                clientId,
                templateId,
                moderationStatus: fixtures.STATUS_FAILED_4xx_STATE(),
            },
        ];

        return expect(store.dispatch(actions.Catalog.moderateTemplate(api, clientId, templateId)))
            .rejects.toBeUndefined()
            .catch(() => expect(store.getActions()).toEqual(expectedActions));
    });

    test("GET/500 should dispatch status with server fail message", () => {
        api.setRejectModerateTemplate(fixtures.API_5xx_ERROR());

        const expectedActions = [
            {
                type: actions.Catalog.REQUEST_MODERATION,
                clientId,
                templateId,
                moderationStatus: fixtures.STATUS_LOADING_STATE(),
            },
            {
                type: actions.Catalog.RECEIVE_MODERATION_FAILED,
                clientId,
                templateId,
                moderationStatus: fixtures.STATUS_FAILED_5xx_STATE(),
            },
        ];

        return expect(store.dispatch(actions.Catalog.moderateTemplate(api, clientId, templateId)))
            .rejects.toBeUndefined()
            .catch(() => expect(store.getActions()).toEqual(expectedActions));
    });
});

describe("initializeCatalog actions", () => {
    let store;
    let clientId;
    let api;

    beforeEach(() => {
        let state = fixtures.INITIAL_STATE();
        state.countries = fixtures.API_COUNTRIES();
        state.clients = fixtures.CLIENTS_STATE();
        clientId = fixtures.CLIENTS_STATE()[0].direct_id;
        store = mockStore(state);
        api = new PhfApiMock();
    });

    test("GET/200 all fetchers should dispatch view catalog panel", () => {
        api.setResolveGetRegions(fixtures.API_COUNTRIES());
        api.setResolveGetClients(fixtures.API_CLIENTS());
        api.setResolveGetTemplates(fixtures.API_TEMPLATES());

        const importantActionTypes = [
            actions.VIEW_LOADING_PANEL,
            actions.START_INITIALIZATION,
            actions.RECEIVE_CLIENTS_SUCCESS,
            actions.RECEIVE_REGIONS_SUCCESS,
            actions.Catalog.RECEIVE_TEMPLATES_SUCCESS,
            actions.END_INITIALIZATION_SUCCESS,
            actions.VIEW_CATALOG_PANEL,
        ];

        return expect(store.dispatch(actions.initializeCatalog(api)))
            .resolves.toBeUndefined()
            .then(() =>
                expect(store.getActions().map((action) => action.type)).toEqual(
                    expect.arrayContaining(importantActionTypes)
                )
            );
    });

    test("GET/4**/5** in getRegions() should dispatch view error panel", () => {
        api.setRejectGetRegions(fixtures.API_4xx_ERROR());
        api.setResolveGetClients(fixtures.API_CLIENTS());
        api.setResolveGetTemplates(fixtures.API_TEMPLATES());

        const expectedActionTypes = [
            actions.VIEW_LOADING_PANEL,
            actions.START_INITIALIZATION,
            actions.RECEIVE_REGIONS_FAILED,
            actions.END_INITIALIZATION_FAILED,
            actions.VIEW_ERROR_PANEL,
        ];

        const preventedActionTypes = [
            actions.REQUEST_CLIENTS,
            actions.Catalog.REQUEST_TEMPLATES,
            actions.END_INITIALIZATION_SUCCESS,
            actions.VIEW_CATALOG_PANEL,
        ];

        return expect(store.dispatch(actions.initializeCatalog(api)))
            .resolves.toBeUndefined()
            .then(() =>
                expect(store.getActions().map((action) => action.type)).toEqual(
                    expect.arrayContaining(expectedActionTypes)
                )
            )
            .then(() =>
                expect(store.getActions().map((action) => action.type)).not.toEqual(
                    expect.arrayContaining(preventedActionTypes)
                )
            );
    });

    test("GET/4**/5** in getClients() should dispatch view error panel", () => {
        api.setResolveGetRegions(fixtures.API_COUNTRIES());
        api.setRejectGetClients(fixtures.API_5xx_ERROR());
        api.setResolveGetTemplates(fixtures.API_TEMPLATES());

        const expectedActionTypes = [
            actions.VIEW_LOADING_PANEL,
            actions.START_INITIALIZATION,
            actions.REQUEST_REGIONS,
            actions.REQUEST_CLIENTS,
            actions.END_INITIALIZATION_FAILED,
            actions.VIEW_ERROR_PANEL,
        ];

        const preventedActionTypes = [
            actions.Catalog.REQUEST_TEMPLATES,
            actions.END_INITIALIZATION_SUCCESS,
            actions.VIEW_CATALOG_PANEL,
        ];

        return expect(store.dispatch(actions.initializeCatalog(api)))
            .resolves.toBeUndefined()
            .then(() =>
                expect(store.getActions().map((action) => action.type)).toEqual(
                    expect.arrayContaining(expectedActionTypes)
                )
            )
            .then(() =>
                expect(store.getActions().map((action) => action.type)).not.toEqual(
                    expect.arrayContaining(preventedActionTypes)
                )
            );
    });

    test("GET/4**/5** in getTemplates() should dispatch view error panel", () => {
        api.setResolveGetRegions(fixtures.API_COUNTRIES());
        api.setResolveGetClients(fixtures.API_CLIENTS());
        api.setRejectGetTemplates(fixtures.API_4xx_ERROR());

        const expectedActionTypes = [
            actions.VIEW_LOADING_PANEL,
            actions.START_INITIALIZATION,
            actions.REQUEST_REGIONS,
            actions.REQUEST_CLIENTS,
            actions.Catalog.REQUEST_TEMPLATES,
            actions.END_INITIALIZATION_FAILED,
            actions.VIEW_ERROR_PANEL,
        ];

        const preventedActionTypes = [actions.END_INITIALIZATION_SUCCESS, actions.VIEW_CATALOG_PANEL];

        return expect(store.dispatch(actions.initializeCatalog(api)))
            .resolves.toBeUndefined()
            .then(() =>
                expect(store.getActions().map((action) => action.type)).toEqual(
                    expect.arrayContaining(expectedActionTypes)
                )
            )
            .then(() =>
                expect(store.getActions().map((action) => action.type)).not.toEqual(
                    expect.arrayContaining(preventedActionTypes)
                )
            );
    });
});
