import * as React from 'react';
import { mount } from 'enzyme';
import * as serializer from 'jest-serializer-html';
import { MimeType } from '@yandex-turbo/components/lcTypes/lcTypes';
import { LcPicture } from '../LcPicture';
import { ILcPictureProps } from '../LcPicture.types';

expect.addSnapshotSerializer(serializer);

describe('LcPicture component', () => {
    function mountLcPicture(props: ILcPictureProps) {
        return mount(<LcPicture {...props} />);
    }

    const stringValue = 'http://ya.ru/image.png';
    const avatarValue = {
        url: 'https://avatars.mds.yandex.net/test-src/orig',
        width: 200,
        height: 100,
    };

    describe('should render legacy image', () => {
        test('should render correct image if value is string', () => {
            expect(mountLcPicture({ value: stringValue }).html()).toMatchSnapshot();
        });

        test('should render correct image if value has no width', () => {
            expect(mountLcPicture({ value: { url: stringValue, height: 500 } }).html()).toMatchSnapshot();
        });

        test('should render correct image if value has no height', () => {
            expect(mountLcPicture({ value: { url: stringValue, width: 700 } }).html()).toMatchSnapshot();
        });

        test('should render correct image if mimetype is svg', () => {
            const lcPicture = mountLcPicture({
                value: { ...avatarValue, mimeType: MimeType.SVG },
            });

            expect(lcPicture.find('source').exists()).toBe(false);
            expect(lcPicture.html()).toMatchSnapshot();
        });

        test('should pass className to img', () => {
            const className = 'test-class-name';

            expect(
                mountLcPicture({ value: stringValue, className })
                    .find('img.lc-picture')
                    .hasClass(className)
            ).toBe(true);
        });
    });

    describe('should pass additional props', () => {
        test('should apply only style prop to container if dimension adjusting is disabled', () => {
            const style = { width: 250, opacity: 0.9 };

            const lcPicture = mountLcPicture({
                value: avatarValue,
                disableDimensionsAdjusting: true,
                style,
            });

            expect(
                lcPicture
                    .find('.lc-picture')
                    .prop('style')
            ).toEqual(style);
        });

        test('should apply width from style prop to container if dimension adjusting is disabled', () => {
            const lcPicture = mountLcPicture({
                value: avatarValue,
                disableDimensionsAdjusting: true,
                style: { width: '250px' },
            });

            const computedWidth = getComputedStyle(
                lcPicture
                    .find('.lc-picture')
                    .getDOMNode()
            ).getPropertyValue('width');

            expect(computedWidth).toBe('250px');
        });

        test('should apply width from style prop to container if dimension adjusting is enabled', () => {
            const lcPicture = mountLcPicture({
                value: avatarValue,
                style: { width: '250px' },
            });

            const computedWidth = getComputedStyle(
                lcPicture
                    .find('.lc-picture')
                    .getDOMNode()
            ).getPropertyValue('width');

            expect(computedWidth).toBe('250px');
        });
    });

    describe('should handle isVisible prop', () => {
        describe('isVisible is false', () => {
            test('should render thumbnail correctly', () => {
                const lcPicture = mountLcPicture({
                    value: avatarValue,
                    isVisible: false,
                });

                expect(lcPicture.html()).toMatchSnapshot();
            });

            test('should render correctly if thumbnail is disabled', () => {
                const lcPicture = mountLcPicture({
                    value: avatarValue,
                    isVisible: false,
                    useThumbnail: false,
                });

                expect(lcPicture.html()).toMatchSnapshot();
            });
        });

        describe('isVisible is true', () => {
            test('should render thumbnail and image correctly while image is not loaded', () => {
                const lcPicture = mountLcPicture({
                    value: avatarValue,
                });

                expect(lcPicture.html()).toMatchSnapshot();
            });

            test('should render correctly if thumbnail is disabled and image is not loaded', () => {
                const lcPicture = mountLcPicture({
                    value: avatarValue,
                    useThumbnail: false,
                });

                expect(lcPicture.html()).toMatchSnapshot();
            });

            test('should render thumbnail and image correctly after image is downloaded', () => {
                const lcPicture = mountLcPicture({
                    value: avatarValue,
                });

                lcPicture.find('.lc-picture__image').simulate('load');
                lcPicture.update();

                expect(lcPicture.html()).toMatchSnapshot();
            });

            test('should render correctly if thumbnail is disabled and image is downloaded', () => {
                const lcPicture = mountLcPicture({
                    value: avatarValue,
                    useThumbnail: false,
                });

                lcPicture.find('.lc-picture__image').simulate('load');
                lcPicture.update();

                expect(lcPicture.html()).toMatchSnapshot();
            });

            test('should render thumbnail and original image correctly after image is downloaded', () => {
                const lcPicture = mountLcPicture({
                    value: { ...avatarValue, useOrig: true },
                });

                lcPicture.find('.lc-picture__image').simulate('load');
                lcPicture.update();

                expect(lcPicture.html()).toMatchSnapshot();
            });
        });
    });
});
