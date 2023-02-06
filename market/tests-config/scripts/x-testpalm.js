/**
 *  Удаляем из скип-пака по списку TestPalm ID.
 */

const fs = require('fs');

const name = process.argv[2];
const target = process.argv[3];

if (!name || !target) {
    console.error('Usage: x-testpalm.js skipped.desktop.txt testpalm-list.txt');
    process.exit(1);
}

const source = fs.readFileSync(name, 'utf8').split('\n');
const victims = new Set(
    fs
        .readFileSync(target, 'utf8')
        .split('\n')
        .map(x => (x.match(/marketmbi-\d+/) || [])[0])
        .filter(Boolean),
);

const filtered = source.filter(l => {
    const [_, testpalm] = l.split(/\s+/) || [];
    if (testpalm && victims.has(testpalm)) {
        console.warn('-', l);
        return false;
    }
    return true;
});

fs.writeFileSync(name, filtered.join('\n'));
