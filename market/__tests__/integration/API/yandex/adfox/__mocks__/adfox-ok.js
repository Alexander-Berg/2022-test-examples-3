import { SOVETNIK_ADFOX_ACCOUT_ID } from '../../../../../../src/constants';

module.exports = {
    host: 'https://fenek.market.yandex.ru',
    route: `/${SOVETNIK_ADFOX_ACCOUT_ID}/getBulk/v1?reqid=123456&req_id=123456`,
    method: 'post',
    allowUnmocked: false,
    response: {
        jsonapi: {
            version: '1.0',
            meta: {
                protocol_version: '1.0',
            },
        },
        meta: {
            session_id: '971077377',
            request_id: '1652695245889/6399acd2ae2465e5cbbae8f4681e76f8',
        },
        data: [
            {
                id: '0',
                type: 'javascript',
                attributes: {
                    origin: 'adfox',
                    http_status_code: '200',
                    content:
                        'ew0KICAibWV0YSI6IHsNCiAgICAibWV0cmlrYVBhcmFtcyI6IHsiY2FtcGFpZ25faWQiOiAiRk1DR19jYXRlZ29yeV9QUk9NTzQ1IiwgInByb21vdGlvbk9iamVjdCI6ICJjYXRlZ29yeV9wcm9tbyIsICJ0eXBlIjogImhlcm9fYmFubmVyIiwgImJhbm5lck5hbWUiOiAiRk1DR19jYXRlZ29yeV9QUk9NTzQ1XzE0NDB4MzAwIn0sDQogICAgInBhcmFtcyI6IHt9fSwNCiAgImJhY2tncm91bmRDb2xvciI6ICIiLA0KICAiaWQiOiAiNTEzNzk5NyIsDQogICJyZWFsTGluayI6ICJodHRwczovL21hcmtldC55YW5kZXgucnUvc3BlY2lhbC9QUk9NTzQ1P2Zyb209YWRmb3hfZGVza3RvcF9tYXJrZXRfRk1DR19jYXRlZ29yeV9QUk9NTzQ1JmJhbm5lcj1GTUNHX2NhdGVnb3J5X1BST01PNDVfMTQ0MHgzMDAmcGxhY2VtZW50PW1haW5ydSZmbXQ9MTQ0MHgzMDAiLA0KICAiZW50aXR5IjogImJhbm5lciIsDQogICJjYXRlZ29yeU5hbWUiOiAiIiwNCiAgInNob3BQcm9tb0lkcyI6ICIiLA0KICAidmlzaWJpbGl0eVVybCI6ICJodHRwczovL2FkczYuYWRmb3gucnUvMjUyNjE2L2V2ZW50P2hhc2g9MGQxNDViZDY4NmIxNDFjOCZwbT1iJnA1PWxnaXBoJnJxcz1kd2JXelJfOFgxamNwNFppcTZKYWhvWUpZMndUTFZabSZwMj1mdXZ6JnB1aWQxNT1ubyZwdWlkMTQ9dGllcl8xJnB1aWQxOD1ubyZyYW5kPWNvbmx5cWomc2o9YXp1Rm4xbE4yWVFuT1VnNGoyZzJIa2dvSUVlaTIzeGQ3M3Z5Q1FJTFBwNmdFNFBSdTJCLUdlbWNyckJ5aXclM0QlM0QmcHVpZDE2PW5vJnB1aWQxMz15ZXMmbHRzPWZqZGdna2EmcHVpZDExPTIxMyZwdWlkMTk9bm8mcHI9ZGR0YWV6eCZwMT1iem1iZCZwdWlkMTI9bm8iLA0KICAibGluayI6ICJodHRwczovL2FkczYuYWRmb3gucnUvMjUyNjE2L2dvTGluaz9wdWlkMTg9bm8mcHVpZDE0PXRpZXJfMSZwdWlkMTI9bm8mcHVpZDExPTIxMyZoYXNoPWZmNjNkNTA1MWE0NDNiNDImcHVpZDEzPXllcyZwdWlkMTY9bm8mc2o9YXp1Rm4xbE4yWVFuT1VnNGoyZzJIa2dvSUVlaTIzeGQ3M3Z5Q1FJTFBwNmdFNFBSdTJCLUdlbWNyckJ5aXclM0QlM0QmcmFuZD1rY293cWFtJnJxcz1kd2JXelJfOFgxamNwNFppcTZKYWhvWUpZMndUTFZabSZwNT1sZ2lwaCZwdWlkMTk9bm8mcHI9ZGR0YWV6eCZwMT1iem1iZCZwdWlkMTU9bm8mcDI9ZnV2eiIsDQogICJpbWFnZSI6IHsNCiAgICAiZW50aXR5IjogInBpY3R1cmUiLA0KICAgICJhbHQiOiAiIiwNCiAgICAidGh1bWJuYWlscyI6IFsNCiAgICAgIHsNCiAgICAgICAgImVudGl0eSI6ICJ0aHVtYm5haWwiLA0KICAgICAgICAidXJsIjogImh0dHBzOi8vYXZhdGFycy5tZHMueWFuZGV4Lm5ldC9nZXQtYWRmb3gtY29udGVudC8yNDYyNjIxLzIyMDUxOV9tYXJrZXRfMTg2MTA3MV81MTM3OTk3XzEuOGJkYjhlMTBjMmViMDNiNGNmYzU4MGMyZDNhZTIwYWMucG5nL29wdGltaXplLndlYnAiLA0KICAgICAgICAid2lkdGgiOiAiMTQ0MCIsDQogICAgICAgICJoZWlnaHQiOiAiMzAwIiwNCiAgICAgICAgImRlbnNpdHkiOiAxDQogICAgICAgICAgICAgICAgICAgIH0sDQogICAgICB7DQogICAgICAgICJlbnRpdHkiOiAidGh1bWJuYWlsIiwNCiAgICAgICAgInVybCI6ICJodHRwczovL2F2YXRhcnMubWRzLnlhbmRleC5uZXQvZ2V0LWFkZm94LWNvbnRlbnQvMjc2NTM2Ni8yMjA1MTlfbWFya2V0XzE4NjEwNzFfNTEzNzk5N180LmU4M2IwOGI4ODQzYzhmM2NiOGFhODczMjU4M2EyMGU3LnBuZy9vcHRpbWl6ZS53ZWJwIiwNCiAgICAgICAgIndpZHRoIjogIjI4ODAiLA0KICAgICAgICAiaGVpZ2h0IjogIjYwMCIsDQogICAgICAgICJkZW5zaXR5IjogMg0KICAgICAgICAgICAgICAgICAgICB9DQogICAgICAgICAgICAgICAgXQ0KICB9LA0KICAic3RhdHMiOiB7DQogICAgImFkZm94VXJsIjogImh0dHBzOi8vYWRzNi5hZGZveC5ydS8yNTI2MTYvZXZlbnQ/aGFzaD1lMDkwMDFkOTQ1NTMxMTc0JnBtPWsmcDU9bGdpcGgmcnFzPWR3Yld6Ul84WDFqY3A0WmlxNkphaG9ZSlkyd1RMVlptJnAyPWZ1dnomcHVpZDE1PW5vJnB1aWQxND10aWVyXzEmcHVpZDE4PW5vJnJhbmQ9aXd3aWNjdCZzaj1henVGbjFsTjJZUW5PVWc0ajJnMkhrZ29JRWVpMjN4ZDczdnlDUUlMUHA2Z0U0UFJ1MkItR2VtY3JyQnlpdyUzRCUzRCZwdWlkMTY9bm8mcHVpZDEzPXllcyZsdHM9ZmpkZ2drYSZwdWlkMTE9MjEzJnB1aWQxOT1ubyZwcj1kZHRhZXp4JnAxPWJ6bWJkJnB1aWQxMj1ubyIsDQogICAgImNsaWVudFVybCI6ICJodHRwczovL2Jhbm5lcnMuYWRmb3gucnUvdHJhbnNwYXJlbnQuZ2lmIg0KICB9DQp9',
                    location: '',
                    encoding: 'utf-8',
                },
            },
        ],
        errors: [],
    },
};
