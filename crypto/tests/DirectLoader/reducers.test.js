import DirectLoaderReducer, {
    clientReducer,
    bannerReducer,
    adGroupReducer,
    imagesReducer,
    bannersReducer,
    adGroupsReducer,
    catalogPanelReducer,
    previewPanelReducer,
    geoReducer,
    campaignReducer,
    templatesReducer,
} from "../../src/DirectLoader/reducers";

import * as _ from "lodash";
import { WindowsEnum } from "../../src/DirectLoader/actions/utils";

import * as actions from "../../src/DirectLoader/actions";
import * as fixtures from "./fixtures";

describe("reducers default initialization", () => {
    test("DirectLoaderReducer initialization", () => {
        expect(DirectLoaderReducer(undefined, {})).toEqual(fixtures.INITIAL_STATE());
    });

    test("clientReducer initialization", () => {
        expect(clientReducer(undefined, {})).toEqual(fixtures.CLIENT_INITIAL_STATE());
    });

    test("bannersReducer initialization", () => {
        expect(bannersReducer(undefined, {})).toEqual(fixtures.CLIENT_INITIAL_STATE().banners);
    });

    test("bannerReducer initialization", () => {
        expect(bannerReducer(undefined, {})).toEqual(fixtures.BANNER_INITIAL_STATE());
    });

    test("adGroupsReducer initialization", () => {
        expect(adGroupsReducer(undefined, {})).toEqual(fixtures.CLIENT_INITIAL_STATE().adGroups);
    });

    test("adGroupReducer initialization", () => {
        expect(adGroupReducer(undefined, {})).toEqual(fixtures.ADGROUP_INITIAL_STATE());
    });

    test("imagesReducer initialization", () => {
        expect(imagesReducer(undefined, {})).toEqual(fixtures.CLIENT_INITIAL_STATE().images);
    });

    test("catalogPanelReducer initialization", () => {
        expect(catalogPanelReducer(undefined, {})).toEqual(fixtures.INITIAL_STATE().catalogPanel);
    });

    test("previewPanelReducer initialization", () => {
        expect(previewPanelReducer(undefined, {})).toEqual(fixtures.INITIAL_STATE().previewPanel);
    });

    test("geoReducer initialization", () => {
        expect(geoReducer(undefined, {})).toEqual(fixtures.CLIENT_INITIAL_STATE().geo);
    });

    test("campaignReducer initialization", () => {
        expect(campaignReducer(undefined, {})).toEqual(fixtures.CLIENT_INITIAL_STATE().campaign);
    });

    test("templatesReducer initialization", () => {
        expect(templatesReducer(undefined, {})).toEqual(fixtures.INITIAL_STATE().catalogPanel.templates);
    });
});

describe("api getRegions() reducers", () => {
    let state;

    beforeEach(() => {
        state = fixtures.INITIAL_STATE();
    });

    test("REQUEST_REGIONS should save regionsStatus", () => {
        state.countriesStatus = fixtures.STATUS_SUCCESS_STATE();

        const action = {
            type: actions.REQUEST_REGIONS,
            countriesStatus: fixtures.STATUS_SUCCESS_STATE(),
        };

        expect(DirectLoaderReducer(undefined, action)).toEqual(state);
    });

    test("RECEIVE_REGIONS_SUCCESS should save clients and clientsStatus", () => {
        state.countriesStatus = fixtures.STATUS_SUCCESS_STATE();
        state.countries = fixtures.API_COUNTRIES();

        const action = {
            type: actions.RECEIVE_REGIONS_SUCCESS,
            countries: fixtures.API_COUNTRIES(),
            countriesStatus: fixtures.STATUS_SUCCESS_STATE(),
        };

        expect(DirectLoaderReducer(undefined, action)).toEqual(state);
    });

    test("RECEIVE_REGIONS_FAILED should save regionsStatus", () => {
        state.countriesStatus = fixtures.STATUS_FAILED_4xx_STATE();

        const action = {
            type: actions.RECEIVE_REGIONS_FAILED,
            countriesStatus: fixtures.STATUS_FAILED_4xx_STATE(),
        };

        expect(DirectLoaderReducer(undefined, action)).toEqual(state);
    });
});

describe("api getClients() reducers", () => {
    let state;

    beforeEach(() => {
        state = fixtures.INITIAL_STATE();
    });

    test("REQUEST_CLIENTS should save clientsStatus", () => {
        state.clientsStatus = fixtures.STATUS_LOADING_STATE();

        const action = {
            type: actions.REQUEST_CLIENTS,
            clientsStatus: fixtures.STATUS_LOADING_STATE(),
        };

        expect(DirectLoaderReducer(undefined, action)).toEqual(state);
    });

    test("RECEIVE_CLIENTS_SUCCESS should save clients and clientsStatus", () => {
        state.clientsStatus = fixtures.STATUS_SUCCESS_STATE();
        state.clients = fixtures.CLIENTS_STATE();

        const action = {
            type: actions.RECEIVE_CLIENTS_SUCCESS,
            clients: fixtures.API_CLIENTS(),
            clientsStatus: fixtures.STATUS_SUCCESS_STATE(),
        };

        expect(DirectLoaderReducer(undefined, action)).toEqual(state);
    });

    test("RECEIVE_CLIENTS_FAILED should save clientsStatus", () => {
        state.clientsStatus = fixtures.STATUS_FAILED_4xx_STATE();

        const action = {
            type: actions.RECEIVE_CLIENTS_FAILED,
            clientsStatus: fixtures.STATUS_FAILED_4xx_STATE(),
        };

        expect(DirectLoaderReducer(undefined, action)).toEqual(state);
    });
});

