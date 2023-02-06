import { Provider } from "react-redux";
import React from "React";
import thunk from "redux-thunk";
import { createStore, applyMiddleware, compose } from "redux";

import sinon from "sinon";
import { mount } from "enzyme";
import { createWaitForElement } from "enzyme-wait";

import { DirectLoaderPanelBase, DirectLoaderPanel } from "../../src/DirectLoader";
import DirectLoaderReducer from "../../src/DirectLoader/reducers";

import PhfApiMock from "./fixtures";
import * as fixtures from "./fixtures";

describe("fully mounted components", () => {
    let api;
    let store;
    const sandbox = sinon.sandbox.create();
    const waitForSample = createWaitForElement(".LayoutFinish");

    beforeEach(() => {
        api = new PhfApiMock();
        store = createStore(DirectLoaderReducer, compose(applyMiddleware(thunk)));
        sandbox.spy(DirectLoaderPanelBase.prototype, "renderCatalog");
        sandbox.spy(DirectLoaderPanelBase.prototype, "renderAuthorizationError");
        sandbox.spy(DirectLoaderPanelBase.prototype, "renderCommonError");
        sandbox.spy(DirectLoaderPanelBase.prototype, "renderError");
    });

    afterEach(() => {
        sandbox.restore();
    });

    test("DirectLoaderPanel should call renderCommonError() after initialization failed with status 500", () => {
        api.setResolveGetRegions(fixtures.API_COUNTRIES());
        api.setResolveGetClients(fixtures.API_CLIENTS());
        api.setRejectGetTemplates(fixtures.API_5xx_ERROR());

        const props = {
            api: api,
        };

        const component = mount(
            <Provider store={store}>
                <DirectLoaderPanel {...props} />
            </Provider>
        );

        return expect(waitForSample(component))
            .resolves.toBeDefined()
            .then(() => {
                expect(DirectLoaderPanelBase.prototype.renderCatalog.called).toBe(false);
                expect(DirectLoaderPanelBase.prototype.renderCommonError.calledOnce).toBe(true);
            });
    });

    test("DirectLoaderPanel should call renderAuthorizationError() after initialization failed with status 403", () => {
        api.setResolveGetRegions(fixtures.API_COUNTRIES());
        api.setResolveGetClients(fixtures.API_CLIENTS());
        api.setRejectGetTemplates(fixtures.API_403_ERROR());

        const props = {
            api: api,
        };

        const component = mount(
            <Provider store={store}>
                <DirectLoaderPanel {...props} />
            </Provider>
        );

        return expect(waitForSample(component))
            .resolves.toBeDefined()
            .then(() => {
                expect(DirectLoaderPanelBase.prototype.renderCatalog.called).toBe(false);
                expect(DirectLoaderPanelBase.prototype.renderAuthorizationError.calledOnce).toBe(true);
            });
    });

    test("DirectLoaderPanel should call renderCatalog() after success initialization", () => {
        api.setResolveGetRegions(fixtures.API_COUNTRIES());
        api.setResolveGetClients(fixtures.API_CLIENTS());
        api.setResolveGetTemplates(fixtures.API_TEMPLATES());

        const props = {
            api: api,
        };
        const component = mount(
            <Provider store={store}>
                <DirectLoaderPanel {...props} />
            </Provider>
        );

        return expect(waitForSample(component))
            .resolves.toBeDefined()
            .then(() => {
                expect(DirectLoaderPanelBase.prototype.renderError.called).toBe(false);
                expect(DirectLoaderPanelBase.prototype.renderCatalog.calledOnce).toBe(true);
            });
    });
});
