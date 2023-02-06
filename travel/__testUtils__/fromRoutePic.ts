/**
 * Turns:
 * ```
 * 200->100->300, 300->100->150->200
 * ```
 *
 * Into:
 * ```
 * [
 *   {departure: 200, arrival: 300, transfers: [100]},
 *   {departure: 300, arrival: 200, transfers: [100, 150]}
 * ]
 * ```
 *
 * @param {string} pic - route picture
 *
 * @returns {*}
 */
export function fromRoutePic(pic: string) {
    const trimmed = pic.replace(/\s/g, '');
    const segments = trimmed.split(',');
    return segments.map(segmentPic => {
        const stations = segmentPic.split('->').map(Number);
        if (stations.length < 2) {
            throw new TypeError('Wrong route format');
        }
        const departure = stations.shift() as number;
        const arrival = stations.pop() as number;
        return {departure, arrival, transfers: stations};
    });
}
