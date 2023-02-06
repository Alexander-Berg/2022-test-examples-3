import React from 'react';
import block from 'propmods';
import { SyntaxHighlighter, github } from '../../lib/react-syntax-highlighter';

import './TestPageHistoryHighlighter.scss';

const b = block('TestPageHistoryHighlighter');

interface Props {
    data?: string | null;
    label: string;
    placeholder?: string;
}

export const TestPageHistoryHighlighter: React.FC<Props> = ({ data, label, placeholder }) => {
    return (
        <div>
            <h3 {...b('header')}>{label}</h3>
            <SyntaxHighlighter language="json" style={github}>
                {data || placeholder}
            </SyntaxHighlighter>
        </div>
    );
};
