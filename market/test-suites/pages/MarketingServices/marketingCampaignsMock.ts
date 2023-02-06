export const getMarketingCampaignsMock = <A extends unknown>({
    length,
    approvedBy,
    status,
}: {
    length: number;
    approvedBy?: A;
    status?: string;
}) => {
    const state = {
        list: {},
    };

    return Array.from({length}).reduce((acc, item, i) => {
        const id = i + 1;

        // @ts-expect-error(TS2571) найдено в рамках VNDFRONT-4580
        acc.list[String(id)] = {
            id,
            approvedBy: approvedBy === null ? undefined : 1,
            dateFrom: 3405531600010,
            dateTo: 3405531600010,
            status,
        };

        return acc;
    }, state);
};
