import { LcEventAction, LcSectionType } from '@yandex-turbo/components/LcEvents/LcEvents.constants';
import lcActions from '../index';

describe('lcActions', () => {
    const defaultPage = {
        children: [
            {
                props: {
                    sectionId: 'qwerty',
                    children: [
                        {
                            props: {
                                anchor: 'anchor1',
                            },
                        },
                        {
                            props: {
                                sectionId: '',
                                anchor: 'anchor2',
                            },
                        },
                    ],
                },
            },
        ],
    };
    const defaultSectionsList = {
        '': { id: '', parentId: null, anchor: '', childrenIds: ['qwerty'] },
        qwerty: { id: 'qwerty', parentId: '', anchor: undefined, childrenIds: ['anchor1', 'anchor2'] },
        anchor1: { id: 'anchor1', parentId: 'qwerty', anchor: 'anchor1', childrenIds: [] },
        anchor2: { id: 'anchor2', parentId: 'qwerty', anchor: 'anchor2', childrenIds: [] },
    };

    beforeEach(() => {
        lcActions.buildSectionsList(defaultPage);
    });

    afterEach(() => {
        lcActions.sections = {};
    });

    describe('lcActions.buildSectionsList', () => {
        test('should build sections list', () => {
            lcActions.buildSectionsList(defaultPage);

            expect(lcActions.sections).toEqual(defaultSectionsList);
        });
    });

    describe('lcActions.setSectionData', () => {
        test('should set sections data', () => {
            const actions = {
                [LcEventAction.ChangeTab]: () => {},
            };
            lcActions.setSectionData('anchor1', LcSectionType.LcTabs, actions);

            expect(lcActions.sections.anchor1).toEqual({
                id: 'anchor1',
                anchor: 'anchor1',
                parentId: 'qwerty',
                type: LcSectionType.LcTabs,
                childrenIds: [],
                actions,
            });
        });
    });

    describe('lcActions.removeSectionData', () => {
        test('should remove sections data', () => {
            const actions = {
                [LcEventAction.ChangeTab]: () => {},
            };
            lcActions.setSectionData('anchor1', LcSectionType.LcTabs, actions);
            lcActions.removeSectionData('anchor1');

            expect(lcActions.sections.anchor1).toEqual({
                id: 'anchor1',
                anchor: 'anchor1',
                parentId: 'qwerty',
                childrenIds: [],
            });
        });
    });

    describe('lcActions.showSectionByAnchor', () => {
        test('should remove sections data', () => {
            const activateTab = jest.fn();
            const actions = {
                [LcEventAction.ChangeTab]: activateTab,
            };
            lcActions.setSectionData('qwerty', LcSectionType.LcTabs, actions);
            lcActions.showSectionByAnchor('anchor2');

            expect(activateTab).toHaveBeenCalledWith({ data: { index: 2 } });
        });
    });
});
