import React, {Fragment} from 'react';
import {connect} from '@yandex-market/apiary';

import type {Data} from '.';

type Props = {
    content: string;
    onClick: () => void;
    onSecondButtonClick: () => void;
};

function mapDataToProps(data: Data) {
    return {
        content: data.content,
    };
}

const mapDispatchToProps = {
    onClick: () => ({type: '#ONE'}),
    onSecondButtonClick: () => ({type: '#THREE'}),
};

const View: React.FC<Props> = (props: Props) => {
    const {onClick, onSecondButtonClick, content} = props;

    return (
        <Fragment>
            <button role="my-button" onClick={onClick}>
                Click
            </button>
            <button role="my-second-button" onClick={onSecondButtonClick}>
                Click again
            </button>
            <div role="content">{content}</div>
        </Fragment>
    );
};

// @ts-ignore
export default connect(mapDataToProps, mapDispatchToProps)(View);
