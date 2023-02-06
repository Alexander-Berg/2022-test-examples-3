import * as React from 'react';
import { shallow, mount } from 'enzyme';
import { LcLinkPresenter as Link } from '../LcLink';

describe('<Link/> component', () => {
    let children: React.ReactNode;
    let url: string;
    let handleClick = jest.fn();

    beforeEach(() => {
        children = 'This is link';
        url = 'https://yandex.ru/';
        handleClick = jest.fn();
    });

    afterEach(() => {
        handleClick.mockClear();
    });

    test('should render component', () => {
        const props = {
            url,
            htmlAttrs: {
                title: 'Title',
                tabIndex: 2,
            },
            openInNewTab: true,
            mix: 'link-test',
            onClick: (_e: Event) => alert('test'),
        };

        const html = shallow(
            <Link {...props}><span><strong>Жирная</strong><em>курсивная</em>ссылка</span></Link>
        ).html();

        expect(html).toMatchSnapshot();
    });

    test('should render tag <a>', () => {
        const wrapper = mount(<Link url={url}>{children}</Link>);

        expect(wrapper.find('a').prop('href')).toBe(url);
    });

    test('should render pseudo link', () => {
        const wrapper = mount(<Link url="">{children}</Link>);

        expect(wrapper.find('span').hasClass('link_pseudo')).toBe(true);
    });

    test('should render children', () => {
        const wrapper = mount(<Link url={url}>{children}</Link>);

        expect(wrapper.text()).toBe(children);
    });

    test('should mix classes', () => {
        const mix = 'mix';
        const wrapper = shallow(<Link url={url} className={mix}>{children}</Link>);

        expect(wrapper.hasClass(mix)).toBe(true);
    });

    test('should handle click', () => {
        const wrapper = shallow(
            <Link url={url} onClick={handleClick}>
                {children}
            </Link>
        );

        wrapper.simulate('click', {});

        expect(handleClick).toHaveBeenCalled();
    });

    test('should render component without url', () => {
        const wrapper = mount(
            <Link>
                {children}
            </Link>
        );

        expect(wrapper.isEmptyRender()).toBe(false);
    });

    test('should render component without url with LpcMode', () => {
        const wrapper = mount(
            <Link
                isLpcMode
            >
                {children}
            </Link>
        );

        expect(wrapper.isEmptyRender()).toBe(false);
    });

    describe('extend url with page query', () => {
        test('should not extend url with page query', () => {
            const wrapper = mount(
                <Link url={url} lpcQuery={{ utm_source: 'gtm' }}>
                    {children}
                </Link>
            );

            expect(wrapper.find('a').prop('href')).toBe('https://yandex.ru/');
        });

        test('should extend url with page query in LPC mode', () => {
            const wrapper = mount(
                <Link
                    isLpcMode
                    url={url}
                    lpcQuery={{ utm_source: 'gtm' }}
                >
                    {children}
                </Link>
            );

            expect(wrapper.find('a').prop('href')).toBe('https://yandex.ru/?utm_source=gtm');
        });
    });

    describe('set "data-smooth-scroll" attribute', () => {
        test('should set "data-smooth-scroll" attribute if withSmoothScroll = true', () => {
            const wrapper = mount(
                <Link
                    url={url}
                    withSmoothScroll
                >
                    {children}
                </Link>
            );

            expect(wrapper.find('a').prop('data-smooth-scroll')).toEqual('true');
        });

        test('should set "data-smooth-scroll" attribute in LPC mode if url is anchor', () => {
            const wrapper = mount(
                <Link
                    isLpcMode
                    url={'#anchor'}
                >
                    {children}
                </Link>
            );

            expect(wrapper.find('a').prop('data-smooth-scroll')).toEqual('true');
        });

        test('should not set "data-smooth-scroll" attribute', () => {
            const wrapper = mount(
                <Link
                    url={url}
                >
                    {children}
                </Link>
            );

            expect(wrapper.find('a').prop('data-smooth-scroll')).toBeUndefined;
        });
    });
});
