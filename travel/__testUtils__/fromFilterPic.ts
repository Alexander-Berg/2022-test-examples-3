/**
 * Turns:
 * ```
 * |>100,200|200|100 |>||100
 * ```
 *
 * Into:
 * ```
 * [
 *   {departure: [100, 200], arrival: [100], transfers: [200]},
 *   {departure: [], arrival: [100], transfers: []}
 * ]
 * ```
 *
 * @param {string} pic - route picture
 *
 * @returns {*}
 */
export function fromFilterPic(pic: string) {
    const trimmed = pic.replace(/\s/g, '');
    const segments = trimmed.split('|>').slice(1);
    return segments.map(segmentPic => {
        const [departurePic, transfersPic, arrivalPic] = segmentPic.split('|');
        const departure = departurePic.split(',').filter(Boolean).map(Number);
        const transfers = transfersPic.split(',').filter(Boolean).map(Number);
        const arrival = arrivalPic.split(',').filter(Boolean).map(Number);
        return {departure, transfers, arrival};
    });
}
