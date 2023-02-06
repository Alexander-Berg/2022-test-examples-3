// eslint-disable-next-line valid-jsdoc
/** Скролит родителя к указанному дочернему элементу */
export async function yaScrollContainerToElem(
    this: WebdriverIO.Browser,
    container: string,
    elem: string,
): Promise<void> {
    const isScrolled = await this.execute((container: string, elem: string) => {
        const elementNode = document.querySelector(elem);
        let containerNode = document.querySelector(container);

        if (!elementNode || !containerNode) {
            return false;
        }

        let boundingClientRect = elementNode.getBoundingClientRect();

        containerNode.scrollTop = containerNode.scrollTop + boundingClientRect.top;
        containerNode.scrollLeft = containerNode.scrollLeft + boundingClientRect.left;

        return true;
    }, container, elem);

    if (!isScrolled) {
        throw new Error('Указанные элементы отсутствуют на странице');
    }
}
