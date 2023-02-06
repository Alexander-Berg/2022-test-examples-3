/* eslint-disable */
export const sortByName = <T extends { name: string }>(items: T[]) =>
    items.sort((a, b) => a.name.localeCompare(b.name));
