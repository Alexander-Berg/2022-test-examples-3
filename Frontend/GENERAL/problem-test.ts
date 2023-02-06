import { object, string } from 'yup';

import { CommonError } from 'common/schemas/common';

const { REQUIRED_VALUE } = CommonError;

export const createProblemTestSchema = object({
    inputPath: string().required(REQUIRED_VALUE),
    inputContent: string(),
    outputPath: string().required(REQUIRED_VALUE),
    outputContent: string(),
});
