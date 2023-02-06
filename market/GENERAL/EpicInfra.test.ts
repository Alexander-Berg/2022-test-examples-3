import { AnyAction, applyMiddleware, compose, createStore, Dispatch } from 'redux';
import { combineEpics, createEpicMiddleware, Epic } from 'redux-observable';
import { from, Observable, of } from 'rxjs';
import { catchError, filter, map, mergeMap } from 'rxjs/operators';
import actionCreatorFactory from 'typescript-fsa';
import { reducerWithInitialState } from 'typescript-fsa-reducers';

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
          // возвращая единственный action используем map
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

describe('Epics (just experiment with it when you dont know how to deal with epics)', () => {
  let actionsStore: AnyAction[] = [];

  const mockStoreMiddleware = () => (next: Dispatch<AnyAction>) => (action: AnyAction) => {
    actionsStore.push(action);
    next(action);
  };

  const setupStore = () => {
    actionsStore = [];
    // eslint-disable-next-line no-empty-pattern
    const reducer = reducerWithInitialState<any>({}).case(testActions.first, ({}, value) => ({ value }));

    const epicMiddleware = createEpicMiddleware<AnyAction, AnyAction, { value?: string }>();
    // const loggerMiddleware = storeParam => next => action =>
    //   [console.log(action && action.type, action), next(action)][1];
    const store = createStore(
      reducer,
      compose(
        applyMiddleware(
          epicMiddleware,
          mockStoreMiddleware
          // , loggerMiddleware
        )
      )
    );
    epicMiddleware.run(combineEpics(plainEpic, asyncEpic, asyncComplexEpic));

    return { store };
  };

  const checkEpic = (...acts: any[]) => {
    const [done, fire, ...rest] = acts;
    const { store } = setupStore();

    store.dispatch(fire);

    setTimeout(() => {
      expect(actionsStore).toHaveLength(rest.length + 1);
      expect(actionsStore).toEqual([fire, ...rest]);
      done();
    }, 1);
  };

  it('epic with promise dispatching result', done => {
    checkEpic(done, testActions.first(), testActions.second(), testActions.third());
  }, 100);

  it('more complex epic with throw', done => {
    checkEpic(
      done,
      testActions.complexFire(0),
      testActions.complexWrap({ error: ERR_0 }),
      testActions.complexWrap('raw fail')
    );
  }, 100);

  it('more complex epic with wrapping', done => {
    checkEpic(
      done,
      testActions.complexFire(1),
      testActions.complexWrap([testActions.complexEmit(0), testActions.complexEmit(1)])
    );
  }, 100);

  it('more complex epic with error processing promise', done => {
    checkEpic(done, testActions.complexFire(2), testActions.complexWrap({ processedError: ERR_0 }));
  }, 100);

  it('more complex epic with error processing promise returns array', done => {
    checkEpic(
      done,
      testActions.complexFire(3),
      testActions.complexWrap({ processedError1: ERR_0 }),
      testActions.complexWrap('raw fail 1')
    );
  }, 100);
});
