import * as React from 'react';

export interface IProps {
    text: string;
    bottomElem: React.ReactNode;
}

export function Test(props: IProps) {
    return (
        <div>
            {props.text}
            {props.bottomElem}
        </div>
    );
}
