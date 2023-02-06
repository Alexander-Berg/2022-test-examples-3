import { bucketsReducer, init } from '../../buckets';

let maxVersion = 0;

export const bucketsMockFactory = () => {
    return {
        createState(state?: Partial<Client.Buckets.State>) {
            maxVersion++;

            return bucketsReducer({ maxVersion, }, init({
                maxVersion,
                ...state,
            }));
        },
        createHiddenPrivateChats(
            chatsIds: Client.Buckets.HiddenPrivateChat,
        ): Client.Buckets.Bucket<Client.Buckets.HiddenPrivateChat> {
            maxVersion++;

            return {
                data: { ...chatsIds },
                version: maxVersion,
            };
        },
        createChatMutings(
            chatsIds: string[],
        ): Client.Buckets.Bucket<Client.Buckets.ChatMutingsData> {
            maxVersion++;

            return {
                data: chatsIds.reduce<Client.Buckets.ChatMutingsData>((aux, chatId) => {
                    aux[chatId] = {
                        mute: true,
                        mute_mentions: false,
                    };

                    return aux;
                }, {}),
                version: maxVersion,
            };
        },
        createRestrictionsBucket(
            data: Partial<Client.Buckets.RestrictionsData>,
        ): Client.Buckets.Bucket<Client.Buckets.RestrictionsData> {
            maxVersion++;

            return {
                data: {
                    blacklist: data.blacklist || [],
                    whitelist: data.whitelist || [],
                },
                version: maxVersion,
            };
        },
    };
};
