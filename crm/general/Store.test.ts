import { Subject } from 'rxjs';
import {
  TargetMeta,
  CategoriesObject,
  Tabs,
  Search,
  Tree,
  Tip,
  ChangeStrategyType,
  Rating,
  TextHighlighting,
  AlertStore,
} from '../../../types';
import { Store } from './Store';
import { OneBranchStrategy } from '../Tree/ChangeStrategies';
import { CommentForm } from '../Tip/Rating/CommentForm';
import { Category } from '../Category';
import { BoolState } from '../BoolState';

describe('State/Store', () => {
  let mockSubject = new Subject<{
    event: string;
    payload: TargetMeta;
  }>();
  let store = new Store({
    emitter: mockSubject,
  });
  beforeEach(() => {
    store = new Store({
      emitter: mockSubject,
    });
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('.setup', () => {
    it('sets .targetMeta', () => {
      store.setup({
        targetMeta: {
          id: 1,
          type: 'Mail',
          description: 'test',
        },
      });

      expect(store.targetMeta).toStrictEqual({
        id: 1,
        type: 'Mail',
        description: 'test',
      });
    });

    it('sets .emitter', () => {
      const subject = new Subject<{
        event: string;
        payload: TargetMeta;
      }>();
      store.setup({
        targetMeta: {
          id: 1,
          type: 'Mail',
        },
        emitter: subject,
      });

      expect(store.emitter).toBe(subject);
    });

    it('sets .loadHandler', () => {
      const load = () => {
        return Promise.resolve({}) as Promise<CategoriesObject>;
      };

      store.setup({
        targetMeta: {
          id: 1,
          type: 'Mail',
        },
        load,
      });

      expect(store.loadHandler).toBe(load);
    });

    it('calls .tip.setExternalHandler', () => {
      const loadTip = () => Promise.resolve('');
      store.tip = {
        setExternalHandler: jest.fn(),
        loading: new BoolState(),
        runReactions: jest.fn(),
        load: jest.fn(),
        reset: () => {},
        setStores: () => {},
        rating: ({ reset: jest.fn() } as unknown) as Rating,
      };

      store.setup({
        targetMeta: {
          id: 1,
          type: 'Mail',
        },
        loadTip,
      });

      expect(store.tip.setExternalHandler).lastCalledWith(loadTip);
    });

    it('calls .search.setExternalHandler', () => {
      const search = () =>
        Promise.resolve({
          resultIds: [],
          highlightRangesById: {},
        });
      store.search = {
        setExternalHandler: jest.fn(),
        text: '',
        setText: jest.fn(),
        resultIds: [],
        loading: new BoolState(),
        error: new BoolState(),
        handler: () => {},
        reset: () => {},
        setStores: () => {},
        runReactions: () => [],
      };

      store.setup({
        targetMeta: {
          id: 1,
          type: 'Mail',
        },
        search,
      });

      expect(store.search.setExternalHandler).lastCalledWith(search);
    });

    it('sets .saveHandler', () => {
      const save = () => {};

      store.setup({
        targetMeta: {
          id: 1,
          type: 'Mail',
        },
        save,
      });

      expect(store.saveHandler).toBe(save);
    });

    it('sets .previewComponent', () => {
      const previewComponent = jest.fn(() => null);

      store.setup({
        targetMeta: {
          id: 1,
          type: 'Mail',
        },
        previewComponent,
      });

      (store.previewComponent as () => null)();

      expect(previewComponent).toBeCalled();
    });

    it('sets .changeStrategy', () => {
      const changeStrategy: ChangeStrategyType = 'one-branch';
      const mockSetChangeStrategy = jest.fn();
      store.tree.setChangeStrategy = mockSetChangeStrategy;

      store.setup({
        targetMeta: {
          id: 1,
          type: 'Mail',
        },
        changeStrategy,
      });

      expect(mockSetChangeStrategy).toBeCalledTimes(1);
      expect(mockSetChangeStrategy.mock.calls[0][0]).toBeInstanceOf(OneBranchStrategy);
    });

    it('calls .tip.rating.setExternalHandler', () => {
      const onCommentSubmit = jest.fn();
      store.tip = {
        setExternalHandler: jest.fn(),
        rating: {
          byId: {},
          commentForm: new CommentForm(),
          like: () => {},
          dislike: () => {},
          reset: () => {},
          runReactions: () => [],
          setStores: () => {},
          setExternalHandler: jest.fn(),
        },
        loading: new BoolState(),
        load: () => {},
        reset: () => {},
        setStores: () => {},
        runReactions: () => [],
      };

      store.setup({
        targetMeta: {
          id: 1,
          type: 'test',
        },
        commentSubmit: onCommentSubmit,
      });

      expect(store.tip.rating.setExternalHandler).lastCalledWith(onCommentSubmit);
    });
  });

  describe('.save', () => {
    beforeEach(() => {
      store.saveHandler = jest.fn();
    });

    it('calls .saveHandler with selected array', async () => {
      store.tree.byId = {
        1: new Category({
          id: 1,
          name: '1',
          isLeaf: false,
          items: [2],
        }),
        2: new Category({
          id: 2,
          parentId: 1,
          name: '2',
          isLeaf: false,
          items: [3, 4],
        }),
        3: new Category({
          id: 3,
          parentId: 2,
          name: '3',
          isLeaf: false,
        }),
        4: new Category({
          id: 4,
          parentId: 2,
          name: '4',
          isLeaf: false,
        }),
      };

      store.tree.valueAsTree.current = {
        1: {
          2: {
            3: {},
            4: {},
          },
        },
      };

      store.save();

      expect(store.saveHandler).lastCalledWith([{ id: 3 }, { id: 4 }]);
    });
  });

  describe('.open', () => {
    it('sets .openess.state and .fullness.state', () => {
      store.open();

      expect(store.openess.state).toBeTruthy();
      expect(store.fullness.state).toBeTruthy();
    });

    it('stops reactions initially', () => {
      store.stopReactions = jest.fn();

      store.open();

      expect(store.stopReactions).toBeCalledTimes(1);
    });

    it('runs reactions', () => {
      store.runReactions = jest.fn();

      store.open();

      expect(store.runReactions).toBeCalledTimes(1);
    });

    describe('when store has .loadHandler', () => {
      const byId = {};
      const root = [];
      const highlighted = [];
      const valueAsTree = {};
      const loadHandler = jest.fn(() =>
        Promise.resolve({
          byId,
          root,
          highlighted,
          valueAsTree,
        }),
      );

      beforeEach(() => {
        store.loadHandler = loadHandler;
      });

      it('calls .loadHandler', async () => {
        store.open();

        expect(store.loadHandler).toBeCalled();
      });
    });
  });

  describe('.reset', () => {
    it('resets everything', () => {
      const tree = ({ reset: jest.fn() } as unknown) as Tree;
      const search = ({ reset: jest.fn(), setStores: jest.fn() } as unknown) as Search;
      const tabs = ({ reset: jest.fn() } as unknown) as Tabs;
      const tip = ({ reset: jest.fn(), setStores: jest.fn() } as unknown) as Tip;
      const alertStore = ({ reset: jest.fn(), setStores: jest.fn() } as unknown) as AlertStore;
      const textHighlighting: TextHighlighting = {
        byId: {
          1: [],
        },
        setById: jest.fn(),
        reset: jest.fn(),
      };
      store = new Store({
        tree,
        search,
        tabs,
        tip,
        textHighlighting,
        alertStore,
      });
      store.reset();

      expect(store.tree.reset).toBeCalled();
      expect(store.search.reset).toBeCalled();
      expect(store.tabs.reset).toBeCalled();
      expect(store.tip.reset).toBeCalled();
      expect(store.textHighlighting.reset).toBeCalled();
      expect(store.alertStore.reset).toBeCalled();
      expect(store.openess.state).toBeFalsy();
    });
  });

  describe('.close', () => {
    it('calls .stopReactions', () => {
      store.stopReactions = jest.fn();

      store.close();

      expect(store.stopReactions).toBeCalledTimes(1);
    });

    it('calls .reset', () => {
      store.reset = jest.fn();

      store.close();

      expect(store.reset).toBeCalledTimes(1);
    });
  });
});
