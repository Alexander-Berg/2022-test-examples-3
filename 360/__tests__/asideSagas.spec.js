import {expectSaga} from 'redux-saga-test-plan';
import {select} from 'redux-saga-test-plan/matchers';
import {throwError} from 'redux-saga-test-plan/providers';
import queryString from 'query-string';
import * as matchers from 'redux-saga-test-plan/matchers';

import SagaErrorReporter from 'utils/SagaErrorReporter';
import * as settingsActions from 'features/settings/settingsActions';
import * as sidebarsActions from 'features/sidebars/sidebarsActions';
import * as routerSelectors from 'features/router/routerSelectors';
import {
  LeftViews as LeftSidebars,
  RightViews as RightSidebars
} from 'features/sidebars/sidebarsConstants';

import * as asideSagas from '../asideSagas';
import * as asideSelectors from '../asideSelectors';
import * as asideActions from '../asideActions';

const errorReporter = new SagaErrorReporter('aside');

describe('asideSagas', () => {
  describe('toggleAside', () => {
    describe('успешное выполнение', () => {
      test('должен менять флаг раскрытия панели в состоянии', () => {
        const location = {search: ''};
        return expectSaga(asideSagas.toggleAside)
          .provide([
            [select(asideSelectors.getIsAsideExpanded), false],
            [select(routerSelectors.getRouterLocation), location]
          ])
          .put(asideActions.setAsideExpansion(true))
          .run();
      });

      test('должен сохранять флаг раскрытия панели в настройках', () => {
        const location = {search: ''};
        return expectSaga(asideSagas.toggleAside)
          .provide([
            [select(asideSelectors.getIsAsideExpanded), false],
            [select(routerSelectors.getRouterLocation), location]
          ])
          .put(settingsActions.updateSettings({values: {isAsideExpanded: true}}))
          .run();
      });

      test('должен закрывать всплывающую панель слева, когда боковая панель скрывается', () => {
        const location = {
          search: queryString.stringify({
            sidebar: LeftSidebars[0]
          })
        };
        return expectSaga(asideSagas.toggleAside)
          .provide([
            [select(asideSelectors.getIsAsideExpanded), true],
            [select(routerSelectors.getRouterLocation), location]
          ])
          .put(sidebarsActions.hideSidebar())
          .run();
      });

      test('не должен закрывать всплывающую панель справа, когда боковая панель скрывается', () => {
        const location = {
          search: queryString.stringify({
            sidebar: RightSidebars[0]
          })
        };
        return expectSaga(asideSagas.toggleAside)
          .provide([
            [select(asideSelectors.getIsAsideExpanded), true],
            [select(routerSelectors.getRouterLocation), location]
          ])
          .not.put(sidebarsActions.hideSidebar())
          .run();
      });

      test('не должен закрывать всплывающие панели, когда боковая панель раскрывается', () => {
        const location = {
          search: queryString.stringify({
            sidebar: LeftSidebars[0]
          })
        };
        return expectSaga(asideSagas.toggleAside)
          .provide([
            [select(asideSelectors.getIsAsideExpanded), false],
            [select(routerSelectors.getRouterLocation), location]
          ])
          .not.put(sidebarsActions.hideSidebar())
          .run();
      });
    });

    describe('неуспешное выполнение', () => {
      test('должен логировать ошибку', () => {
        return expectSaga(asideSagas.toggleAside)
          .provide([
            [select(asideSelectors.getIsAsideExpanded), throwError({name: 'error'})],
            [matchers.call.fn(errorReporter.send)]
          ])
          .call([errorReporter, errorReporter.send], 'toggleAside', {name: 'error'})
          .run();
      });
    });
  });
});
