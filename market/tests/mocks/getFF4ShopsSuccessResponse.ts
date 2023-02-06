import type {FF4ShopsResult} from '~/app/bcm/ff4Shops/Backend/types';

export default (result: unknown): FF4ShopsResult<unknown> => ({
    result,
});
