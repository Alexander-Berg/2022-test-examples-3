/**
 * @typedef {Object} KV
 * @property {String} key
 * @property {String} value
 */

/**
 * Deserialize 2nd section of blockstat log record (blocks stats)
 * to the map of path to array of key-value pairs
 * represented as objects with fields "key" and "value".
 *
 * @param {String} stats Second section of blockstat log record
 * @returns {Map<String, Array<KV>>} path => [ {key, value} ]
 */
function parseStats(stats) {
    /* eslint complexity: [1, 11] */ // not so complex for a little stateful parser, doh.. -_-`
    const tokens = stats.substr(1).split('\t');
    const out = new Map();

    // parser states
    const BLOCK = Symbol('BLOCK');
    const PAIRS_COUNT = Symbol('PAIRS_COUNT');
    const PAIR = Symbol('PAIR');

    // s- prefix means [s]tateful
    let sRestPairs = 0;
    let sBlock = '';
    let next = BLOCK;

    for (const token of tokens) {
        switch (next) {
            case BLOCK:
                sBlock = token;
                if (!out.has(sBlock)) {
                    out.set(sBlock, []);
                }
                next = PAIRS_COUNT;
                break;
            case PAIRS_COUNT:
                if (isNaN(token)) {
                    throw new Error(`KV-pairs count of block "${sBlock}" is NaN: "${token}"`);
                }
                sRestPairs = Number(token);
                next = sRestPairs > 0 ? PAIR : BLOCK;
                break;
            case PAIR:
                const m = token.match(/^(.+?)=(.+)$/);
                if (!m || m.length !== 3) {
                    throw new Error(`KV-pair "${token}" of block "${sBlock}" does not satisfy regex /^.+=.+$/`);
                }
                out.get(sBlock).push({ key: m[1], value: m[2] });
                if (--sRestPairs === 0) {
                    next = BLOCK;
                }
                break;
            default:
                throw new Error(`Unexpected parser state: ${next.toString()}`);
        }
    }

    return out;
}

module.exports = parseStats;
