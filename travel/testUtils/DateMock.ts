const RealDate = Date;

function mock(isoDate: string) {
    // @ts-ignore
    global.Date = class extends RealDate {
        constructor(...args: any[]) {
            super();
            if (args.length) {
                // @ts-ignore
                return new RealDate(...args);
            }
            return new RealDate(isoDate);
        }
    };

    // @ts-ignore
    global.Date.now = function (...args: any[]) {
        if (args.length) {
            // @ts-ignore
            return RealDate.now(...args);
        }
        return new RealDate(isoDate).getTime();
    };
}

function restore() {
    global.Date = RealDate;
}

export default {
    mock,
    restore,
};
