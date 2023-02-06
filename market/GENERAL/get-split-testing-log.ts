import { BucketInfo } from '../types/usaas';

type LogObject = { [item: string]: string };

const getSplitTestingLog = (bucketInfo?: BucketInfo): LogObject => {
    const log: LogObject = {};

    if (!bucketInfo) {
        return log;
    }
    // Convert from `{ experiment: "variation" }` to `[ ["experiment", "variation"] ]`
    const ab = Object.keys(bucketInfo).map((key) => [key, bucketInfo[key]]);

    if (ab.length) {
        log.ab = JSON.stringify(ab);
    }

    return log;
};

export default getSplitTestingLog;