describe("api getTemplates() reducers", () => {
    let state;

    beforeEach(() => {
        state = fixtures.INITIAL_STATE();
    });

    test("REQUEST_TEMPLATES should save templatesStatus", () => {
        state.catalogPanel.templatesStatus = fixtures.STATUS_LOADING_STATE();

        const action = {
            type: actions.Catalog.REQUEST_TEMPLATES,
            clientId: fixtures.API_CLIENTS()[0].direct_id,
            templatesStatus: fixtures.STATUS_LOADING_STATE(),
        };

        expect(DirectLoaderReducer(undefined, action)).toEqual(state);
    });

    test("RECEIVE_TEMPLATES_SUCCESS should save templates and templatesStatus", () => {
        state.catalogPanel.templatesStatus = fixtures.STATUS_SUCCESS_STATE();
        state.catalogPanel.templates = fixtures.TEMPLATES_STATE();

        const action = {
            type: actions.Catalog.RECEIVE_TEMPLATES_SUCCESS,
            clientId: fixtures.API_CLIENTS()[0].direct_id,
            templates: fixtures.API_TEMPLATES(),
            templatesStatus: fixtures.STATUS_SUCCESS_STATE(),
        };

        expect(DirectLoaderReducer(undefined, action)).toEqual(state);
    });

    test("RECEIVE_TEMPLATES_SUCCESS with empty templates should save templates and templatesStatus", () => {
        state.catalogPanel.templatesStatus = fixtures.STATUS_SUCCESS_STATE();
        state.catalogPanel.templates = [];

        const action = {
            type: actions.Catalog.RECEIVE_TEMPLATES_SUCCESS,
            clientId: fixtures.API_CLIENTS()[0].direct_id,
            templates: [],
            templatesStatus: fixtures.STATUS_SUCCESS_STATE(),
        };

        expect(DirectLoaderReducer(undefined, action)).toEqual(state);
    });

    test("RECEIVE_TEMPLATES_FAILED should save templatesStatus", () => {
        state.catalogPanel.templatesStatus = fixtures.STATUS_FAILED_4xx_STATE();

        const action = {
            type: actions.Catalog.RECEIVE_TEMPLATES_FAILED,
            clientId: fixtures.API_CLIENTS()[0].direct_id,
            templatesStatus: fixtures.STATUS_FAILED_4xx_STATE(),
        };

        expect(DirectLoaderReducer(undefined, action)).toEqual(state);
    });
});

describe("api moderateTemplate() reducers", () => {
    let state;
    let clientId;
    let templateId;
    let templateIndex = 0;

    beforeEach(() => {
        state = fixtures.INITIAL_STATE();
        clientId = fixtures.API_CLIENTS()[0].direct_id;
        templateId = fixtures.API_TEMPLATES()[templateIndex].id;
        state.catalogPanel.templates = fixtures.TEMPLATES_STATE();
    });

    test("REQUEST_MODERATION should save moderationStatus", () => {
        const actionState = _.cloneDeep(state);

        const action = {
            type: actions.Catalog.REQUEST_MODERATION,
            clientId: clientId,
            templateId: templateId,
            moderationStatus: fixtures.STATUS_LOADING_STATE(),
        };

        state.catalogPanel.templates[templateIndex].moderationStatus = fixtures.STATUS_LOADING_STATE();

        expect(DirectLoaderReducer(actionState, action)).toEqual(state);
    });

    test("RECEIVE_MODERATION_SUCCESS should save templatesStatus", () => {
        const actionState = _.cloneDeep(state);

        const action = {
            type: actions.Catalog.RECEIVE_MODERATION_SUCCESS,
            clientId: clientId,
            templateId: templateId,
            moderationStatus: fixtures.STATUS_SUCCESS_STATE(),
        };

        state.catalogPanel.templates[templateIndex].moderationStatus = fixtures.STATUS_SUCCESS_STATE();

        expect(DirectLoaderReducer(actionState, action)).toEqual(state);
    });

    test("RECEIVE_MODERATION_FAILED should save templatesStatus", () => {
        const actionState = _.cloneDeep(state);

        const action = {
            type: actions.Catalog.RECEIVE_MODERATION_FAILED,
            clientId: clientId,
            templateId: templateId,
            moderationStatus: fixtures.STATUS_FAILED_4xx_STATE(),
        };

        state.catalogPanel.templates[templateIndex].moderationStatus = fixtures.STATUS_FAILED_4xx_STATE();

        expect(DirectLoaderReducer(actionState, action)).toEqual(state);
    });
});

