const proxyquire = require('proxyquire');
const _ = require('lodash');

const calcDesignNormalTasks = sinon.stub();
const calcDesignHoneypotTasks = sinon.stub();

const freshener = proxyquire.load('../../../../src/shared/fresheners/layout', {
    '../utils/calc-design-normal-tasks': calcDesignNormalTasks,
    '../utils/calc-design-honeypot-tasks': calcDesignHoneypotTasks,
});

const EXP = {
    title: 'Авиа календарь',
    description: '',
    poolTitle: 'touch_320',
    question: 'Какой вариант вам больше нравится?',
    layouts: {
        systems: [
            { name: 'прод' },
            { name: 'контроль' },
        ],
        screens: [
            {
                name: 'Экран 1',
                question: 'Какой вариант вам больше нравится?',
            },
        ],
        layouts: [
            {
                screens: [
                    {
                        origUrl: 'https://samadhi-layouts.s3.yandex.net/filtered-02cVZt/548010230330.png',
                        fileName: 'prod.png',
                    },
                    {
                        origUrl: 'https://samadhi-layouts.s3.yandex.net/filtered-02cVZt/756277814008.png',
                        fileName: 'control_2.png',
                    },
                ],
            },
        ],
    },
    badTasks: 0,
    goodTasks: 1,
    useMerger: 'yes',
    configOverride: '',
    mailNotify: 'silent',
    overlap: {
        mode: 'default',
        value: '',
    },
    useAutoHoneypots: 'yes',
    workflowType: 'stable',
    assessmentDeviceType: 'desktop',
    runInYang: 'no',
    simpleUpload: 'simple',
    notificationMode: {
        preset: 'stOnly',
        workflowNotificationChannels: ['email'],
    },
};

const META = {
    layoutsPoolsList: [
        {
            poolId: 23651786,
            sandboxId: 58432,
            title: 'desktop_new',
            platform: 'desktop',
            region: 'ru',
            label: 'Десктоп / любые картинки',
            profile: 'default_desktop',
            default: true,
        },
        {
            poolId: 23651786,
            sandboxId: 58433,
            title: 'touch_360',
            platform: 'touch',
            region: 'ru',
            label: 'Телефон, ширина экрана 360px',
            profile: 'touch-medium',
        },
    ],
};

describe('design fresheners', function() {
    let exp, sandbox;

    beforeEach(() => {
        exp = _.cloneDeep(EXP);
        sandbox = sinon.createSandbox();
    });

    afterEach(() => sandbox.restore());

    it('должен задавать значение поля goodTasks, если не задано явно пользователем', function() {
        const goodTasks = 100500;
        calcDesignNormalTasks.returns(goodTasks);
        delete exp.goodTasks;

        const { exp: freshedExp } = freshener(exp, {}, META);

        assert.equal(freshedExp.goodTasks, goodTasks);
    });

    it('должен задавать значение поля badTasks, если не задано явно пользователем', function() {
        const badTasks = 100542;
        calcDesignHoneypotTasks.returns(badTasks);
        exp.badTasks = undefined;

        const { exp: freshedExp } = freshener(exp, {}, META);

        assert.equal(freshedExp.badTasks, badTasks);
    });
});
