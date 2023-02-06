import * as React from 'react';
import { cn } from '@yandex-turbo/core/cn';
import { compose } from '@yandex-turbo/core/hoc';
import { bind } from '@yandex-turbo/core/decorators/bind';
import { withDisplayName } from '@yandex-turbo/components/withDisplayName/withDisplayName';
import { withVisibilityProvider } from '@yandex-turbo/components/withVisibilityProvider/withVisibilityProvider';

import { IInjectedLazyProps, withLazyLoad, ELazyTypes } from '@yandex-turbo/components/withLazyLoad/withLazyLoad';

import './TestLazyLoad.scss';

export interface IProps {
    type: string;
}

interface IState {
    status: EStatus;
}

enum EStatus {
    WAITING = 'WAITING',
    LOADING = 'LOADING',
    LOADED = 'LOADED'
}

const colors = {
    [EStatus.WAITING]: 'red',
    [EStatus.LOADING]: 'yellow',
    [EStatus.LOADED]: 'green',
};

const cls = cn('lazy-load-test');

export class TestLazyLoadBlock extends React.PureComponent<IProps & IInjectedLazyProps, IState> {
    public state: IState = {
        status: EStatus.WAITING,
    };

    public componentDidUpdate(prevProps: Readonly<IProps & IInjectedLazyProps>): void {
        if (!prevProps.shouldLoad && this.props.shouldLoad) {
            this.setState({
                status: EStatus.LOADING,
            }, this.startLoading);
        }
    }

    public render() {
        const { visibilityRootRef, type } = this.props;
        const { status } = this.state;
        const color = colors[status];

        return (
            <div
                className={cls({ color, type })}
                ref={visibilityRootRef}
            >
                {type}
            </div>
        );
    }

    @bind
    private startLoading() {
        const time = Math.floor(Math.random() * 3000) + 1500;

        setTimeout(() => {
            this.setState({
                status: EStatus.LOADED,
            }, this.props.onLoadEnd);
        }, time);
    }
}

export const TestLazyLoadEmbed = compose<IProps>(
    withDisplayName('TestLazyLoadEmbed'),
    withVisibilityProvider(),
    withLazyLoad(ELazyTypes.EMBED)
)(TestLazyLoadBlock);

export const TestLazyLoadAdvert = compose<IProps>(
    withDisplayName('TestLazyLoadAdvert'),
    withVisibilityProvider(),
    withLazyLoad(ELazyTypes.ADVERT)
)(TestLazyLoadBlock);
