import React from 'react';
import { ChangeEvent, FC } from 'react';
import { ExtractProps, compose } from '@bem-react/core';
import { mount, ReactWrapper } from 'enzyme';
import { act } from 'react-dom/test-utils';
import { withExcludedRobots } from './_withExcluded/Suggest_withExcluded_robots';
import { ToolsSuggestItem, ToolsSuggestItems, ToolsSuggestProps } from './Suggest.lib';

import { Suggest } from './Suggest';
import { withDefaults } from './_withDefaults/Suggest_withDefaults';
import { withDataProvider } from './_withDataProvider/Suggest_withDataProvider';
import { withHandlers } from './_withHandlers/Suggest_withHandlers';
import { withTypeStaff } from './_type/Suggest_type_staff';

const MockComponent: FC<ToolsSuggestProps> = (props: ToolsSuggestProps) => {
    const onChange = ({ target }: ChangeEvent) => {
        // @ts-ignore "Property 'value' does not exist on type 'EventTarget & Element'"
        props.onValueChange && props.onValueChange(target.value);
    };
    return (
        <input id="mockComponent" data-attrs={props} onChange={onChange} />
    );
};

jest.mock('@yandex-int/tools-components/ToolsSuggest/desktop/bundle', () => {
    return {
        __esModule: true,
        ToolsSuggest(props: ToolsSuggestProps) { return <MockComponent {...props} /> },
    };
});

const SuggestWithDefaults = withDefaults(MockComponent);
const SuggestWithDataProvider = withDataProvider(MockComponent);
const SuggestWithHandlers = withHandlers(MockComponent);
const SuggestWithExcludedRobots = withExcludedRobots(MockComponent);
const SuggestTypeStaff = withTypeStaff(MockComponent);

type ISuggest = ExtractProps<typeof Suggest>;
type ISuggestWithDefaults = ExtractProps<typeof SuggestWithDefaults>;
type ISuggestWithDataProvider = ExtractProps<typeof SuggestWithDataProvider>;
type ISuggestWithExcludedRobots = ExtractProps<typeof SuggestWithExcludedRobots>;

