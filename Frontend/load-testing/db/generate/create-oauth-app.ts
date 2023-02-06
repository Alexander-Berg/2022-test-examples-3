/* eslint-disable */
import { OAuthApp, User } from '../../../db';

export const generateOauthApps = async() => {
    const appsCount = 100;
    const userIds = (await User.findAll({ limit: appsCount })).map(({ id }) => id);
    const names = [...Array(appsCount).keys()].map(i => `Oauth app ${i}`);
    const socialNames = [...Array(appsCount).keys()].map(i => `social_app_name_${i}`);

    const oauthAppsAttributes = userIds.map((userId, i) => ({
        userId,
        name: names[i],
        socialAppName: socialNames[i],
    }));

    await OAuthApp.bulkCreate(oauthAppsAttributes);
};

(async() => {
    await generateOauthApps();
})().then(() => process.exit(0), console.error);
