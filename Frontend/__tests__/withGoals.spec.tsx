import * as React from 'react';
import { shallow, render } from 'enzyme';
import { GAnalyticsGoalsArray } from '@yandex-turbo/core/goals';
import { IInjectedGoalsProps, withGoals } from '../withGoals';

class Button extends React.Component<IInjectedGoalsProps> {
    public render() {
        const { goals } = this.props;

        return (
            <button {...goals}>With Goals</button>
        );
    }
}

const metrikaFromHOK = {
    '46417413': ['first-goal-from-hok', 'second-goal-from-hok'],
};
const metrikaFromProps = {
    '': ['first-goal-from-props'],
    '46417413': ['second-goal-from-props'],
    '46417414': ['third-goal-from-props'],
};
const mergedMetrika = {
    '': ['first-goal-from-props'],
    '46417413': ['first-goal-from-hok', 'second-goal-from-hok', 'second-goal-from-props'],
    '46417414': ['third-goal-from-props'],
};

describe('HOC withGoals для подключения компонента к Яндекс.метрике', () => {
    it('Обернутому компоненту добавляются props, необходимые для метрики, из хока', () => {
        const ButtonWithGoals = withGoals({ yandex: metrikaFromHOK })(Button);
        const buttonWithGoalsHok = shallow<Button>(<ButtonWithGoals />);

        const dataMetrikaGoal = JSON.stringify(metrikaFromHOK);
        const buttonProps = buttonWithGoalsHok.props();

        expect(buttonProps.goals && buttonProps.goals['data-metrika-goals']).toEqual(dataMetrikaGoal);
    });

    it('Обернутому компоненту добавляются props, необходимые для метрики, из пропсов', () => {
        const ButtonWithGoals = withGoals()(Button);
        const buttonWithGoalsHok = shallow<Button>(<ButtonWithGoals yaMetrikaGoals={metrikaFromProps} />);

        const dataMetrikaGoal = JSON.stringify(metrikaFromProps);
        const buttonProps = buttonWithGoalsHok.props();

        expect(buttonProps.goals && buttonProps.goals['data-metrika-goals']).toEqual(dataMetrikaGoal);
    });

    it('Обернутому компоненту добавляются props, необходимые для метрики, из хока и пропсов', () => {
        const ButtonWithGoals = withGoals({ yandex: metrikaFromHOK })(Button);
        const buttonWithGoalsHok = shallow<Button>(<ButtonWithGoals yaMetrikaGoals={metrikaFromProps} />);

        const dataMetrikaGoal = JSON.stringify(mergedMetrika);
        const buttonProps = buttonWithGoalsHok.props();

        expect(buttonProps.goals && buttonProps.goals['data-metrika-goals']).toEqual(dataMetrikaGoal);
    });
});

const analyticsFromHOK = [{ category: 'first-from-hok', action: 'click' }];
const analyticsFromProps = [{ category: 'first-from-props', action: 'click' }];

describe('HOC withGoals для подключения компонента к Гугл.аналитике', () => {
    it('Обернутому компоненту добавляются props, необходимые для анатилики, из хока', () => {
        const ButtonWithGoals = withGoals({ google: analyticsFromHOK })(Button);
        const buttonWithGoalsHok = shallow<Button>(<ButtonWithGoals />);

        const dataAnalyticsGoals = JSON.stringify(analyticsFromHOK);
        const buttonProps = buttonWithGoalsHok.props();

        expect(buttonProps.goals && buttonProps.goals['data-analytics-goals']).toEqual(dataAnalyticsGoals);
    });

    it('Обернутому компоненту добавляются props, необходимые для метрики, из пропсов', () => {
        const ButtonWithGoals = withGoals()(Button);
        const buttonWithGoalsHok = shallow<Button>(<ButtonWithGoals gAnalyticsGoals={analyticsFromProps} />);

        const dataAnalyticsGoals = JSON.stringify(analyticsFromProps);
        const buttonProps = buttonWithGoalsHok.props();

        expect(buttonProps.goals && buttonProps.goals['data-analytics-goals']).toEqual(dataAnalyticsGoals);
    });

    it('Обернутому компоненту добавляются props, необходимые для метрики, из хока и пропсов', () => {
        const ButtonWithGoals = withGoals({ google: analyticsFromHOK })(Button);
        const buttonWithGoalsHok = shallow<Button>(<ButtonWithGoals gAnalyticsGoals={analyticsFromProps} />);

        const dataAnalyticsGoals = JSON.stringify(([] as GAnalyticsGoalsArray).concat(analyticsFromHOK, analyticsFromProps));
        const buttonProps = buttonWithGoalsHok.props();

        expect(buttonProps.goals && buttonProps.goals['data-analytics-goals']).toEqual(dataAnalyticsGoals);
    });
});

describe('HOC withGoals без целей', () => {
    it('Если в данных нет целей - приходит пустой goals', () => {
        const ButtonWithGoals = withGoals({})(Button);
        const buttonWithGoalsHok = shallow<Button>(<ButtonWithGoals />);

        const buttonProps = buttonWithGoalsHok.props();

        expect(buttonProps.goals).toEqual({});
    });
});

describe('HOC withGoals проверка аттрибутов', () => {
    it('Добавляются аттрибуты, если цели есть', () => {
        const ButtonWithGoals = withGoals()(Button);
        const buttonWithGoalsHok = render(
            <ButtonWithGoals gAnalyticsGoals={analyticsFromProps} yaMetrikaGoals={metrikaFromProps} />
        );
        const dataAnalyticsGoals = JSON.stringify(analyticsFromProps);
        const dataMetrikaGoal = JSON.stringify(metrikaFromProps);

        expect(buttonWithGoalsHok.attr('data-analytics-goals')).toEqual(dataAnalyticsGoals);
        expect(buttonWithGoalsHok.attr('data-metrika-goals')).toEqual(dataMetrikaGoal);
    });

    it('Не добавляются аттрибуты, если целей нет', () => {
        const ButtonWithGoals = withGoals()(Button);
        const buttonWithGoalsHok = render(<ButtonWithGoals />);

        expect(buttonWithGoalsHok.attr('data-analytics-goals')).toEqual(undefined);
        expect(buttonWithGoalsHok.attr('data-metrika-goals')).toEqual(undefined);
    });
});