describe('Suggest', () => {
    const choices: ToolsSuggestItems = [
        { id: 'test1' },
        { id: 'test2' },
    ];

    const chosen: ToolsSuggestItems = [
        { id: 'test1' },
        { id: 'test2' },
    ];

    describe('SuggestWithDefaults', () => {
        const dataProvider: ISuggestWithDefaults['dataProvider'] = (
            text: string,
        ) => Promise.resolve([
            { id: `${text}1` },
            { id: `${text}2` },
        ]);

        it('Should render component without defaults', done => {
            const props = {
                onChosenChange: jest.fn(),
                dataProvider,
                defaults: [],
            };

            let wrapper: ReactWrapper;
            act(() => {
                wrapper = mount(<SuggestWithDefaults {...props} />);
            });

            setTimeout(() => {
                wrapper.update();
                expect(wrapper).toMatchSnapshot();

                expect(props.onChosenChange).toBeCalledTimes(1);
                expect(props.onChosenChange).toBeCalledWith([]);

                wrapper.unmount();
                done();
            }, 300);
        });

        it('Should render component with defaults', done => {
            const props = {
                defaults: ['first', 'second'],
                dataProvider,
                onChosenChange: jest.fn(),
            };

            let wrapper: ReactWrapper;
            act(() => {
                wrapper = mount(<SuggestWithDefaults {...props} />);
            });

            setTimeout(() => {
                wrapper.update();
                expect(wrapper).toMatchSnapshot();

                expect(props.onChosenChange).toBeCalledTimes(1);
                expect(props.onChosenChange).toBeCalledWith([{ id: 'first1' }, { id: 'second1' }]);

                wrapper.unmount();
                done();
            }, 300);
        });

        it('Should not fail with rejected promise', done => {
            const dataProvider: ISuggestWithDefaults['dataProvider'] = (
                _text: string,
            ) => Promise.reject();
            const props = {
                defaults: ['first', 'second'],
                dataProvider,
                chosen: [],
                onChosenChange: jest.fn(),
            };

            let wrapper: ReactWrapper;
            act(() => {
                wrapper = mount(<SuggestWithDefaults {...props} />);
            });

            setTimeout(() => {
                wrapper.update();
                expect(wrapper).toMatchSnapshot();

                expect(props.onChosenChange).toBeCalledTimes(1);
                expect(props.onChosenChange).toBeCalledWith(props.chosen);

                wrapper.unmount();
                done();
            }, 300);
        });

        it('Should not set defaults with other chosen items', done => {
            const props = {
                defaults: ['first', 'second'],
                dataProvider,
                chosen: [{ id: 'chosen1' }, { id: 'chosen2' }],
                onChosenChange: jest.fn(),
            };
            const expectedChosen = [
                { id: 'chosen1' },
                { id: 'chosen2' },
            ];

            let wrapper: ReactWrapper;
            act(() => {
                wrapper = mount(<SuggestWithDefaults {...props} />);
            });

            setTimeout(() => {
                wrapper.update();
                expect(wrapper).toMatchSnapshot();

                expect(props.onChosenChange).toBeCalledTimes(0);
                const componentProps = wrapper.find('input').prop('data-attrs') as ToolsSuggestProps;
                expect(componentProps.chosen).toStrictEqual(expectedChosen);

                wrapper.unmount();
                done();
            }, 300);
        });
    });

    describe('SuggestWithDataprovider', () => {
        const dataProvider: ISuggestWithDataProvider['dataProvider'] = (
            text: string,
        ) => Promise.resolve([
            { id: `${text}1` },
            { id: `${text}2` },
        ]);

        it('Should render simple component', () => {
            const wrapper = mount(<SuggestWithDataProvider dataProvider={dataProvider} />);

            expect(wrapper).toMatchSnapshot();

            wrapper.unmount();
        });

        it('Should change component props after onValueChange call', done => {
            const wrapper = mount(<SuggestWithDataProvider dataProvider={dataProvider} />);

            const expectedProps = {
                choices: choices,
                loading: false,
            };

            wrapper.find('input').simulate('change', {
                target: { value: 'test' },
            });

            setTimeout(() => {
                wrapper.update();

                const componentProps = wrapper.find('input').prop('data-attrs') as ToolsSuggestProps;

                expect(wrapper).toMatchSnapshot();
                expect(componentProps).toMatchObject(expectedProps);

                wrapper.unmount();
                done();
            }, 300);
        });

        it('Should not crash on dataProvider rejected promise', done => {
            const dataProviderReject: ISuggestWithDataProvider['dataProvider'] = (
                _text: string,
            ) => Promise.reject({});

            const wrapper = mount(<SuggestWithDataProvider dataProvider={dataProviderReject} />);

            const expectedProps = {
                choices: [],
                loading: false,
            };

            wrapper.find('input').simulate('change', {
                target: { value: 'test' },
            });

            setTimeout(() => {
                wrapper.update();

                const componentProps = wrapper.find('input').prop('data-attrs') as ToolsSuggestProps;

                expect(wrapper).toMatchSnapshot();
                expect(componentProps).toMatchObject(expectedProps);

                wrapper.unmount();
                done();
            }, 300);
        });

        it('Should set loading state while fetching', done => {
            const dataProviderLoading: ISuggestWithDataProvider['dataProvider'] = (
                _text: string,
            ) => new Promise(() => { });

            const wrapper = mount(<SuggestWithDataProvider dataProvider={dataProviderLoading} />);

            const expectedProps = {
                choices: [],
                loading: true,
            };

            wrapper.find('input').simulate('change', {
                target: { value: 'test' },
            });

            setTimeout(() => {
                wrapper.update();

                const componentProps = wrapper.find('input').prop('data-attrs') as ToolsSuggestProps;

                expect(wrapper).toMatchSnapshot();
                expect(componentProps).toMatchObject(expectedProps);

                wrapper.unmount();
                done();
            }, 300);
        });
    });

    describe('SuggestWithHandlers', () => {
        const originalFetch = global.fetch;
        beforeAll(() => {
            global.fetch = jest.fn();
        });
        afterAll(() => {
            global.fetch = originalFetch;
        });

        it('Should render simple component', () => {
            const wrapper = mount(<SuggestWithHandlers choices={choices} />);

            expect(wrapper).toMatchSnapshot();

            wrapper.unmount();
        });

        it('Should open suggest after onOpenedChange call', () => {
            const wrapper = mount(<SuggestWithHandlers choices={choices} />);

            let componentProps = wrapper.find('input').prop('data-attrs') as ToolsSuggestProps;

            expect(componentProps.opened).toBeFalsy();
            act(() => {
                componentProps.onOpenedChange && componentProps.onOpenedChange(true);
            });
            wrapper.update();
            componentProps = wrapper.find('input').prop('data-attrs') as ToolsSuggestProps;

            expect(componentProps.opened).toBeTruthy();

            wrapper.unmount();
        });

        it('Should open suggest after onValueChange call', () => {
            const onValueChange = jest.fn();
            const wrapper = mount(
                <SuggestWithHandlers
                    choices={choices}
                    onValueChange={onValueChange}
                />,
            );

            let componentProps = wrapper.find('input').prop('data-attrs') as ToolsSuggestProps;

            expect(componentProps.opened).toBeFalsy();
            act(() => {
                componentProps.onValueChange && componentProps.onValueChange('test');
            });
            wrapper.update();
            componentProps = wrapper.find('input').prop('data-attrs') as ToolsSuggestProps;

            expect(componentProps.opened).toBeTruthy();
            expect(onValueChange).toBeCalled();

            wrapper.unmount();
        });

        it('Should change picked items after onPickedChange call', () => {
            const picked = [{ id: 'test' }];
            const wrapper = mount(<SuggestWithHandlers />);

            let componentProps = wrapper.find('input').prop('data-attrs') as ToolsSuggestProps;

            expect(componentProps.picked).toEqual([]);
            act(() => {
                componentProps.onPickedChange && componentProps.onPickedChange(picked);
            });
            wrapper.update();
            componentProps = wrapper.find('input').prop('data-attrs') as ToolsSuggestProps;

            expect(componentProps.picked).toEqual(picked);

            wrapper.unmount();
        });

        it('Should change chosen items after onChosenChange call', () => {
            const onChosenChange = jest.fn();
            const wrapper = mount(
                <SuggestWithHandlers
                    choices={choices}
                    chosen={[]}
                    onChosenChange={onChosenChange}
                />,
            );

            let componentProps = wrapper.find('input').prop('data-attrs') as ToolsSuggestProps;

            expect(componentProps.chosen).toEqual([]);
            act(() => {
                componentProps.onChosenChange && componentProps.onChosenChange(chosen);
            });
            wrapper.update();

            expect(onChosenChange).toBeCalledWith(chosen);

            wrapper.unmount();
        });

        it('Should not call external onChosenChange with disabled items', () => {
            const onChosenChange = jest.fn();
            const checkDisabled = (item: ToolsSuggestItem) => item.id === 'test2';
            const wrapper = mount(
                <SuggestWithHandlers
                    checkDisabled={checkDisabled}
                    onChosenChange={onChosenChange}
                    chosen={[{ id: 'test1' }]}
                />,
            );

            let componentProps = wrapper.find('input').prop('data-attrs') as ToolsSuggestProps;

            expect(componentProps.chosen).toEqual([{ id: 'test1' }]);
            act(() => {
                componentProps.onChosenChange && componentProps.onChosenChange(chosen);
            });

            expect(onChosenChange).not.toBeCalled();

            wrapper.unmount();
        });

        it('Should send click after onChosenChange call', () => {
            const onChosenChange = jest.fn();
            const chosenLocal: ToolsSuggestItems = [
                { id: 'test1', clickUrls: ['url1'] },
                { id: 'test2', clickUrls: ['url2'] },
            ];
            const wrapper = mount(
                <SuggestWithHandlers
                    choices={choices}
                    chosen={[{ id: 'test1', clickUrls: ['url1'] }]}
                    onChosenChange={onChosenChange}
                />,
            );

            let componentProps = wrapper.find('input').prop('data-attrs') as ToolsSuggestProps;

            act(() => {
                componentProps.onChosenChange && componentProps.onChosenChange(chosenLocal);
            });
            wrapper.update();

            expect(global.fetch).toBeCalledTimes(1);
            expect(global.fetch).toBeCalledWith('url2');

            wrapper.unmount();
        });

        it('Should pass choices processed by handleChoices', () => {
            const handleChoices = (items: ToolsSuggestItems) => items.map(item => ({ handled: true, ...item }));
            const wrapper = mount(
                <SuggestWithHandlers
                    choices={choices}
                    handleChoices={handleChoices}
                />,
            );

            const componentProps = wrapper.find('input').prop('data-attrs') as ToolsSuggestProps;
            const expectedChoices = [
                { id: 'test1', handled: true },
                { id: 'test2', handled: true },
            ];

            expect(componentProps.choices).toEqual(expectedChoices);

            wrapper.unmount();
        });
    });

    describe('SuggestWithExcludedRobots', () => {
        const robot = { id: 'robot', isRobot: true };
        const person = { id: 'person' };
        const dismissed = { id: 'dismissed', isDismissed: true };

        it('Should render simple component', () => {
            const wrapper = mount(<SuggestWithExcludedRobots excludeRobots />);

            expect(wrapper).toMatchSnapshot();

            wrapper.unmount();
        });

        it('Should apply internal checkDisabled and handleChoices when they are not passed', () => {
            const wrapper = mount(<SuggestWithExcludedRobots excludeRobots />);

            const expected = [
                { id: 'robot', isRobot: true, disabled: true },
                { id: 'person', disabled: false },
                { id: 'dismissed', isDismissed: true, disabled: false },
            ];

            const componentProps = wrapper.find('input').prop('data-attrs') as ISuggestWithExcludedRobots;

            act(() => {
                expect(componentProps.checkDisabled).toBeDefined();
                // На данном этапе функция уже должна быть определена, поэтому игнорим ошибку
                // @ts-ignore "Cannot invoke an object which is possibly 'undefined'."
                expect(componentProps.checkDisabled(robot)).toBeTruthy();
                // @ts-ignore
                expect(componentProps.checkDisabled(person)).toBeFalsy();
                // @ts-ignore
                expect(componentProps.checkDisabled(dismissed)).toBeFalsy();

                expect(componentProps.handleChoices).toBeDefined();
                // @ts-ignore
                expect(componentProps.handleChoices([robot, person, dismissed])).toEqual(expected);
            });

            wrapper.unmount();
        });

        it('Should apply internal and external checkDisabled and handleChoices', () => {
            const checkDisabled = (item: ToolsSuggestItem) => Boolean(item.isDismissed);
            const handleChoices = (items: ToolsSuggestItems) => items.map(item => ({ handled: true, ...item }));
            const wrapper = mount(
                <SuggestWithExcludedRobots
                    excludeRobots
                    checkDisabled={checkDisabled}
                    handleChoices={handleChoices}
                />,
            );

            const expected = [
                { id: 'robot', isRobot: true, disabled: true, handled: true },
                { id: 'person', disabled: false, handled: true },
                { id: 'dismissed', isDismissed: true, disabled: true, handled: true },
            ];

            const componentProps = wrapper.find('input').prop('data-attrs') as ISuggestWithExcludedRobots;

            act(() => {
                expect(componentProps.checkDisabled).toBeDefined();
                // На данном этапе функция уже должна быть определена, поэтому игнорим ошибку
                // @ts-ignore "Cannot invoke an object which is possibly 'undefined'."
                expect(componentProps.checkDisabled(robot)).toBeTruthy();
                // @ts-ignore
                expect(componentProps.checkDisabled(person)).toBeFalsy();
                // @ts-ignore
                expect(componentProps.checkDisabled(dismissed)).toBeTruthy();

                expect(componentProps.handleChoices).toBeDefined();
                // @ts-ignore
                expect(componentProps.handleChoices([robot, person, dismissed])).toEqual(expected);
            });

            wrapper.unmount();
        });
    });

    describe('SuggestTypeStaff', () => {
        it('Should render simple component', () => {
            const wrapper = mount(<SuggestTypeStaff type="staff" />);

            expect(wrapper).toMatchSnapshot();

            wrapper.unmount();
        });

        it('Should pass props to base component', () => {
            const props = {
                prop1: 'val1',
                prop2: 'val2',
            };

            const wrapper = mount(<SuggestTypeStaff type="staff" { ...props } />);

            expect(wrapper).toMatchSnapshot();

            wrapper.unmount();
        });
    });

    describe('Suggest', () => {
        // Тесты, зависящие от порядка подключения модификаторов

        const PeopleSuggest = compose(
            withExcludedRobots,
        )(Suggest);

        const dataProvider: ISuggest['dataProvider'] = (
            text: string,
        ) => Promise.resolve([
            { id: 'robot', isRobot: true },
            { id: 'login' },
        ].filter(x => x.id === text));

        it('Should select default option', done => {
            const props = {
                onChosenChange: jest.fn(),
                dataProvider: jest.fn(dataProvider),
                defaults: ['robot', 'login'],
            };

            let wrapper: ReactWrapper;
            act(() => {
                wrapper = mount(<PeopleSuggest excludeRobots {...props} />);
            });

            setTimeout(() => {
                wrapper.update();

                expect(props.onChosenChange).toBeCalledTimes(1);
                expect(props.onChosenChange).toBeCalledWith([{ id: 'login' }]);

                wrapper.unmount();
                done();
            }, 300);
        });

        it('Should not select disabled options as default', done => {
            const props = {
                onChosenChange: jest.fn(),
                dataProvider: jest.fn(dataProvider),
                defaults: ['robot'],
            };

            let wrapper: ReactWrapper;
            act(() => {
                wrapper = mount(<PeopleSuggest excludeRobots {...props} />);
            });

            setTimeout(() => {
                wrapper.update();

                expect(props.onChosenChange).toBeCalledTimes(1);
                expect(props.onChosenChange).toBeCalledWith([]);

                wrapper.unmount();
                done();
            }, 300);
        });
    });
});
