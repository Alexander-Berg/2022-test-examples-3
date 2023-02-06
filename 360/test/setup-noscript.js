'use strict';

/**
 * Стабим все нужные нам части noscript-а.
 * Вызывается с забинженным this-ом на контекст выполнения тестов.
 *
 * Получаем
 * - доступный глобально объект ns (он же window.ns)
 * - доступный в тестах метод this.stubNsModel(id, params, data)
 */
export const setupNoscript = function() {
    window.ns = {
        Model: {
            get: this.sinon.stub()
        },
        page: {
            current: {
                page: 'messages',
                params: {
                    current_folder: '1',
                    threaded: 'yes'
                }
            },
            go: this.sinon.stub(),
            getDefaultUrl: this.sinon.stub().returns('#inbox'),
            history: {
                _history: [],

                // полная копия из ns.page.history.js
                getFirstValidPrevious: function(condition) {
                    var history = ns.page.history._history.concat(ns.page.currentUrl);
                    var length = history.length;
                    var url;

                    while (length--) {
                        url = history[length];
                        if (condition(url)) {
                            return url;
                        }
                    }

                    return null;
                }
            }
        },
        events: {
            on: this.sinon.stub()
        },
        router: {
            generateUrl: this.sinon.stub()
        }
    };

    this.stubNsModel = (id, params, data) => {
        let key = `model=${id}`;
        for (let pName in params) {
            key += `&${pName}=${params[pName]}`;
        }

        const fakeModel = {
            key: key,
            on: this.sinon.stub(),
            off: this.sinon.stub(),
            getData: this.sinon.stub(),
            getError: this.sinon.stub()
        };

        if (data) {
            fakeModel.getData.returns(data);
        }

        ns.Model.get.withArgs(id, params).returns(fakeModel);
    };
};
