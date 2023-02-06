console.log(''); // https://github.com/facebook/jest/issues/5792#issuecomment-376678248
const parseS3Path = require('../parseS3Path');

describe('parseS3Path', () => {
    it('should return empty array on falsy input', () => {
        const { protocol, bucket, path } = parseS3Path(undefined);
        expect(protocol).toBeFalsy();
        expect(bucket).toBeFalsy();
        expect(path).toBeFalsy();
    });

    it('should parse absolute fs path', () => {
        const someBucket = 'path';
        const rest = 'to/somewhere';
        const pathToSomewhere = `/${someBucket}/${rest}`;
        const { protocol, bucket, path } = parseS3Path(pathToSomewhere);
        expect(protocol).toBeFalsy();
        expect(bucket).toBeFalsy();
        expect(path).toEqual(pathToSomewhere);
    });

    it('should parse relative fs path', () => {
        const someBucket = 'path';
        const rest = 'to/somewhere';
        const pathToSomewhere = `./${someBucket}/${rest}`;
        const { protocol, bucket, path } = parseS3Path(pathToSomewhere);
        expect(protocol).toBeFalsy();
        expect(bucket).toBeFalsy();
        expect(path).toEqual(pathToSomewhere);
    });

    it('should parse correct s3 path', () => {
        const someBucket = 'bucket';
        const rest = 'to/somewhere';
        const pathToSomewhere = `s3://${someBucket}/${rest}`;
        const { protocol, bucket, path } = parseS3Path(pathToSomewhere);
        expect(protocol).toEqual('s3://');
        expect(bucket).toEqual(someBucket);
        expect(path).toEqual(rest);
    });

    it('should not parse wrong bucket name', () => {
    /**
     * https://docs.aws.amazon.com/awscloudtrail/latest/userguide/cloudtrail-s3-bucket-naming-requirements.html
     */
        const someBucket = '$wrong_bucket_name';
        const rest = 'to/somewhere';
        const pathToSomewhere = `s3://${someBucket}/${rest}`;
        const { protocol, bucket, path } = parseS3Path(pathToSomewhere);
        expect(protocol).toEqual('s3://');
        expect(bucket).toBeFalsy();
        expect(path).toEqual(`${someBucket}/${rest}`);
    });
});
