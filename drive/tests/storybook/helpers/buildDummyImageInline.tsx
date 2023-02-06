import { buildDummyImageSVG, BuildDummyImageSVGOptions } from 'tests/storybook/helpers/buildDummyImageSVG';

export function buildDummyImageInline(props: BuildDummyImageSVGOptions): string {
    if (props.origUrl && /^data:/.test(props.origUrl)) {
        return props.origUrl;
    }

    const svg = buildDummyImageSVG(props);

    let encoded: string = '';

    if (typeof Buffer !== 'undefined') {
        encoded = Buffer.from(svg, 'utf8').toString('base64');
    } else if (typeof window !== 'undefined') {
        encoded = btoa(svg);
    }

    return `data:image/svg+xml;base64,${encoded}`;
}
