import {prepareSuite, mergeSuites, makeSuite} from 'ginny';

import GallerySuite from '@self/platform/spec/hermione/test-suites/blocks/Gallery';
import ProductCardTitle from '@self/platform/spec/page-objects/widgets/parts/ProductCard/ProductCardTitle';
import GalleryPicture from '@self/platform/spec/page-objects/components/Gallery/GalleryPicture';

import {productWithPicture, productOptions} from './product.mock';

export default mergeSuites(
    makeSuite('Страница карточки модели', {
        environment: 'kadavr',
        story: mergeSuites(
            {
                async beforeEach() {
                    await this.browser.setState('report', productWithPicture);

                    return this.browser.yaOpenPage('touch:product', {
                        productId: productOptions.id,
                        slug: productOptions.slug,
                    });
                },
            },
            prepareSuite(GallerySuite, {
                pageObjects: {
                    productCardTitle() {
                        return this.createPageObject(ProductCardTitle);
                    },
                    galleryPicture() {
                        return this.createPageObject(GalleryPicture);
                    },
                },
            })
        ),
    })
);
