import type { IEventData } from '@yandex-int/react-baobab-logger/lib/common/Baobab/Baobab.typings/Baobab_dynamic';

// eslint-disable-next-line valid-jsdoc
/** Поиск событий, отправленных в redir лог */
export async function yaGetBaobabSentEvents(this: WebdriverIO.Browser, type?: string, reqid?: string) {
    const curReqid = reqid || await this.yaGetReqId();
    const { client } = await this.getCounters(curReqid);

    const events: IEventData[] = [];

    client.forEach(({ events: eventsStr }) => {
        const event: IEventData = JSON.parse(decodeURIComponent(eventsStr))[0];

        if (type && type !== event.event) {
            return;
        }

        events.push(event);
    });

    return events;
}
