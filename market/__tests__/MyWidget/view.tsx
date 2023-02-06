import React, {Fragment} from 'react';
import {connect} from '@yandex-market/apiary';

import type {Item, Data, Collections} from '.';

type Props = {
    items: Item[];
    isRobot: boolean;
    touches: number;
    onClick: () => void;
    onTouchClick: () => void;
};

function mapDataToProps(data: Data, collections: Collections) {
    return {
        isRobot: data.isRobot,
        items: data.items.map(id => collections.list[id.toString()]),
        touches: data.touches,
    };
}

const mapDispatchToProps = {
    onClick: () => ({type: '#ONE'}),
    onTouchClick: () => ({type: '#TOUCH'}),
};

const View: React.FC<Props> = (props: Props) => {
    const {items, isRobot, touches, onClick, onTouchClick} = props;
    const list = items.map(({id, name}) => (
        <li key={id} role="item">
            {name}
        </li>
    ));

    return (
        <Fragment>
            <div role="is-robot">{isRobot ? 'robot' : 'not robot'}</div>
            <button role="my-button" onClick={onClick}>
                Click
            </button>
            <ul>{list}</ul>
            <button role="touch-button" onClick={onTouchClick}>
                Touch
            </button>
            <div role="touches">{touches}</div>
        </Fragment>
    );
};

// @ts-ignore
export default connect(mapDataToProps, mapDispatchToProps)(View);
