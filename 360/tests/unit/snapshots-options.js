export { render, renderIntoDocument } from 'react-testing-library';

/**
 * @param node
 */
function removeComments(node) {
    if (node.hasChildNodes()) {
        let child = node.firstChild;
        while (child) {
            if (child.nodeName === '#comment') {
                const prevChild = child;
                child = child.nextSibling;
                node.removeChild(prevChild);
            } else {
                removeComments(child);
                child = child.nextSibling;
            }
        }
    }
    return node;
}

export const serialize = ({ container }) => removeComments(container).firstChild;
