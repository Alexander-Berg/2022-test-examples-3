const _ = require('lodash');
const { isPoolValid, getValidPoolList, getValidPoolTitle } = require('../../../../src/shared/utils/pool-validation');

const POOLS = [
    {
        title: 'touch_360_default',
        platform: 'touch',
        region: 'ru',
        default: true,
    },
    {
        title: 'sbs_touch_images',
        platform: 'touch',
        region: 'ru',
        default: false,
    },
    {
        default: false,
        region: 'ru',
        platform: 'touch',
        title: 'touch_skill',
    },
    {
        title: 'touch_skill_renew',
        platform: 'pad',
        region: 'ru',
        default: false,
    },
    {
        default: false,
        region: 'ru',
        platform: 'desktop',
        title: 'desktop_toloka',
    },
    {
        title: 'touch_kpi_q2q3',
        platform: 'pad',
        region: 'ru',
        default: false,
    },
    {
        title: 'touch_kpi_q2q3_renew',
        platform: 'touch',
        region: 'ru',
        default: false,
    },
    {
        title: 'touch_360_beauty',
        platform: 'touch',
        region: 'uk',
        default: false,
    },
    {
        platform: 'touch',
        isIphone: false,
        region: 'by',
        default: false,
        title: 'test_pool_name',
    },
    {
        platform: 'touch',
        isIphone: true,
        region: 'kz',
        default: false,
        title: 'test_pool_name_2',
    },
];

const ARGS = {
    device: 'touch',
    region: 'ru',
    iphone: 'no',
    poolsList: POOLS,
};

describe('pool-validation', () => {
    let poolListStub, args;
    beforeEach(() => {
        poolListStub = _.cloneDeep(POOLS);
        args = _.cloneDeep(ARGS);
    });

    describe('isPoolValid', () => {
        it('должен вернуть true если пул есть в списке', () => {
            assert.isTrue(isPoolValid('touch_360_default', poolListStub));
        });

        it('должен вернуть false если пула нет в списке', () => {
            assert.isFalse(isPoolValid('fake_pool_title', poolListStub));
        });
    });

    describe('getValidPoolList', () => {
        it('должен корректно фильтровать пулы по региону', () => {
            const REGION = 'ru';
            args.region = REGION;
            const poolList = getValidPoolList(args);

            assert.isTrue(poolList.every((pool) => pool.region === REGION));
        });

        it('должен корректно фильтровать пулы по платформе', () => {
            const DEVICE = 'pad';
            args.device = DEVICE;
            const poolList = getValidPoolList(args);

            assert.isTrue(poolList.every((pool) => pool.platform === DEVICE));
        });

        it('должен корректно фильтровать пулы для iphone', () => {
            args.iphone = 'yes';
            args.region = 'kz';
            const poolList = getValidPoolList(args);
            assert.isTrue(poolList.every((pool) => pool.isIphone));
        });
    });

    describe('getValidPoolTitle', () => {
        it('должен вернуть дефолтный пул, если он есть', () => {
            const DEFAULT_POOL_TITLE = 'touch_360_default';
            assert.equal(getValidPoolTitle(args), DEFAULT_POOL_TITLE);
        });

        it('должен вернуть первый пул, если нет дефолтного', () => {
            args.region = 'by';
            const poolList = getValidPoolList(args);
            assert.equal(getValidPoolTitle(args), poolList[0].title);
        });

        it('должен вернуть null, если нет подходящих пулов', () => {
            args.region = 'bb';
            args.device = 'tv';
            assert.isNull(getValidPoolTitle(args));
        });
    });
});
