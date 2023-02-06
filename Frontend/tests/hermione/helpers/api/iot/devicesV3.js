/**
 * @class DevicesV3
 * @type {{methods: {dingDevice: (function(string): {path: string, options: {method: string}}), getDevices: (function(): Promise<ApiIotDevicesV3Response>), setDeviceQuasarConfig: (function(SetDeviceQuasarConfigParams): {path: string, options: {method: string, body: string}}), getPropertyHistoryGraph: (function(GetPropertyHistoryGraphParams): {path: string, query: {grid: string, from: *, aggregation: *, to: *}})}, namespace: string}}
 */
module.exports = {
    namespace: '/m/v3/user/devices',
    methods: {
        getDevices: () => ({
            path: '',
        }),

        setDeviceQuasarConfig: ({ deviceId, config, version }) => ({
            path: `/${deviceId}/configuration/quasar`,
            options: {
                method: 'POST',
                body: JSON.stringify({
                    config,
                    version,
                }),
            },
        }),

        dingDevice: (id) => ({
            path: `/${id}/ding`,
            options: {
                method: 'POST',
            },
        }),

        getPropertyHistoryGraph: ({ id, property, from, to, gridPeriod, gridType, aggregation }) => ({
            path: `/${id}/properties/${property}/history/graph`,
            query: {
                from,
                to,
                grid: `${gridPeriod}${gridType}`,
                aggregation,
            },
        }),
    },
};
