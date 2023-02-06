import React from 'react';
import { Yaplus } from '../../desktop';

const styles = `
.Wrapper {
    padding: 20px;
}
`;

export const Simple = () => (
    <div className="Wrapper">
        <style>{styles}</style>
        <div className="Simple"><Yaplus /></div>
        <div className="Text"><Yaplus showText /></div>
    </div>
);
