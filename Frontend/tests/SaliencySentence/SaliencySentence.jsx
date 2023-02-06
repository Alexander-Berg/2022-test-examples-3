import React, { useEffect, useState } from 'react';
import { cn } from '@bem-react/classname';
import chroma from 'chroma-js';
import './SaliencySentence.css';

const b = cn('SaliencySentence');

const preparedPalette = chroma
    .cubehelix()
    .start(200)
    .rotations(-0.35)
    .gamma(0.7)
    .lightness([0.3, 0.8])
    .scale()
    .correctLightness();

const getColor = (saliency, tokensIndex, palette) => {
    if (saliency === 0) {
        return 'transparent';
    }

    const idx = tokensIndex.indexOf(saliency);

    return palette[idx] || '#fff';
};

export const SaliencySentence = ({
    tokens,
    saliencyInfo,
    hideText,
    noLeading,
}) => {
    const [hoveredToken, setHoveredToken] = useState(tokens.length);
    const [saliencyPalette, setSaliencyPalette] = useState([]);

    useEffect(() => {
        setHoveredToken(tokens.length);
        setSaliencyPalette(preparedPalette.colors(tokens.length));
    }, [tokens]);

    const saliency = saliencyInfo[hoveredToken] || [];
    const tokensIndex = Array.from(new Set(saliency)).sort((a, b) => b - a);

    return (
        <div
            className={ b({ noLeading }) }
            onMouseLeave={ () => setHoveredToken(tokens.length) }
        >
            { tokens.map((token, idx) =>
                token === '[NL]' ?
                    <br key={ idx } /> :
                    <span
                        className={ b('Token', { hideText }) }
                        key={ idx }
                        style={{
                            background: getColor(saliency[idx], tokensIndex, saliencyPalette),
                        }}
                        onMouseEnter={ () => setHoveredToken(idx + 1) }
                    >
                        { token }
                    </span>,
            )
            }
        </div>
    );
};
