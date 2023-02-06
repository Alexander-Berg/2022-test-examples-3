const { jsonStr } = require('./json-str');

function cleanup(node) {
    const res = { ...node };
    delete res.type;
    delete res.children;
    delete res.value;
    if (Array.isArray(res.title)) {
        delete res.title;
    }
    delete res.position;
    return res;
}

const lc = ({ line, column, offset }) => `${offset}:${line}:${column}`;

function serialize(node, indent = 0) {
    function draw(field) {
        const hasField = field in node && Array.isArray(node[field]);
        const hasTitle = 'title' in node && Array.isArray(node.title);
        if (!hasField) {
            return '';
        }

        return '\n' +
            (hasTitle ? ('  '.repeat(++indent)) + '@' + field + ':\n' : '') +
            (node[field].map(child => serialize(child, indent + 1)).join('\n') || `${'  '.repeat(indent + 1)}—`) +
            (hasTitle && --indent, '');
    }

    const pos = node.position ? `${lc(node.position.start)}-${lc(node.position.end)}` : '?';
    const data = jsonStr(cleanup(node)) || '';

    return `${'  '.repeat(indent)}#${node.type} @${pos} ${data.length > 2 ? data : ''} ${'value' in node && node.value !== undefined ?
        jsonStr(node.value) : ''} ${draw('title')} ${draw('children')}`;
}

exports.serialize = serialize;
