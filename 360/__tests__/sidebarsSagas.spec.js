import {expectSaga} from 'redux-saga-test-plan';
import {select, call} from 'redux-saga-test-plan/matchers';
import {throwError} from 'redux-saga-test-plan/providers';
import {push} from 'connected-react-router';
import queryString from 'query-string';

import SagaErrorReporter from 'utils/SagaErrorReporter';
import * as routerSelectors from 'features/router/routerSelectors';

import * as sidebarsSagas from '../sidebarsSagas';
import {Views, SettingsTabs} from '../sidebarsConstants';

const errorReporter = new SagaErrorReporter('sidebars');

describe('sidebarsSagas', () => {
  describe('showSidebar', () => {
    describe('успешное выполнение', () => {
      test('должен добавлять переданные параметры в url', () => {
        const location = {
          pathname: '/event',
          search: queryString.stringify({
            test: 1
          })
        };
        const payload = {
          sidebar: Views.SETTINGS
        };
        return expectSaga(sidebarsSagas.showSidebar, {payload})
          .provide([[select(routerSelectors.getRouterLocation), location]])
          .put(
            push({
              pathname: location.pathname,
              search: queryString.stringify({
                test: 1,
                sidebar: Views.SETTINGS
              })
            })
          )
          .run();
      });
    });

    describe('неуспешное выполнение', () => {
      test('должен логировать ошибку', () => {
        const payload = {
          sidebar: Views.SETTINGS
        };
        return expectSaga(sidebarsSagas.showSidebar, {payload})
          .provide([
            [select(routerSelectors.getRouterLocation), throwError({name: 'error'})],
            [call.fn(errorReporter.send)]
          ])
          .call([errorReporter, errorReporter.send], 'showSidebar', {name: 'error'})
          .run();
      });
    });
  });

  describe('hideSidebar', () => {
    describe('успешное выполнение', () => {
      test('должен удалять параметры из url, имена которых начинаются с sidebar', () => {
        const location = {
          pathname: '/event',
          search: queryString.stringify({
            test: 1,
            sidebar: Views.SETTINGS,
            sidebarTab: SettingsTabs.NOTIFICATIONS
          })
        };
        return expectSaga(sidebarsSagas.hideSidebar)
          .provide([[select(routerSelectors.getRouterLocation), location]])
          .put(
            push({
              pathname: location.pathname,
              search: queryString.stringify({
                test: 1
              })
            })
          )
          .run();
      });
    });

    describe('неуспешное выполнение', () => {
      test('должен логировать ошибку', () => {
        return expectSaga(sidebarsSagas.hideSidebar)
          .provide([
            [select(routerSelectors.getRouterLocation), throwError({name: 'error'})],
            [call.fn(errorReporter.send)]
          ])
          .call([errorReporter, errorReporter.send], 'hideSidebar', {name: 'error'})
          .run();
      });
    });
  });
});
