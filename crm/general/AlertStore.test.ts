import { runInAction } from 'mobx';
import { waitFor } from '@testing-library/react';
import { User } from 'types/entities/user';
import { AlertDTO } from 'types/dto/AlertDTO';
import { Store } from '../Store';
import { Tree } from '../Tree';
import { AlertStore } from './AlertStore';
import { AlertLoadHandler, Tree as ITree } from '../../../types';

const createAlertDTO = (partialAlertDTO: Partial<AlertDTO>): AlertDTO => ({
  id: 1,
  category: {
    id: 1,
    name: '1',
  },
  name: '1',
  modifiedOn: new Date().toISOString(),
  modifiedBy: {} as User,
  expiredOn: new Date().toISOString(),
  ...partialAlertDTO,
});

describe('AlertStore', () => {
  let alertStore = new AlertStore();
  let tree: ITree = new Tree();
  const _store = new Store({
    tree,
  });
  let disposers: (() => void)[];
  beforeEach(() => {
    alertStore = new AlertStore();
    alertStore.setExternalHandler(jest.fn(() => Promise.resolve([])));
    tree = new Tree();
    const _store = new Store({
      tree,
      alertStore,
    });
    disposers = alertStore.runReactions();
  });

  afterEach(() => {
    disposers.forEach((disposer) => disposer());
  });

  describe('.runReactions', () => {
    describe('when tree.lastHighlighted changes', () => {
      it('reloads alerts', () => {
        const externalHandler = jest.fn((categoryIds: number[]) =>
          Promise.resolve(
            categoryIds.map((categoryId) =>
              createAlertDTO({
                id: categoryId,
                category: {
                  id: categoryId,
                  name: categoryId.toString(),
                },
              }),
            ),
          ),
        );
        alertStore.setExternalHandler(externalHandler);

        runInAction(() => {
          tree.highlightPath = [1, 2, 3];
        });

        expect(externalHandler).toBeCalledWith([1, 2, 3]);
      });
    });
  });

  describe('.load', () => {
    it('uses external handler to load alerts', () => {
      const externalHandler = jest.fn(() => Promise.resolve([]));
      alertStore.setExternalHandler(externalHandler);

      alertStore.load([1, 2]);

      expect(externalHandler).toBeCalledWith([1, 2]);
    });

    describe('when there are existing alerts for categoryIds', () => {
      it('sets loaded alerts to .byCategoryId', async () => {
        const alert1 = createAlertDTO({
          id: 1,
          category: {
            name: '5',
            id: 5,
          },
        });
        const externalHandler: AlertLoadHandler = jest.fn(() => Promise.resolve([alert1]));
        alertStore.setExternalHandler(externalHandler);

        alertStore.load([5, 2]);

        await waitFor(() => {
          expect(alertStore.byCategoryId.size).toBe(1);
          expect(alertStore.byCategoryId.get(5)).toEqual(alert1);
        });
      });
    });

    describe('when there are no alerts for categoryIds', () => {
      it('sets loaded alerts to .byCategoryId', async () => {
        const externalHandler: AlertLoadHandler = jest.fn(() => Promise.resolve([]));
        alertStore.setExternalHandler(externalHandler);

        alertStore.load([5, 2]);

        await waitFor(() => {
          expect(Array.from(alertStore.byCategoryId)).toEqual([]);
        });
      });
    });
  });

  describe('.canBeVisibleAlert', () => {
    describe('when there are no current alerts', () => {
      it('returns false', () => {
        runInAction(() => {
          tree.highlightPath = [1, 2, 3];
        });

        expect(alertStore.canBeVisibleAlert).toBe(false);
      });
    });

    describe('when there is no last highlighted category', () => {
      it('returns false', () => {
        runInAction(() => {
          alertStore.byCategoryId.set(
            2,
            createAlertDTO({
              id: 1,
              category: {
                id: 2,
                name: '2',
              },
            }),
          );

          tree.highlightPath = [];
        });

        expect(alertStore.canBeVisibleAlert).toBe(false);
      });
    });

    describe('when there are alerts and highlighted categories', () => {
      it('returns true', async () => {
        const externalHandler = () =>
          Promise.resolve([
            createAlertDTO({
              category: {
                id: 2,
                name: '2',
              },
            }),
          ]);
        alertStore.setExternalHandler(externalHandler);
        runInAction(() => {
          tree.highlightPath = [1, 2, 3];
        });

        await waitFor(() => {
          expect(alertStore.canBeVisibleAlert).toBe(true);
        });
      });
    });
  });

  describe('.visibleAlert', () => {
    it('returns the nearest alert relatively to highlight path #1', async () => {
      const alert3 = createAlertDTO({
        id: 3,
        category: {
          id: 3,
          name: '3',
        },
      });
      const externalHandler: AlertLoadHandler = () => {
        return Promise.resolve([
          createAlertDTO({
            id: 2,
            category: {
              id: 2,
              name: '2',
            },
          }),
          alert3,
        ]);
      };
      alertStore.setExternalHandler(externalHandler);
      runInAction(() => {
        tree.highlightPath = [1, 2, 3, 4];
      });

      await waitFor(() => {
        expect(alertStore.visibleAlert).toEqual(alert3);
      });
    });

    it('returns the nearest alert relatively to highlight path #2', async () => {
      const alert2 = createAlertDTO({
        id: 2,
        category: {
          id: 2,
          name: '3',
        },
      });
      const externalHandler: AlertLoadHandler = () => {
        return Promise.resolve([
          alert2,
          createAlertDTO({
            id: 3,
            category: {
              id: 3,
              name: '3',
            },
          }),
        ]);
      };
      alertStore.setExternalHandler(externalHandler);
      runInAction(() => {
        tree.highlightPath = [3, 1, 2, 5, 4];
      });

      await waitFor(() => {
        expect(alertStore.visibleAlert).toEqual(alert2);
      });
    });

    describe('when no loaded alerts', () => {
      it('returns undefined', async () => {
        const externalHandler: AlertLoadHandler = () => {
          return Promise.resolve([]);
        };
        alertStore.setExternalHandler(externalHandler);
        runInAction(() => {
          tree.highlightPath = [3, 1, 2, 5, 4];
        });

        await waitFor(() => {
          expect(alertStore.visibleAlert).toBe(undefined);
        });
      });
    });
  });

  describe('.hasRelatedAlerts', () => {
    describe('when loaded alerts amount is less than 1', () => {
      it('return false', async () => {
        const externalHandler = () =>
          Promise.resolve([
            createAlertDTO({
              id: 1,
              category: {
                id: 2,
                name: '2',
              },
            }),
          ]);
        alertStore.setExternalHandler(externalHandler);
        runInAction(() => {
          tree.highlightPath = [1, 2, 3];
        });

        await waitFor(() => {
          expect(alertStore.hasRelatedAlerts).toBe(false);
        });
      });
    });

    describe('when only one category is highlighted', () => {
      it('returns false', async () => {
        const externalHandler = () =>
          Promise.resolve([
            createAlertDTO({
              id: 1,
              category: {
                id: 2,
                name: '2',
              },
            }),
            createAlertDTO({
              id: 2,
              category: {
                id: 2,
                name: '2',
              },
            }),
          ]);
        alertStore.setExternalHandler(externalHandler);
        runInAction(() => {
          tree.highlightPath = [2];
        });

        await waitFor(() => {
          expect(alertStore.hasRelatedAlerts).toBe(false);
        });
      });
    });

    describe('when there are highlighted categories and alerts', () => {
      it('returns true', async () => {
        const externalHandler = () =>
          Promise.resolve([
            createAlertDTO({
              id: 1,
              category: {
                id: 2,
                name: '2',
              },
            }),
            createAlertDTO({
              id: 2,
              category: {
                id: 3,
                name: '3',
              },
            }),
          ]);
        alertStore.setExternalHandler(externalHandler);
        runInAction(() => {
          tree.highlightPath = [1, 2, 3];
        });

        await waitFor(() => {
          expect(alertStore.hasRelatedAlerts).toBe(true);
        });
      });
    });
  });

  describe('.relatedAlerts', () => {
    describe(`when it doesn't have related alerts`, () => {
      it('returns empty list', async () => {
        const externalHandler = () => Promise.resolve([]);
        alertStore.setExternalHandler(externalHandler);
        runInAction(() => {
          tree.highlightPath = [];
        });

        await waitFor(() => {
          expect(alertStore.relatedAlerts).toEqual([]);
        });
      });
    });

    it('returns alerts in order of highlight path', async () => {
      const alert2 = createAlertDTO({
        id: 2,
        category: {
          id: 2,
          name: '2',
        },
      });
      const alert1 = createAlertDTO({
        id: 1,
        category: {
          id: 1,
          name: '1',
        },
      });
      const externalHandler = () => Promise.resolve([alert2, alert1]);
      alertStore.setExternalHandler(externalHandler);
      runInAction(() => {
        tree.highlightPath = [4, 1, 3, 2];
      });

      await waitFor(() => {
        expect(alertStore.relatedAlerts).toEqual([alert1, alert2]);
      });
    });
  });
});
