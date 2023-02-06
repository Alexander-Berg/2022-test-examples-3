import getAdjustedImageDimensions from '../getAdjustedImageDimensions';

const imageConstraints = {
    minSize: 60,
    maxSize: 280,
};

const data = [
    [30, 40, 60, 80],
    [30, 30, 60, 60],
    [40, 40, 60, 60],
    [236, 60, 236, 60],
    [236, 59, 240, 60],
    [1000, 59, 280, 60],
    [60, 236, 60, 236],
    [59, 236, 60, 240],
    [59, 1000, 60, 280],
    [133, 168, 133, 168],
    [2677, 1058, 280, 110.66118789689952],
    [500, 252, 280, 141.12],
    [1280, 843, 280, 184.40625],
    [1944, 2592, 210, 280],
    [23133, 2545, 280, 60],
    [4525, 2545, 280, 157.48066298342542],
    [818, 1000, 229.04, 280],
    [1200, 797, 280, 185.96666666666667],
    [3993, 7876, 141.95530726256982, 280],
    [285, 443, 180.1354401805869, 280],
];

describe('getAdjustedImageDimensions', () => {
    data.forEach(([width, height, shouldWidth, shouldHeight]) => {
        it(`${width}x${height} should be ${shouldWidth}x${shouldHeight}`, () => {
            expect(getAdjustedImageDimensions({ width, height }, imageConstraints))
                .toEqual({ width: shouldWidth, height: shouldHeight });
        });
    });
});
