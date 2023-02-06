import * as React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';

const SLICE_FROM = 0;
const HASH_LENGTH = 6;

/*eslint-disable @typescript-eslint/no-magic-numbers*/
function hashCode(str: string): number {
    let hash = 0;

    for (let i = 0; i < str.length; i++) {
        const character = str.charCodeAt(i);

        hash = (hash << 5) - hash + character;
        hash = hash & hash; // Convert to 32bit integer
    }

    return hash;
}

function colorHash(str: string): string {
    return hashCode(str).toString(16).slice(SLICE_FROM, HASH_LENGTH);
}

export interface BuildDummyImageSVGOptions {
    width?: number;
    height?: number;
    patternSize?: number;
    color?: string;
    origUrl?: string;
}

export function buildDummyImageSVG(props: BuildDummyImageSVGOptions): string {
    let { width = 100, height = 100, patternSize = 10, color = '#000', origUrl } = props;

    if (color[0] !== '#') {
        color = '#' + color;
    }

    if (origUrl) {
        if (/^data/.test(origUrl)) {
            return origUrl;
        }

        color = '#' + colorHash(origUrl);
    }

    return renderToStaticMarkup(
        <svg
            xmlns="http://www.w3.org/2000/svg"
            width={width}
            height={height}
            viewBox={`0 0 ${width} ${height}`}
            preserveAspectRatio="none"
        >
            <defs>
                <pattern
                    id="pattern"
                    x="0"
                    y="0"
                    patternUnits="userSpaceOnUse"
                    width={patternSize * 2}
                    height={patternSize * 2}
                >
                    <g
                        fill={color}
                        fillOpacity="1"
                    >
                        <rect
                            x="0"
                            y="0"
                            width={patternSize}
                            height={patternSize}
                        />
                        <rect
                            x={patternSize}
                            y={patternSize}
                            width={patternSize}
                            height={patternSize}
                        />
                    </g>
                </pattern>
            </defs>

            <rect
                x="0"
                y="0"
                width={width}
                height={height}
                fill="url(#pattern)"
            />
        </svg>,
    );
}
