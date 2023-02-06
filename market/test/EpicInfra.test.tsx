import { Messages } from '@yandex-market/mbo-messages';
import { AnyAction, applyMiddleware, compose, createStore, Dispatch } from 'redux';
import { combineEpics, createEpicMiddleware, Epic } from 'redux-observable';
import { from, Observable, of } from 'rxjs';
import { catchError, filter, map, mergeMap } from 'rxjs/operators';
import actionCreatorFactory from 'typescript-fsa';
import { reducerWithInitialState } from 'typescript-fsa-reducers';

import { messageActions } from 'src/store/globalMessages';
import { response403 } from 'src/test/data/http';
import { catchFail } from 'src/utils/rxjs/errorProcessing';
import { waitForPromises } from './utils/utils';

interface State {
  value?: string;
}

const actionFactory = actionCreatorFactory('TEST_ACTION');
const ERR_0 = new Error('0');

const testActions = {
  first: actionFactory<void>('FIRST'),
  second: actionFactory<void>('SECOND'),
  third: actionFactory<void>('THIRD'),

  complexFire: actionFactory<number>('COMPLEX_FIRE'),
  complexEmit: actionFactory<number>('COMPLEX_EMIT'),
  complexWrap: actionFactory<any>('COMPLEX_WRAP'),

  httpMock: actionFactory<Response>('HTTP_MOCK'),
};

const plainEpic: Epic<AnyAction, AnyAction, State> = action$ =>
  action$.pipe(
    filter(testActions.first.match),
    mergeMap(() => of(testActions.second()))
  );

const asyncEpic: Epic<AnyAction, AnyAction, State> = action$ =>
  action$.pipe(
    filter(testActions.first.match),
    mergeMap(() => {
      return from(Promise.resolve().then(() => testActions.third()));
    })
  );

const asyncComplexEpic: Epic<AnyAction, AnyAction, State> = action$ =>
  action$.pipe(
    filter(testActions.complexFire.match),
    mergeMap(
      ({ payload }): Observable<AnyAction> =>
        // для Promises используем from->pipe
        from(
          Promise.all([
            Promise.resolve().then(() => testActions.complexEmit(0)),
            Promise.resolve().then(() => testActions.complexEmit(1)),
          ])
        ).pipe(
          // возвращая единственный action используем map, для массива mergeMap
          map(act => {
            if (payload === 0 || payload === 2 || payload === 3) {
              throw ERR_0;
            }

            return testActions.complexWrap(act);
          }),
          catchError(error => {
            if (payload === 2) {
              return from(Promise.resolve().then(() => testActions.complexWrap({ processedError: error })));
            }

            if (payload === 3) {
              return from(Promise.resolve().then(() => testActions.complexWrap({ processedError1: error }))).pipe(
                // возвращая массив используем mergeMap
                mergeMap(wrappedError => [wrappedError, testActions.complexWrap('raw fail 1')])
              );
            }

            return [testActions.complexWrap({ error }), testActions.complexWrap('raw fail')];
          })
        )
    )
  );

const asyncComplexEpicWithErrors: Epic<AnyAction, AnyAction, State> = action$ =>
  action$.pipe(
    filter(testActions.httpMock.match),
    mergeMap(
      ({ payload }): Observable<AnyAction> =>
        from(Promise.resolve(payload)).pipe(
          map(() => {
            if (payload.status !== 200) {
              throw payload;
            }

            return testActions.complexWrap(payload);
          }),
          catchFail()
        )
    )
  );

describe('Epics (just experiment with it when you dont know how to deal with epics)', () => {
  let actionsStore: AnyAction[] = [];

  const mockStoreMiddleware = () => (next: Dispatch<AnyAction>) => (action: AnyAction) => {
    actionsStore.push(action);
    next(action);
  };

  const setupStore = () => {
    actionsStore = [];
    const reducer = reducerWithInitialState<{}>({}).case(testActions.first, (_, value) => ({ value }));

    const epicMiddleware = createEpicMiddleware<AnyAction, AnyAction, { value?: string }>();

    const store = createStore(
      reducer,
      compose(
        applyMiddleware(
          epicMiddleware,
          mockStoreMiddleware
          // TODO DEBUG storeParam => next => action => [console.log(action && action.type, action), next(action)][1];
        )
      )
    );
    epicMiddleware.run(combineEpics(plainEpic, asyncEpic, asyncComplexEpic, asyncComplexEpicWithErrors));

    return { store };
  };

  const checkEpic = async (...acts: AnyAction[]) => {
    const [fire, ...rest] = acts;
    const { store } = setupStore();

    store.dispatch(fire);

    await waitForPromises(1);

    expect(actionsStore).toHaveLength(rest.length + 1);
    expect(actionsStore).toEqual([fire, ...rest]);
  };

  it('epic with promise dispatching result', async () => {
    await checkEpic(testActions.first(), testActions.second(), testActions.third());
  });

  it('more complex epic with throw', async () => {
    await checkEpic(
      testActions.complexFire(0),
      testActions.complexWrap({ error: ERR_0 }),
      testActions.complexWrap('raw fail')
    );
  });

  it('more complex epic with wrapping', async () => {
    await checkEpic(
      testActions.complexFire(1),
      testActions.complexWrap([testActions.complexEmit(0), testActions.complexEmit(1)])
    );
  });

  it('more complex epic with error processing promise', async () => {
    await checkEpic(testActions.complexFire(2), testActions.complexWrap({ processedError: ERR_0 }));
  });

  it('epic with http 200 processing', async () => {
    await checkEpic(testActions.httpMock({ status: 200 } as any), testActions.complexWrap({ status: 200 }));
  });

  it('epic with http 403 error processing', async () => {
    const http403ErrorMock = response403(['ROLE1', 'ROLE2']);
    await checkEpic(
      testActions.httpMock(http403ErrorMock),
      messageActions.add(Messages.error('', { details: { roles: ['ROLE1', 'ROLE2'] } }))
    );
  });

  it('epic with http 500 error processing', async () => {
    const http500ErrorMock = {
      status: 500,
      statusText: 'Err',
      text(): Promise<any> {
        return Promise.resolve('Error text');
      },
    };
    await checkEpic(
      testActions.httpMock(http500ErrorMock as any),
      messageActions.add(Messages.error('Error text', { details: { error: http500ErrorMock } }))
    );
  });
});
