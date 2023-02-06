import {EApiEntry} from 'types/EApiEntry';

export function getRequestApiHostType(
    url: string | undefined,
    servicesAPI: Record<EApiEntry, string>,
): EApiEntry | undefined {
    if (!url) {
        return;
    }

    let matchedApiHostType: EApiEntry | undefined;

    Object.values(EApiEntry).forEach(apiHostType => {
        const apiHost = servicesAPI[apiHostType];

        if (
            url.startsWith(apiHost) &&
            (!matchedApiHostType ||
                servicesAPI[matchedApiHostType].length < apiHost.length)
        ) {
            matchedApiHostType = apiHostType;
        }
    });

    return matchedApiHostType;
}