describe("api biddingTemplate() reducers", () => {
    let state;
    let templateId;
    let clientId;
    let templateIndex = 0;

    beforeEach(() => {
        state = fixtures.INITIAL_STATE();
        clientId = fixtures.API_CLIENTS()[0].direct_id;
        templateId = fixtures.API_TEMPLATES()[templateIndex].id;
        state.catalogPanel.templates = fixtures.TEMPLATES_STATE();
    });

    test("REQUEST_BIDDING should save moderationStatus", () => {
        const actionState = _.cloneDeep(state);

        const action = {
            type: actions.Catalog.REQUEST_BIDDING,
            clientId: clientId,
            templateId: templateId,
            biddingStatus: fixtures.STATUS_LOADING_STATE(),
        };

        state.catalogPanel.templates[templateIndex].biddingStatus = fixtures.STATUS_LOADING_STATE();

        expect(DirectLoaderReducer(actionState, action)).toEqual(state);
    });

    test("RECEIVE_BIDDING_SUCCESS should save templatesStatus", () => {
        const actionState = _.cloneDeep(state);

        const action = {
            type: actions.Catalog.RECEIVE_BIDDING_SUCCESS,
            clientId: clientId,
            templateId: templateId,
            biddingStatus: fixtures.STATUS_SUCCESS_STATE(),
        };

        state.catalogPanel.templates[templateIndex].biddingStatus = fixtures.STATUS_SUCCESS_STATE();

        expect(DirectLoaderReducer(actionState, action)).toEqual(state);
    });

    test("RECEIVE_BIDDING_FAILED should save templatesStatus", () => {
        const actionState = _.cloneDeep(state);

        const action = {
            type: actions.Catalog.RECEIVE_BIDDING_FAILED,
            clientId: clientId,
            templateId: templateId,
            biddingStatus: fixtures.STATUS_FAILED_4xx_STATE(),
        };

        state.catalogPanel.templates[templateIndex].biddingStatus = fixtures.STATUS_FAILED_4xx_STATE();

        expect(DirectLoaderReducer(actionState, action)).toEqual(state);
    });
});

describe("non-api reducers", () => {
    let state;

    beforeEach(() => {
        state = fixtures.INITIAL_STATE();
    });

    test("SELECT_CLIENT should set activeClientId", () => {
        state.activeClientId = fixtures.API_CLIENTS()[0].direct_id;

        const action = {
            type: actions.SELECT_CLIENT,
            clientId: fixtures.API_CLIENTS()[0].direct_id,
        };

        expect(DirectLoaderReducer(undefined, action)).toMatchObject(state);
    });

    test("VIEW_CATALOG_PANEL should set window", () => {
        state.window = WindowsEnum.CATALOG;

        const action = {
            type: actions.VIEW_CATALOG_PANEL,
        };

        expect(DirectLoaderReducer(undefined, action)).toEqual(state);
    });

    test("VIEW_ERROR_PANEL should set window", () => {
        state.window = WindowsEnum.ERROR;

        const action = {
            type: actions.VIEW_ERROR_PANEL,
        };

        expect(DirectLoaderReducer(undefined, action)).toEqual(state);
    });

    test("VIEW_LOADING_PANEL should set window", () => {
        state.window = WindowsEnum.LOADING;

        const action = {
            type: actions.VIEW_LOADING_PANEL,
        };

        expect(DirectLoaderReducer(undefined, action)).toEqual(state);
    });

    test("START_INITIALIZATION set initialization status", () => {
        state.initializationStatus = fixtures.STATUS_LOADING_STATE();

        const action = {
            type: actions.START_INITIALIZATION,
            initializationStatus: fixtures.STATUS_LOADING_STATE(),
        };

        expect(DirectLoaderReducer(undefined, action)).toEqual(state);
    });

    test("END_INITIALIZATION_SUCCESS set initialization status", () => {
        state.initializationStatus = fixtures.STATUS_SUCCESS_STATE();

        const action = {
            type: actions.END_INITIALIZATION_SUCCESS,
            initializationStatus: fixtures.STATUS_SUCCESS_STATE(),
        };

        expect(DirectLoaderReducer(undefined, action)).toEqual(state);
    });

    test("END_INITIALIZATION_FAILED set initialization status", () => {
        state.initializationStatus = fixtures.STATUS_FAILED_4xx_STATE();

        const action = {
            type: actions.END_INITIALIZATION_FAILED,
            initializationStatus: fixtures.STATUS_FAILED_4xx_STATE(),
        };

        expect(DirectLoaderReducer(undefined, action)).toEqual(state);
    });
});
