import * as React from 'react';
import { omit } from 'lodash/fp';
import * as scroll from '@yandex-turbo/core/utils/scroll';
import { shallow, render } from 'enzyme';
import { withBWMScrollTo, IInjectedProps } from '../withBWMScrollTo';

interface IProps {
    prop1?: string;
    prop2?: string;
}

class TestComponent extends React.Component<IProps & IInjectedProps> {
    render() {
        this.props.scrollTo!('.test');

        return <div />;
    }
}

describe('withBWMScrollTo', () => {
    it('хок должен прокидывать пропсы в основную компоненту и инжектить метод scrollTo', () => {
        const Component = withBWMScrollTo<IProps>()(TestComponent);
        const props = shallow(<Component prop1="one" prop2="two" />).props();
        const passedProps = omit('scrollTo', props);

        expect(passedProps).toEqual({ prop1: 'one', prop2: 'two' });
        expect(typeof props.scrollTo).toEqual('function');
    });

    describe('scrollTo ф-ия', () => {
        const querySelector = document.querySelector;
        const fakeQuerySelector = jest.fn();
        const fakeScrollToElement = jest.spyOn(scroll, 'scrollToElement');

        beforeEach(() => {
            fakeScrollToElement.mockReturnValue();
            document.querySelector = fakeQuerySelector;
        });

        afterEach(() => {
            fakeQuerySelector.mockClear();
            fakeScrollToElement.mockClear();
            document.querySelector = querySelector;
            delete History.prototype.scrollRestoration;
            delete history.scrollRestoration;
        });

        it('должна скролить к перданному блоку и препятствовать возврату скрола к предыдущему состоянию', () => {
            const Component = withBWMScrollTo<IProps>()(TestComponent);

            fakeQuerySelector.mockReturnValue('element');
            History.prototype.scrollRestoration = 'auto';

            render(<Component />);
            expect(fakeQuerySelector).toHaveBeenCalledTimes(1);
            expect(fakeScrollToElement).toHaveBeenCalledWith('element');
            expect(fakeScrollToElement).toHaveBeenCalledTimes(1);
            expect(history.scrollRestoration).toEqual('manual');
        });

        it('должна не вызывать прокрутку к указанному элементу если элемента нет в DOM и не востанавливать скрол если ' +
            'нет поддержки scrollRestoration', () => {
            const Component = withBWMScrollTo<IProps>()(TestComponent);

            fakeQuerySelector.mockReturnValue(undefined);
            render(<Component />);
            expect(fakeQuerySelector).toHaveBeenCalledTimes(1);
            expect(fakeScrollToElement).not.toHaveBeenCalled();
            expect(history.scrollRestoration).toEqual(undefined);
        });
    });
});
