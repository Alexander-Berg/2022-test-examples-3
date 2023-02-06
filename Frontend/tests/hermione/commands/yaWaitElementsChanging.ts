interface IYaWaitElementsChangingOptions {
    timeout?: number;
    timeoutMsg?: string;
    /** дополнительная функция с условием для ожидания */
    condition?: () => Promise<boolean>;
}

const getElementIdsString = (elements: WebdriverIO.ElementArray) => {
    return elements.map(({ elementId }) => elementId).join(':');
};

export async function yaWaitElementsChanging(
    this: WebdriverIO.Browser,
    selector: string,
    options: IYaWaitElementsChangingOptions = {},
) {
    const currentElems = await this.$$(selector);
    const currentIds = getElementIdsString(currentElems);

    return () => {
        return this.waitUntil(
            async() => {
                const nextElems = await this.$$(selector);
                const nextIds = getElementIdsString(nextElems);

                if (options.condition && !await options.condition()) {
                    return false;
                }

                return currentIds !== nextIds;
            },
            {
                timeout: options.timeout || 5000,
                timeoutMsg: options.timeoutMsg || 'элементы не поменялись',
            },
        );
    };
}
