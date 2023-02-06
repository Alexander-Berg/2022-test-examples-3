export const replaceItem = <T>(array: T[], newItem: T, index: number) => {
    const rightPart = array.slice(0, index);
    const leftPart = array.slice(index + 1);

    return [...rightPart, newItem, ...leftPart];
};
