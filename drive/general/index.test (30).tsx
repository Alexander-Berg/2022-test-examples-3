import { mount, shallow } from 'enzyme';
import React from 'react';

import Select from './index';

const STRING_OPTIONS_ARRAY = [{ value: 'a', text: 'text a', description: 'description a' }];
const NUMBER_OPTIONS_ARRAY = [{ value: 0, text: 'text 0', description: 'description 0' }];

describe('Select', () => {

    describe('Renders', () => {
        it('Renders without options', () => {
            const wrapper = shallow(<Select options={[]}
                                            onSelect={() => {
                                            }}/>);
            expect(wrapper.exists()).toEqual(true);
        });

        it('Renders with string options', () => {
            const wrapper = shallow(<Select options={STRING_OPTIONS_ARRAY}
                                            onSelect={() => {
                                            }}/>);
            expect(wrapper.exists()).toEqual(true);
        });

        it('Renders with number options', () => {
            const wrapper = shallow(<Select options={NUMBER_OPTIONS_ARRAY}
                                            onSelect={() => {
                                            }}/>);
            expect(wrapper.exists()).toEqual(true);
        });
    });

    describe('Format Options', () => {
        it('Format Options without options', () => {
            const wrapper: any = shallow(<Select options={[]}
                                                 onSelect={() => {
                                                 }}/>);
            expect(wrapper.instance()?.state?.options).toEqual([]);
        });

        it('Format Options with string options', () => {
            const wrapper: any = shallow(<Select options={STRING_OPTIONS_ARRAY}
                                                 onSelect={() => {
                                                 }}/>);
            expect(wrapper.instance()?.state?.options).toEqual([{
                info: Object.assign(STRING_OPTIONS_ARRAY[0], { selectedDisplayText: STRING_OPTIONS_ARRAY[0].text }),
                settings: {
                    isSelected: false,
                    isOdd: false,
                },
            }]);
        });

        it('Format Options with number options', () => {
            const wrapper: any = shallow(<Select options={NUMBER_OPTIONS_ARRAY}
                                                 onSelect={() => {
                                                 }}/>);
            expect(wrapper.instance()?.state?.options).toEqual([{
                info: Object.assign(NUMBER_OPTIONS_ARRAY[0], { selectedDisplayText: NUMBER_OPTIONS_ARRAY[0].text }),
                settings: {
                    isSelected: false,
                    isOdd: false,
                },
            }]);
        });

        it('Format Options with string text', () => {
            const OPTIONS: any[] = [{ value: 'a', text: 'text a', description: 'description a' }];

            const wrapper: any = shallow(<Select options={OPTIONS}
                                                 onSelect={() => {
                                                 }}/>);
            expect(wrapper.instance()?.state?.options).toEqual([{
                info: Object.assign(OPTIONS[0], { selectedDisplayText: OPTIONS[0].text }),
                settings: {
                    isSelected: false,
                    isOdd: false,
                },
            }]);
        });

        it('Format Options with number text', () => {
            const OPTIONS: any[] = [{ value: 'a', text: 123, description: 'description a' }];

            const wrapper: any = shallow(<Select options={OPTIONS}
                                                 onSelect={() => {
                                                 }}/>);
            expect(wrapper.instance()?.state?.options).toEqual([{
                info: Object.assign(OPTIONS[0], { selectedDisplayText: '123' }),
                settings: {
                    isSelected: false,
                    isOdd: false,
                },
            }]);
        });

        it('Format Options with boolean text', () => {
            const OPTIONS: any[] = [{ value: 'a', text: true, description: 'description a' }];

            const wrapper: any = shallow(<Select options={OPTIONS}
                                                 onSelect={() => {
                                                 }}/>);
            expect(wrapper.instance()?.state?.options).toEqual([{
                info: Object.assign(OPTIONS[0], { selectedDisplayText: 'true' }),
                settings: {
                    isSelected: false,
                    isOdd: false,
                },
            }]);
        });
    });

    describe('Format initialValues', () => {
        it('Format Options without options and without initialValues', () => {
            const wrapper: any = shallow(<Select options={[]}
                                                 onSelect={() => {
                                                 }}/>);
            expect(wrapper.instance()?.state?.options).toEqual([]);
        });

        it('Format Options without options and with string initialValues', () => {
            const wrapper: any = shallow(<Select initialValues={['a']}
                                                 options={[]}
                                                 onSelect={() => {
                                                 }}/>);
            expect(wrapper.instance()?.state?.options).toEqual([{
                info: { text: 'a', value: 'a' }, settings: {
                    isSelected: true,
                    isOdd: true,
                },
            }]);
        });

        it('Format Options without options and with number initialValues', () => {
            const wrapper: any = shallow(<Select initialValues={[0]}
                                                 options={[]}
                                                 onSelect={() => {
                                                 }}/>);
            expect(wrapper.instance()?.state?.options).toEqual([{
                info: { text: 0, value: 0 }, settings: {
                    isSelected: true,
                    isOdd: true,
                },
            }]);
        });

        it('Format Options with options and with string initialValues', () => {
            const wrapper: any = shallow(<Select initialValues={['a']}
                                                 options={STRING_OPTIONS_ARRAY}
                                                 onSelect={() => {
                                                 }}/>);
            expect(wrapper.instance()?.state?.options).toEqual([{
                info: STRING_OPTIONS_ARRAY[0],
                settings: {
                    isSelected: true,
                    isOdd: false,
                },
            }]);
        });

        it('Format Options with options and with number initialValues', () => {
            const wrapper: any = shallow(<Select initialValues={[0]}
                                                 options={NUMBER_OPTIONS_ARRAY}
                                                 onSelect={() => {
                                                 }}/>);
            expect(wrapper.instance()?.state?.options).toEqual([{
                info: NUMBER_OPTIONS_ARRAY[0],
                settings: {
                    isSelected: true,
                    isOdd: false,
                },
            }]);
        });

        it('Format Options with options and with odd string initialValues', () => {
            const wrapper: any = shallow(<Select initialValues={['b']}
                                                 options={STRING_OPTIONS_ARRAY}
                                                 onSelect={() => {
                                                 }}/>);
            expect(wrapper.instance()?.state?.options).toEqual([{
                info: STRING_OPTIONS_ARRAY[0],
                settings: {
                    isSelected: false,
                    isOdd: false,
                },
            }, {
                info: { text: 'b', value: 'b' }, settings: {
                    isSelected: true,
                    isOdd: true,
                },
            }]);
        });

        it('Format Options with options and with odd number initialValues', () => {
            const wrapper: any = shallow(<Select initialValues={[1]}
                                                 options={NUMBER_OPTIONS_ARRAY}
                                                 onSelect={() => {
                                                 }}/>);
            expect(wrapper.instance()?.state?.options).toEqual([{
                info: NUMBER_OPTIONS_ARRAY[0],
                settings: {
                    isSelected: false,
                    isOdd: false,
                },
            }, {
                info: { text: 1, value: 1 }, settings: {
                    isSelected: true,
                    isOdd: true,
                },
            }]);
        });
    });

    describe('Show right selected values', () => {
        it('Show right selected string value', () => {

            const OPTIONS: any[] = [{ value: 'a', text: 'text a' }];

            const wrapper: any = mount(<Select multiSelect
                                               initialValues={['a']}
                                               options={OPTIONS}
                                               onSelect={() => {
                                               }}/>);

            const values_container = wrapper.find('.values_container');
            const current_value = values_container.find('.current_value');
            const currentFirstValue = current_value.at(0).text();

            expect(currentFirstValue).toEqual('text a');
        });

        it('Show right selected react element value', () => {

            const OPTIONS: any[] = [{ value: 'a', text: <span>text a</span> }];

            const wrapper: any = mount(<Select multiSelect
                                               initialValues={['a']}
                                               options={OPTIONS}
                                               onSelect={() => {
                                               }}/>);

            const values_container = wrapper.find('.values_container');
            const current_value = values_container.find('.current_value');
            const currentFirstValue = current_value.at(0).text();

            expect(currentFirstValue).toEqual('text a');
        });

        it('Show right selected number value', () => {

            const OPTIONS: any[] = [{ value: 'a', text: 123 }];

            const wrapper: any = mount(<Select multiSelect
                                               initialValues={['a']}
                                               options={OPTIONS}
                                               onSelect={() => {
                                               }}/>);

            const values_container = wrapper.find('.values_container');
            const current_value = values_container.find('.current_value');
            const currentFirstValue = current_value.at(0).text();

            expect(currentFirstValue).toEqual('123');
        });

        it('Show right selected boolean true value', () => {

            const OPTIONS: any[] = [{ value: 'a', text: true }];

            const wrapper: any = mount(<Select multiSelect
                                               initialValues={['a']}
                                               options={OPTIONS}
                                               onSelect={() => {
                                               }}/>);

            const values_container = wrapper.find('.values_container');
            const current_value = values_container.find('.current_value');
            const currentFirstValue = current_value.at(0).text();

            expect(currentFirstValue).toEqual('true');
        });

        it('Show right selected boolean false value', () => {

            const OPTIONS: any[] = [{ value: 'a', text: false }];

            const wrapper: any = mount(<Select multiSelect
                                               initialValues={['a']}
                                               options={OPTIONS}
                                               onSelect={() => {
                                               }}/>);

            const values_container = wrapper.find('.values_container');
            const current_value = values_container.find('.current_value');
            const currentFirstValue = current_value.at(0).text();

            expect(currentFirstValue).toEqual('false');
        });
    });
});
