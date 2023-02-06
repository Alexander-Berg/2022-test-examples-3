'use strict';

// stores
import FoldersStore from '../mail-react/store/FoldersStore';
import RouterStore from '../mail-react/store/RouterStore';
import SearchStore from '../mail-react/store/SearchStore';

import SettingsStore from '../mail-react/store/SettingsStore';

// services
import SearchService from '../mail-react/services/search/SearchService';
import RouterService from '../mail-react/services/router/RouterService';
import QueryLanguageService from '../mail-react/services/query-language';

import UnsubscribeFiltersLogService from '../mail-react/services/unsubscribe-filters/UnsubscribeFiltersLogService';
import SettingsService from '../mail-react/services/settings/SettingsService';

/**
 * Создаём сторы и сервисы для тестирования сервисов, реакции и чего-то ещё более высокоуровневого.
 * Для тестирования сторов нужно их (сторы) создавать руками прямо в тестах, чтобы иметь больший контроль
 * при тестировании ну и плюс там не к чему иметь сервисы.
 *
 * Вызывается с забинженным this-ом на контекст выполнения тестов (чтобы можно было пользоваться this.sinon).
 * @returns {{ stores:Object, services:Object }}
 */
export const setupApp = function() {
    this.sinon.stub(FoldersStore.prototype, '_addModelSubscription');
    this.sinon.stub(SearchStore.prototype, '_addModelSubscription');
    this.sinon.stub(SettingsStore.prototype, '_addModelSubscription');

    this.sinon.spy(SearchService.prototype, 'syncSearchQuery');


    this.stores = {
        foldersStore: new FoldersStore(),
        routerStore: new RouterStore(),
        searchStore: new SearchStore(),
        settingsStore: new SettingsStore()
    };

    this.services = {};
    this.services.queryLanguageService = new QueryLanguageService(this.stores);
    this.services.searchService = new SearchService(this.stores, this.services);
    this.services.unsubscribeFiltersLogService = new UnsubscribeFiltersLogService({}, {});
    this.services.settingsService = new SettingsService(this.stores);
    this.services.routerService = new RouterService(this.stores, this.services);

    this.sinon.stub(this.services.queryLanguageService, 'hasBubbleQLFeature').get(() => false);
};
