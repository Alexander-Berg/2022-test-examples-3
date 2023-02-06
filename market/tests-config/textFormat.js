/**
 *   Упрощённый текстовый формат скип-пака.
 */

const {readFileSync, writeFileSync} = require('fs');
const {join, relative} = require('path');

const reSkipFormat = /^(=?)([A-Z]{3,}-\d+)\s+([a-z]+-\d+)\s+(.+)$/;

function parseText(text) {
    const result = [];
    const lines = text.split(/\r?\n/);
    let comment = '';

    for (const [index, rawLine] of lines.entries()) {
        const lineNo = index + 1;
        const line = rawLine.trim();

        if (line === '') {
            comment = '';
        } else if (/^[;/]/.test(line)) {
            comment = line.slice(1).trimLeft();
        } else if (reSkipFormat.test(line)) {
            const [, flag, issue, id, fullName] = reSkipFormat.exec(line);
            const isInRelease = flag === '=';
            const reason = comment || '/skip'; // skipper уже добавляет issue

            result.push({line: lineNo, issue, id, fullName, reason, isInRelease});
        } else {
            throw SyntaxError(`line #${lineNo}: ${JSON.stringify(line)}`);
        }
    }

    return result;
}

// [ { issue, reason, cases: [ {id, fullName} ], isInRelease } ]
function fromText(text) {
    const result = [];
    let current = null;
    let currentHash = '';

    // упрощенно, всё равно потом пере-reduce-тся
    for (const skip of parseText(text)) {
        const {issue, reason, isInRelease, id, fullName} = skip;
        const hash = `${issue}\n${reason}\n${isInRelease}`;

        if (currentHash === hash) {
            current.cases.push({id, fullName});
        } else {
            current = {
                issue,
                reason,
                cases: [{id, fullName}],
                isInRelease,
            };
            currentHash = hash;
            result.push(current);
        }
    }

    return result;
}

function forPlatform(platformType) {
    const name = `skipped.${platformType}.txt`;

    try {
        const path = relative('.', join(__dirname, name));
        const text = readFileSync(path, {encoding: 'utf-8'});

        return fromText(text);
    } catch (e) {
        e.message = `[${name}] ${e.message}`;
        throw e;
    }
}

function legacyToText(legacySkips) {
    const lines = [];

    for (const {issue, reason, isInRelease, cases} of legacySkips) {
        if (!/^(сломаны? тесты?|ат сломан$|[/;]|\s*$)/i.test(reason)) {
            lines.push(`; ${reason}`);
        }

        for (const {id, fullName} of cases) {
            lines.push(`${isInRelease ? '=' : ''}${issue} ${id} ${fullName}`);
        }

        lines.push('');
    }

    return lines.join('\n');
}

module.exports = {
    parseText,
    fromText,
    forPlatform,
    legacyToText,
};

if (!module.parent) {
    const [fileMask] = process.argv.slice(2);
    const legacy = require('./skipped.js');

    if (fileMask && !/%/.test(fileMask)) {
        console.error('File mask must contain "%" placeholder! (ex: "skipped.%.txt")');
        process.exit(1);
    }

    for (const platformType of Object.keys(legacy)) {
        const text = legacyToText(legacy[platformType]);

        if (fileMask) {
            const path = fileMask.replace('%', platformType);

            console.warn('Writing "%s" (%d)...', path, text.length);
            writeFileSync(path, text, {flag: 'wx'}); // don't overwrite
        } else {
            process.stdout.write(`\n/ ${platformType}\n\n`);
            process.stdout.write(text);
        }
    }
}
