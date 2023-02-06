import UgcFeedScrollBox from '@self/platform/spec/page-objects/widgets/content/UgcFeedScrollBox';

export default {
    // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T15%3A47%3A41.034577.jpg
    suiteName: 'UgcFeedScrollbox',
    selector: `${UgcFeedScrollBox.root} > div`,
    ignore: [
        {every: '[data-cs-name="navigate"]'},
    ],
    capture() {},
};
