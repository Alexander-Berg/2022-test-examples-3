import * as React from 'react';
import { shallow, ShallowWrapper } from 'enzyme';
import * as dateMock from 'jest-date-mock';

import { PersistActions, withPersistentData } from '.';
import localforage from '../.config/jest/__mocks__/localforage';

interface TestData {
    a: string;
    b: number;
    c: boolean;
}

interface TestComponentProps {
    data: TestData;
    onChange: (data: TestData) => void;
    onClear: () => void;
}

const TestComponent: React.FC<TestComponentProps> = () => {
    return <div />;
};

type DataProps = Pick<TestComponentProps, 'data'>;
type ActionProps = Pick<TestComponentProps, 'onChange' | 'onClear'>;

const mapDataToProps = (data: TestData): DataProps => {
    return {
        data,
    };
};

const mapActionsToProps = (actions: PersistActions): ActionProps => {
    return {
        onChange: data => actions.change(data),
        onClear: () => actions.clear(),
    };
};

const DEBOUNCE_TIME = 500;
const WrappedComponent = withPersistentData(mapDataToProps, mapActionsToProps, {
    instanceName: 'yndx-uslugi',
    key: 'test',
    debounceTime: DEBOUNCE_TIME,
})(TestComponent);

const testData: TestData = {
    a: 'test-string',
    b: 536,
    c: false,
};
const expectedResult = {
    updateTime: 0,
    value: testData,
};

let component: ShallowWrapper;
let testComponent: ShallowWrapper<TestComponentProps>;

describe('withPersistentData', () => {
    beforeEach(() => {
        localforage.setItem.mockClear();
        localforage.getItem.mockClear();
        localforage.removeItem.mockClear();

        dateMock.advanceTo();
    });

    afterEach(() => {
        dateMock.clear();
    });

    it('должен читать данные из хранилища после вставки компонента в DOM', async() => {
        component = shallow(<WrappedComponent />, {
            disableLifecycleMethods: true,
        });
        testComponent = component.find(TestComponent);

        localforage.setItem('test', expectedResult);

        expect(localforage.getItem).not.toHaveBeenCalled();
        expect(testComponent.prop('data')).toBeUndefined();

        const instance = component.instance();
        if (typeof instance.componentDidMount === 'function') {
            instance.componentDidMount();
        }

        expect(localforage.getItem).toHaveBeenCalledWith('test');

        await Promise.resolve();

        expect(component.state('data')).toEqual(expectedResult);

        testComponent = component.find(TestComponent);

        expect(testComponent.prop('data')).toEqual(testData);
    });

    it('должен сохранять данные при вызове onChange у компонента', done => {
        component = shallow(<WrappedComponent />);
        testComponent = component.find(TestComponent);

        testComponent.prop('onChange')(testData);

        dateMock.advanceBy(DEBOUNCE_TIME + 1);

        setTimeout(() => {
            expect(localforage.setItem).toHaveBeenCalledWith('test', {
                ...expectedResult,
                updateTime: 1,
            });
            done();
        }, 1000);
    });

    it('должен отрабатывать debounce', done => {
        component = shallow(<WrappedComponent />);
        testComponent = component.find(TestComponent);

        testComponent.prop('onChange')(testData);
        testComponent.prop('onChange')(testData);
        testComponent.prop('onChange')(testData);

        expect(localforage.setItem).not.toHaveBeenCalled();

        dateMock.advanceBy(DEBOUNCE_TIME + 1);

        setTimeout(() => {
            expect(localforage.setItem).toHaveBeenCalledTimes(1);
            done();
        }, 1000);
    });

    it('должен отрабатывать ttl', async() => {
        const WrappedWithTtl = withPersistentData(mapDataToProps, mapActionsToProps, {
            instanceName: 'yndx-uslugi',
            key: 'test',
            debounceTime: DEBOUNCE_TIME,
            ttl: 10,
        })(TestComponent);
        component = shallow(<WrappedWithTtl />);

        localforage.setItem('test', expectedResult);

        const instance = component.instance();

        if (typeof instance.componentDidMount === 'function') {
            instance.componentDidMount();
        }

        await Promise.resolve();

        expect(component.find(TestComponent).prop('data')).toEqual(testData);

        // Сначала перематываем на 5 сек, чтобы убедиться что данные не протухли
        dateMock.advanceBy(5000);

        component.instance().forceUpdate();

        expect(component.find(TestComponent).prop('data')).toEqual(testData);

        // Перематываем еще на 6 сек, чтобы убедиться что данные протухли
        dateMock.advanceBy(6000);

        component.instance().forceUpdate();

        expect(component.find(TestComponent).prop('data')).toBeUndefined();
    });

    it('должен очищать данные после вызова onClear', () => {
        component = shallow(<WrappedComponent />);
        testComponent = component.find(TestComponent);

        testComponent.prop('onClear')();

        expect(localforage.removeItem).toHaveBeenCalledTimes(1);
        expect(localforage.removeItem).toHaveBeenCalledWith('test');
    });
});
