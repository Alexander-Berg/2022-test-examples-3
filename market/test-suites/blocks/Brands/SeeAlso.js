import LogoCarousel from '@self/platform/widgets/content/LogoCarousel/__pageObject';
import LogoCarouselBanner from '@self/platform/widgets/content/LogoCarousel/Banner/__pageObject';

export default {
    suiteName: 'SeeAlso',
    selector: LogoCarousel.root,
    ignore: [{every: LogoCarouselBanner.root}],
    capture() {},
};
