import {makeSuite, prepareSuite, mergeSuites} from 'ginny';

// suites
import ProductPageVideoSuite from '@self/platform/spec/hermione/test-suites/blocks/Gallery/product-page-video';
import ProductPagePhotosSuite from '@self/platform/spec/hermione/test-suites/blocks/Gallery/product-page-photos';
import ProductPageSinglePhotosSuite from '@self/platform/spec/hermione/test-suites/blocks/Gallery/product-page-single-photo';
import ProductPageVideoKMSuite from '@self/platform/spec/hermione/test-suites/blocks/Gallery/product-page-video-km';
// page-objects
import GalleryNumericCounter from '@self/platform/spec/page-objects/components/Gallery/GalleryNumericCounter';
import GallerySlider from '@self/platform/spec/page-objects/components/Gallery/GallerySlider';
import GalleryModal from '@self/platform/spec/page-objects/components/Gallery/GalleryModal';
import GalleryVideoContent from '@self/platform/spec/page-objects/components/Gallery/GalleryVideoContent';

import {
    product as productWithVideo,
    productOptions as productWithVideoOptions,
    tarantinoState as productWithVideoTarantinoState,
} from './fixtures/productWithVideo';

import * as productWithVideoGallery from './fixtures/productWithVideoOnGallery';

import {product as productWithPhotos, productOptions as productWithPhotosOptions} from './fixtures/productWithPhotos';

import {
    product as productWithSinglePhoto,
    productOptions as productWithSinglePhotoOptions,
} from './fixtures/productWithSinglePhoto';

export default makeSuite('Галерея.', {
    environment: 'kadavr',
    story: mergeSuites(
        prepareSuite(ProductPageVideoSuite, {
            pageObjects: {
                galleryNumericCounter() {
                    return this.createPageObject(GalleryNumericCounter);
                },
                gallerySlider() {
                    return this.createPageObject(GallerySlider);
                },
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('report', productWithVideo);
                    await this.browser.setState('Tarantino.data.result', productWithVideoTarantinoState);

                    return this.browser.yaOpenPage('touch:product', {
                        productId: productWithVideoOptions.id,
                        slug: productWithVideoOptions.slug,
                    });
                },
            },
        }),
        prepareSuite(ProductPagePhotosSuite, {
            pageObjects: {
                galleryNumericCounter() {
                    return this.createPageObject(GalleryNumericCounter);
                },
                gallerySlider() {
                    return this.createPageObject(GallerySlider);
                },
                modalGallerySlider() {
                    return this.createPageObject(GallerySlider, {
                        parent: GalleryModal.root,
                    });
                },
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('report', productWithPhotos);

                    return this.browser.yaOpenPage('touch:product', {
                        productId: productWithPhotosOptions.id,
                        slug: productWithPhotosOptions.slug,
                    });
                },
            },
        }),
        prepareSuite(ProductPageSinglePhotosSuite, {
            pageObjects: {
                galleryNumericCounter() {
                    return this.createPageObject(GalleryNumericCounter);
                },
                gallerySlider() {
                    return this.createPageObject(GallerySlider);
                },
                galleryModal() {
                    return this.createPageObject(GalleryModal);
                },
                modalGallerySlider() {
                    return this.createPageObject(GallerySlider, {
                        parent: GalleryModal.root,
                    });
                },
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('report', productWithSinglePhoto);

                    return this.browser.yaOpenPage('touch:product', {
                        productId: productWithSinglePhotoOptions.id,
                        slug: productWithSinglePhotoOptions.slug,
                    });
                },
            },
        }),
        prepareSuite(ProductPageVideoKMSuite, {
            pageObjects: {
                galleryNumericCounter() {
                    return this.createPageObject(GalleryNumericCounter);
                },
                gallerySlider() {
                    return this.createPageObject(GallerySlider);
                },
                galleryVideoContent() {
                    return this.createPageObject(GalleryVideoContent);
                },
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('report', productWithVideoGallery.product);
                    await this.browser.setState('Tarantino.data.result', productWithVideoGallery.tarantinoState);
                    await this.browser.setState('S3Mds.files', {
                        '/products_video/links.json': [productWithVideoGallery.s3Mds],
                    });
                    await this.browser.setState('vhFrontend', productWithVideoGallery.vhFrontend);
                    return this.browser.yaOpenPage('touch:product', {
                        productId: productWithVideoOptions.id,
                        slug: productWithVideoOptions.slug,
                    });
                },
            },
        })
    ),
});
