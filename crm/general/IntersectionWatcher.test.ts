import intersectionWatcher from './IntersectionWatcher';
import intersectionObserverMock from './__mock__/intersectionObserverMock';

window.IntersectionObserver = intersectionObserverMock;

const observableNode = document.createElement('div');

describe('intersectionWatcher', () => {
  let observer;
  beforeEach(() => {
    observer = intersectionWatcher.addObservable('test_name', observableNode);
  });
  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('.addObservable', () => {
    let observer2;
    let observableTestNode;
    beforeEach(() => {
      observableTestNode = document.createElement('div');
      observer2 = intersectionWatcher.addObservable('test_name1', observableTestNode);
    });
    afterAll(() => {
      intersectionWatcher.removeObserver(observer2.name);
      jest.clearAllMocks();
    });

    describe('when observer is not exists', () => {
      it('creates observer', () => {
        expect(intersectionWatcher.getObserverByName('test_name1')).toEqual(observer2);
      });
      it('adds observable', () => {
        expect(intersectionWatcher.findObservableInObserver(observer2, observableTestNode));
      });
    });
    describe('when observer is exists', () => {
      it('adds observable', () => {
        expect(intersectionWatcher.findObservableInObserver(observer2, observableTestNode));
      });
    });
  });

  describe('.removeObservable', () => {
    describe('when there is such observable', () => {
      beforeEach(() => {
        const observerInstance = intersectionWatcher.getObserverByName('test_name')?.instance;
        intersectionWatcher.removeObservable('test_name', observableNode);
        expect(observerInstance.unobserve).toBeCalledTimes(1);
      });
      it('removes it', () => {
        expect(
          observer.observableCollection.find((item) => item.target === observableNode),
        ).toEqual(undefined);
      });
      describe('when observable collection is empty', () => {
        it('removes observer', () => {
          expect(intersectionWatcher.getObserverByName(observer.name)).toEqual(undefined);
        });
      });
    });
    describe('when there is no such observable', () => {
      it('does not throw error', () => {
        expect(() =>
          intersectionWatcher.removeObservable('not-exists', observableNode),
        ).not.toThrow();
      });
    });
  });

  describe('.getObserverByName', () => {
    describe('when there is such observer', () => {
      it('returns observer', () => {
        expect(intersectionWatcher.getObserverByName('test_name')).toEqual(observer);
      });
    });
    describe('when there is no such observer', () => {
      it('returns undefined', () => {
        expect(intersectionWatcher.getObserverByName('test_name1')).toEqual(undefined);
      });
    });
  });

  describe('.getObserverByInstance', () => {
    describe('when there is such observer', () => {
      it('returns observer', () => {
        expect(intersectionWatcher.getObserverByInstance(observer.instance)).toEqual(observer);
      });
    });
    describe('when there is no such observer', () => {
      it('returns undefined', () => {
        const testInstance = new IntersectionObserver(() => {});
        expect(intersectionWatcher.getObserverByInstance(testInstance)).toEqual(undefined);
      });
    });
  });

  describe('.findObservableInObserver', () => {
    describe('when observable is found', () => {
      it('returns observable', () => {
        const observableTestNode = intersectionWatcher.findObservableInObserver(
          observer,
          observableNode,
        );
        expect(observableTestNode).not.toEqual(undefined);
        // @ts-ignore
        expect(observableTestNode.target).toEqual(observableNode);
      });
    });
    describe('when observable is not found', () => {
      it('returns undefined', () => {
        const observableFakeNode = document.createElement('div');
        const observableTestNode = intersectionWatcher.findObservableInObserver(
          observer,
          observableFakeNode,
        );
        expect(observableTestNode).toEqual(undefined);
      });
    });
  });
});
