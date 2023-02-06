import { getCarsScheduleGridMenuItems } from 'entities/Car/helpers/getCarsScheduleGridMenuItems/getCarsScheduleGridMenuItems';

describe('getCarsScheduleGridMenuItems', function () {
    it('should works', function () {
        expect(getCarsScheduleGridMenuItems()).toMatchInlineSnapshot(`
            Array [
              Object {
                "label": "New booking",
                "onClick": [Function],
                "value": "booking",
              },
              Object {
                "label": "New service",
                "onClick": [Function],
                "value": "service",
              },
            ]
        `);
    });
});
