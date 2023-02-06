import _ from 'lodash';

import { TITLE_KEYS } from '../constants';

export function prenormalizeRaw(value: string, fixIndent: boolean): string {
    const arrValue = value.split('\n');

    return arrValue
        .map((line, i) => {
            let newLine = line;

            if (!newLine.includes('#')) {
                return newLine;
            }

            if (fixIndent && arrValue[i + 1] !== undefined) {
                newLine = fixCommentIndent(arrValue, i);
            }

            return prepareCommentLine(newLine, arrValue, i);
        })
        .join('\n');
}

export function normalizeRaw(value: string, params: Record<string, any>): string {
    if (_.get(params, 'refs.length')) {
        value = params.refs.reduce(
            (acc: string, ref: string, i: number) => acc.replace(new RegExp(`ref_${i}`, 'gm'), ref),
            value,
        );
    }

    const arrValue = value
        .split('\n')
        .map((line) => {
            let result = line
                // конвертируем комментарии в массиве или в объекте
                .replace(/^(\s*)(- )?'%%COMMENT.*?%%': ['"]?(.*?)['"]?$/gm, '$1# $3')
                // убираем тройные кавычки-артефакты
                .replace(/'''/gm, "'");

            // убираем двойные кавычки-артефакты, если только это не пустая строка в значении
            result = !result.endsWith(": ''") ? result.replace(/''/gm, '"') : result;

            return result;
        })
        .filter(Boolean);

    return (
        arrValue
            .map((line, i) => {
                if (
                    line.startsWith(' ') ||
                    (arrValue[i - 1] || '').startsWith('#') ||
                    TITLE_KEYS.some((key) => line.startsWith(key))
                ) {
                    return line;
                }

                return `\n${line}`;
            })
            .join('\n') + '\n'
    );
}

export function getLineIndent(line: string | void): number {
    return (line && line.replace(/^(\s*)(.*)$/gm, '$1').length) as number;
}

export function getLineType(line: string | void): string {
    switch (line && line.trim()[0]) {
        case '#':
            return 'comment';
        case '-':
            return 'array';
        default:
            return 'object';
    }
}

export function isCommentLine(line: string): boolean {
    return getLineType(line) === 'comment';
}

export function findSiblingLine(lines: string[], i: number, step = 1): string | void {
    const indent = getLineIndent(lines[i]);
    const tryFindLine = ({ toDown = true, withoutIndent = false }): string | void => {
        for (let j = i; lines[j] !== undefined; j = toDown ? j + step : j - step) {
            if (Boolean(lines[j]) && !isCommentLine(lines[j])) {
                if (withoutIndent || indent === getLineIndent(lines[j])) {
                    return lines[j];
                }
                break;
            }
        }
    };

    return (
        tryFindLine({ toDown: true, withoutIndent: false }) ||
        tryFindLine({ toDown: false, withoutIndent: false }) ||
        tryFindLine({ toDown: true, withoutIndent: true }) ||
        tryFindLine({ toDown: false, withoutIndent: true })
    );
}

export function getCommentLineType(lines: string[], i: number): string | void {
    if (i < 0 || i > lines.length - 1) {
        return;
    }

    if (!isCommentLine(lines[i])) {
        return `stringIn${_.upperFirst(getLineType(lines[i]))}`;
    }

    return getLineType(findSiblingLine(lines, i));
}

export function getCommentLinePrefix(type: string | void): string {
    return type && (type.includes('array') || type.includes('Array')) ? '- ' : '';
}

export function fixCommentIndent(lines: string[], i: number): string {
    const targetLineIndent = getLineIndent(findSiblingLine(lines, i));
    const indentValue = targetLineIndent
        ? _.times(targetLineIndent + 1, _.constant('')).join(' ')
        : '';
    const newComment = targetLineIndent
        ? lines[i].trim().replace('#' + indentValue, '# ')
        : lines[i];

    return indentValue + newComment;
}

export function getCommentLineInnerQuote(line: string): string {
    return line.split('#')[1].includes('"') ? "'" : '"';
}

export function prepareCommentLine(line: string, arrValue: string[], i: number): string {
    const q = getCommentLineInnerQuote(line);
    const type = getCommentLineType(arrValue, i);
    const prefix = getCommentLinePrefix(type);

    switch (type) {
        case 'stringInArray':
        case 'stringInObject':
            return line.replace(
                /^(\s*)(.+)\s#\s?(.*)$/,
                `$1${prefix}'%%COMMENT${i + 1}%%': ${q}$3${q}\n$1$2`,
            );
        case 'array':
        case 'object':
            return line.replace(/^(\s*)#\s?(.*)$/, `$1${prefix}'%%COMMENT${i + 1}%%': ${q}$2${q}`);
        default:
            return line;
    }
}
