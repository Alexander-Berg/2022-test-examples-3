import a from 'src/store/shared/actions';
import { loaderActions } from 'src/store/ui/loader/actions';
import { loadCategories, upload } from 'src/store/shared/epic';
import { setupStoreWithEpics } from 'src/test/setup';

describe('shared data epics', () => {
  jest.useFakeTimers();

  it('loadCategories epic', () => {
    const { store, getActions, api } = setupStoreWithEpics(loadCategories);

    store.dispatch(a.loadCategories.started());

    api.deepmindCategoriesController.all.next().resolve([]);

    expect(getActions()).toEqual([
      a.loadCategories.started(),
      loaderActions.show(a.loadCategories.type),
      a.loadCategories.done({ result: [] }),
      loaderActions.hide(a.loadCategories.type),
    ]);
  });

  it('jobSubmitted epic', () => {
    const { store, getActions } = setupStoreWithEpics(upload);

    store.dispatch(a.jobSubmitted({ jobId: 1, loaderId: 'loader' }));
    // TODO: timer(500, 1000).pipe( не отрабатывает в тестах
    // api.backgroundActionController.checkAction.next().resolve({finished: ''} as BackgroundAction);

    expect(getActions()).toEqual([a.jobSubmitted({ jobId: 1, loaderId: 'loader' })]);
  });
});
