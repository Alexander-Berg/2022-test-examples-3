import React from 'react';
import { renderToString } from 'react-dom/server';
import { CoverLayer, CoverLayerProps } from '../index';

describe('CoverLayer', () => {
    describe('browserless', () => {
        const sit = (name: string, params: CoverLayerProps) => {
            it(name, () => {
                expect(renderToString(<CoverLayer { ...params } className="fixed" />)).toMatchSnapshot();
            });
        };

        sit('should render empty result with no args', {});

        describe('image', () => {
            sit('should render simple img', {
                imageUrl: 'https://example.org/img.jpg',
            });
            sit('should handle position', {
                imageUrl: 'https://example.org/img.jpg',
            });
            sit('should handle contentPosition', {
                contentPosition: 'bottom right',
                imageUrl: 'https://example.org/img.jpg',
            });
            sit('should render srcset img', {
                imageUrl: {
                    jpeg: 'https://example.org/img.jpg',
                    webp: 'https://example.org/img.webp',
                },
            });
            sit('should render stop list img', {
                imageUrl: {
                    '1280': 'https://example.org/img-1280.jpg',
                    '1366': 'https://example.org/img-1366.jpg',
                    '1536': 'https://example.org/img-1536.jpg',
                    '1920': 'https://example.org/img-1920.jpg',
                },
            });
            sit('should render stop srcset list img', {
                imageUrl: {
                    '1280': {
                        jpeg: 'https://example.org/img-1280.jpg',
                        webp: 'https://example.org/img-1280.webp',
                    },
                    '1366': {
                        jpeg: 'https://example.org/img-1366.jpg',
                        webp: 'https://example.org/img-1366.webp',
                    },
                    '1536': {
                        jpeg: 'https://example.org/img-1536.jpg',
                        webp: 'https://example.org/img-1536.webp',
                    },
                    '1920': {
                        jpeg: 'https://example.org/img-1920.jpg',
                        webp: 'https://example.org/img-1920.webp',
                    },
                },
            });
        });

        describe('video', () => {
            sit('should render simple video', {
                videoUrl: 'https://example.org/video.mp4',
            });
            sit('should handle position', {
                imageUrl: 'https://example.org/video.mp4',
            });
            sit('should handle contentPosition', {
                contentPosition: 'bottom right',
                imageUrl: 'https://example.org/video.mp4',
            });
            sit('should render srcset video', {
                videoUrl: {
                    mpeg: 'https://example.org/video.mp4',
                    webm: 'https://example.org/video.webm',
                },
            });
        });
    });
});
