import { render, shallow } from 'enzyme';
import * as React from 'react';

import { deepCopy } from '../../../../utils/utils';
import { CommonMessageContainer } from '../CommonMessageContainer';
import { CommonMessageFooter } from '../CommonMessageFooter';
import { CommonMessageHeader } from '../CommonMessageHeader';
import { AUTHOR_1, CHAT, ITEM } from './MockedData';

const COMMON_PROPS = {
    item: ITEM,
    chat: CHAT,
    isViewedMessage: false,
    isOriginator: false,
    isRobot: false,
    isSelected: false,
    changeSelectedMessages: () => {},
    replaceMessage: () => {},
    removeMessage: () => {},
};

describe('Common message header snapshot', () => {
    it('Correct render common message', () => {
        const component = render(
            <CommonMessageHeader href={''}
                                 author={AUTHOR_1}
                                 isStaffOnly={false}
                                 changeSelectedMessages={() => {}}
                                 isSafeMode={false}
                                 isOriginator={false}
                                 isSelected={false}/>,
        );
        expect(component).toMatchSnapshot();
    });

    it('Correct render for staff only message', () => {
        const component = render(
            <CommonMessageHeader href={''}
                                 author={AUTHOR_1}
                                 isStaffOnly={true}
                                 changeSelectedMessages={() => {}}
                                 isSafeMode={false}
                                 isOriginator={false}
                                 isSelected={false}/>,
        );
        expect(component).toMatchSnapshot();
    });

    it('Correct render with safe mode', () => {
        const component = render(
            <CommonMessageHeader href={''}
                                 author={AUTHOR_1}
                                 isStaffOnly={false}
                                 changeSelectedMessages={() => {}}
                                 isSafeMode={true}
                                 isOriginator={false}
                                 isSelected={false}/>,
        );
        expect(component).toMatchSnapshot();
    });

    it('Correct render with forward ability', () => {
        const component = render(
            <CommonMessageHeader href={''}
                                 author={AUTHOR_1}
                                 isStaffOnly={false}
                                 changeSelectedMessages={() => {}}
                                 isSafeMode={true}
                                 isOriginator={true}
                                 isSelected={false}/>,
        );
        expect(component).toMatchSnapshot();
    });

    it('Correct render with active checkbox', () => {
        const component = render(
            <CommonMessageHeader href={''}
                                 author={AUTHOR_1}
                                 isStaffOnly={false}
                                 changeSelectedMessages={() => {}}
                                 isSafeMode={true}
                                 isOriginator={true}
                                 isSelected={true}/>,
        );
        expect(component).toMatchSnapshot();
    });
});

describe('Common message footer snapshot', () => {
    const TEST_PROPS = deepCopy(COMMON_PROPS);

    it('Doesnt viewed doesnt originator message', () => {
        const component = render(<CommonMessageFooter {...TEST_PROPS}/>);
        expect(component).toMatchSnapshot();
    });

    it('Viewed message', () => {
        TEST_PROPS.isViewedMessage = true;

        const component = render(<CommonMessageFooter {...TEST_PROPS}/>);
        expect(component).toMatchSnapshot();
    });

    it('Originator message', () => {
        TEST_PROPS.isOriginator = true;

        const component = render(<CommonMessageFooter {...TEST_PROPS}/>);
        expect(component).toMatchSnapshot();
    });

    it('Robot message', () => {
        TEST_PROPS.isRobot = true;

        const component = render(<CommonMessageFooter {...TEST_PROPS}/>);
        expect(component).toMatchSnapshot();
    });

    it('Select passive', () => {
        TEST_PROPS.isRobot = false;
        TEST_PROPS.isOriginator = true;
        TEST_PROPS.isSelected = false;

        const component = shallow(<CommonMessageFooter {...TEST_PROPS}/>);
        expect(component).toMatchSnapshot();
    });

    it('Select active', () => {
        TEST_PROPS.isRobot = false;
        TEST_PROPS.isOriginator = true;
        TEST_PROPS.isSelected = true;

        const component = render(<CommonMessageFooter {...TEST_PROPS}/>);
        expect(component).toMatchSnapshot();
    });

    it('Doesnt originator doesnt viewed message', () => {
        TEST_PROPS.isOriginator = false;
        TEST_PROPS.isViewedMessage = false;

        const component = render(<CommonMessageFooter {...TEST_PROPS}/>);
        expect(component).toMatchSnapshot();
    });
});

describe('Common message container snapshot', () => {
    const MESSAGE_CHILDREN = <div>Какое-то соообщение</div>;
    const TEST_PROPS: any = {
        ...COMMON_PROPS,
        author: AUTHOR_1,
        href: '',
        isStaffOnly: false,
        isSafeMode: false,
        isSelected: false,
        isOriginator: false,
        changeSelectedMessages: () => {},
    };

    it('Correct render common message', () => {
        const component = render(
            <CommonMessageContainer {...TEST_PROPS}>{MESSAGE_CHILDREN}</CommonMessageContainer>,
        );
        expect(component).toMatchSnapshot();
    });

    it('Originator message', () => {
        TEST_PROPS.isOriginator = true;

        const component = render(
            <CommonMessageContainer {...TEST_PROPS}>{MESSAGE_CHILDREN}</CommonMessageContainer>,
        );
        expect(component).toMatchSnapshot();
    });

    it('Robot message', () => {
        TEST_PROPS.isRobot = true;
        const component = render(
            <CommonMessageContainer {...TEST_PROPS}>{MESSAGE_CHILDREN}</CommonMessageContainer>,
        );
        expect(component).toMatchSnapshot();
    });

    it('Staff only message', () => {
        TEST_PROPS.isStaffOnly = true;
        const component = render(
            <CommonMessageContainer {...TEST_PROPS}>{MESSAGE_CHILDREN}</CommonMessageContainer>,
        );
        expect(component).toMatchSnapshot();
    });

    it('Originator message with error', () => {
        TEST_PROPS.isOriginator = true;
        TEST_PROPS.extraError = new Error();

        const component = shallow(
            <CommonMessageContainer {...TEST_PROPS}>{MESSAGE_CHILDREN}</CommonMessageContainer>,
        );

        expect(component.exists('.extra_error')).toEqual(true);
    });
});
