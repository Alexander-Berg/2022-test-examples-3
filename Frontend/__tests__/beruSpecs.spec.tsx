import * as React from 'react';
import { shallow, ShallowWrapper } from 'enzyme';
import { omit } from 'lodash';
import { BeruSpecs, IBeruSpecs } from '../BeruSpecs';
import { BeruSpecsModal } from '../Modal/BeruSpecsModal';
import * as stubData from '../datastub';

describe('Компонент BeruSpecs', () => {
    const dataDefault = stubData.dataDefault as IBeruSpecs;
    const dataDescription = omit(dataDefault, 'specs');
    const dataSpecs = omit(dataDefault, 'description');

    let wrapperDefault: ShallowWrapper;
    let wrapperDescription: ShallowWrapper;
    let wrapperSpecs: ShallowWrapper;

    describe('с параметром `mode = label`', () => {
        beforeEach(() => {
            wrapperDefault = shallow(<BeruSpecs {...dataDefault} mode={'label'} />);
            wrapperDescription = shallow(<BeruSpecs {...dataDescription} mode={'label'} />);
            wrapperSpecs = shallow(<BeruSpecs {...dataSpecs} mode={'label'} />);
        });

        it('должен отрендериться без ошибок', () => {
            expect(wrapperDefault.exists()).toBe(true);
            expect(wrapperDescription.exists()).toBe(true);
            expect(wrapperSpecs.exists()).toBe(true);
        });

        it('не является блоком "Описание и характеристики"', () => {
            expect(wrapperDefault.find('.beru-specs__content').exists()).toBe(false);
            expect(wrapperSpecs.find('.beru-specs__content').exists()).toBe(false);
            expect(wrapperDescription.find('.beru-specs__content').exists()).toBe(false);
        });

        describe('если передали description', () => {
            it('имеет состояние `showDescription = true`', () => {
                expect(wrapperDefault.state('showDescription')).toBe(true);
                expect(wrapperDescription.state('showDescription')).toBe(true);
            });
        });

        describe('если НЕ передали description', () => {
            it('имеет состояние `showDescription = false`', () => {
                expect(wrapperSpecs.state('showDescription')).toBe(false);
            });
        });

        describe('если передали характеристики', () => {
            it('имеет состояние `showSpecs = true`', () => {
                expect(wrapperDefault.state('showSpecs')).toBe(true);
                expect(wrapperSpecs.state('showSpecs')).toBe(true);
            });

            it('содержит иконку с текстом "Характеристики"', () => {
                expect(wrapperDefault.find('BeruSpecsLabel').exists()).toBe(true);
                expect(wrapperSpecs.find('BeruSpecsLabel').exists()).toBe(true);
            });
        });

        describe('если НЕ передали характеристики', () => {
            it('имеет состояние `showSpecs = false`', () => {
                expect(wrapperDescription.state('showSpecs')).toBe(false);
            });

            it('не содержит иконку с текстом "Характеристики"', () => {
                expect(wrapperDescription.find('BeruSpecsLabel').exists()).toBe(false);
            });
        });

        it('имеет состояние `showPopup = false`', () => {
            expect(wrapperDefault.state('showPopup')).toBe(false);
            expect(wrapperSpecs.state('showPopup')).toBe(false);
            expect(wrapperDescription.state('showPopup')).toBe(false);
        });

        it('не отображает попап', () => {
            expect(wrapperDefault.find(BeruSpecsModal).exists()).toBe(false);
            expect(wrapperSpecs.find(BeruSpecsModal).exists()).toBe(false);
            expect(wrapperDescription.find(BeruSpecsModal).exists()).toBe(false);
        });

        describe('по клику на "Характеристики"', () => {
            beforeEach(() => {
                wrapperDefault.find('BeruSpecsLabel').simulate('click');
                wrapperSpecs.find('BeruSpecsLabel').simulate('click');
            });

            it('меняет состояние на `showPopup = true`', () => {
                expect(wrapperDefault.state('showPopup')).toBe(true);
                expect(wrapperSpecs.state('showPopup')).toBe(true);
            });

            it('отображает попап', () => {
                expect(wrapperDefault.find(BeruSpecsModal).exists()).toBe(true);
                expect(wrapperSpecs.find(BeruSpecsModal).exists()).toBe(true);
            });

            describe('в открывшемся попапе', () => {
                it('отображает верный заголовок', () => {
                    expect(wrapperDefault.find('BeruSpecsTitle').render().text()).toBe('характеристики и описание');
                    expect(wrapperSpecs.find('BeruSpecsTitle').render().text()).toBe('характеристики');
                });

                it('отображает полное описание если передали description', () => {
                    const input = dataDefault.description!.full;
                    const doc = new DOMParser().parseFromString(input, 'text/html');
                    const text = doc.body.textContent;

                    expect(wrapperDefault.find('BeruSpecsDescription').exists()).toBe(true);
                    expect(wrapperDefault.find('BeruSpecsDescription').render().text()).toBe(text);
                });

                it('НЕ отображает полное описание если НЕ передали description', () => {
                    expect(wrapperSpecs.find('BeruSpecsDescription').exists()).toBe(false);
                });

                it('отображает все блоки со спеками', () => {
                    expect(wrapperDefault.find('.beru-specs__group-specs').length).toBe(dataDefault.specs!.length);
                    expect(wrapperSpecs.find('.beru-specs__group-specs').length).toBe(dataSpecs.specs!.length);
                });

                it('кол-во характеристик совпадает с переданными данными', () => {
                    const countDefault = dataDefault.specs!.reduce((sum, { groupSpecs }) => (sum + groupSpecs!.length), 0);
                    const countSpecs = dataSpecs.specs!.reduce((sum, { groupSpecs }) => (sum + groupSpecs!.length), 0);

                    expect(wrapperDefault.find('BeruSpecsItem').length).toBe(countDefault);
                    expect(wrapperSpecs.find('BeruSpecsItem').length).toBe(countSpecs);
                });
            });
        });
    });

    describe('при значении параметре `mode = preview`', () => {
        beforeEach(() => {
            wrapperDefault = shallow(<BeruSpecs {...dataDefault} mode={'preview'} />);
            wrapperDescription = shallow(<BeruSpecs {...dataDescription} mode={'preview'} />);
            wrapperSpecs = shallow(<BeruSpecs {...dataSpecs} mode={'preview'} />);
        });

        it('должен отрендериться без ошибок', () => {
            expect(wrapperDefault.exists()).toBe(true);
            expect(wrapperDescription.exists()).toBe(true);
            expect(wrapperSpecs.exists()).toBe(true);
        });

        it('не содеожит иконку с текстом "Характеристики"', () => {
            expect(wrapperDefault.find('BeruSpecsLabel').exists()).toBe(false);
            expect(wrapperDescription.find('BeruSpecsLabel').exists()).toBe(false);
            expect(wrapperSpecs.find('BeruSpecsLabel').exists()).toBe(false);
        });

        it('является блоком "Описание и характеристики"', () => {
            expect(wrapperDefault.find('.beru-specs__content').exists()).toBe(true);
            expect(wrapperDescription.find('.beru-specs__content').exists()).toBe(true);
            expect(wrapperSpecs.find('.beru-specs__content').exists()).toBe(true);
        });

        it('содержит заголовок', () => {
            expect(wrapperDefault.find('BeruSpecsTitle').exists()).toEqual(true);
            expect(wrapperDescription.find('BeruSpecsTitle').exists()).toEqual(true);
            expect(wrapperSpecs.find('BeruSpecsTitle').exists()).toEqual(true);
        });

        describe('если передали description', () => {
            it('содержит краткое описание', () => {
                expect(wrapperDefault.find('BeruSpecsDescription').exists()).toBe(true);
            });

            it('текст краткого описания соответствует переданному', () => {
                const input = dataDefault.description!.short;
                const doc = new DOMParser().parseFromString(input, 'text/html');
                const text = doc.body.textContent;

                expect(wrapperDefault.find('BeruSpecsDescription').render().text().length)
                    .toBeLessThan(Math.min(text!.length || 203));
            });
        });

        describe('если НЕ передали description', () => {
            it('не содержит краткого описания', () => {
                expect(wrapperSpecs.find('BeruSpecsDescription').exists()).toBe(false);
            });
        });

        describe('если передали характеристики', () => {
            it('содержит не более 5 характеристик', () => {
                expect(wrapperDefault.find('BeruSpecsItem').length).toBeLessThanOrEqual(5);
            });

            it('содержит кнопку "Посмотреть все характеристики"', () => {
                expect(wrapperDefault.find('BeruSpecsMoreButton').exists()).toBe(true);
            });
        });

        describe('если НЕ передали характеристики', () => {
            it('НЕ содержит характеристик', () => {
                expect(wrapperDescription.find('BeruSpecsItem').length).toBe(0);
            });

            it('НЕ содержит кнопку "Посмотреть все характеристики"', () => {
                expect(wrapperDescription.find('BeruSpecsMoreButton').exists()).toBe(false);
            });
        });

        it('не отображает попап', () => {
            expect(wrapperDefault.state('showPopup')).toBe(false);
            expect(wrapperDefault.find(BeruSpecsModal).exists()).toBe(false);
        });

        describe('по клику на "посмотреть все характеристики"', () => {
            beforeEach(() => {
                wrapperDefault.find('BeruSpecsMoreButton').simulate('click');
                wrapperSpecs.find('BeruSpecsMoreButton').simulate('click');
            });

            it('меняет состояние на `showPopup = true`', () => {
                expect(wrapperDefault.state('showPopup')).toBe(true);
                expect(wrapperSpecs.state('showPopup')).toBe(true);
            });

            it('отображает попап', () => {
                expect(wrapperDefault.find(BeruSpecsModal).exists()).toBe(true);
                expect(wrapperSpecs.find(BeruSpecsModal).exists()).toBe(true);
            });

            describe('в открывшемся попапе', () => {
                let modalDefault: ShallowWrapper;
                let modalSpec: ShallowWrapper;

                beforeEach(() => {
                    modalDefault = wrapperDefault.find(BeruSpecsModal);
                    modalSpec = wrapperSpecs.find(BeruSpecsModal);
                });

                it('отображает верный заголовок', () => {
                    expect(modalDefault.find('BeruSpecsTitle').render().text()).toBe('характеристики и описание');
                    expect(modalSpec.find('BeruSpecsTitle').render().text()).toBe('характеристики');
                });

                it('отображает полное описание если передали description', () => {
                    const input = dataDefault.description!.full;
                    const doc = new DOMParser().parseFromString(input, 'text/html');
                    const text = doc.body.textContent;

                    expect(modalDefault.find('BeruSpecsDescription').exists()).toBe(true);
                    expect(modalDefault.find('BeruSpecsDescription').render().text()).toBe(text);
                });

                it('НЕ отображает полное описание если НЕ передали description', () => {
                    expect(modalSpec.find('BeruSpecsDescription').exists()).toBe(false);
                });

                it('отображает все блоки со спеками', () => {
                    expect(modalDefault.find('.beru-specs__group-specs').length).toBe(dataDefault.specs!.length);
                    expect(modalSpec.find('.beru-specs__group-specs').length).toBe(dataSpecs.specs!.length);
                });

                it('кол-во характеристик совпадает с переданными данными', () => {
                    const countDefault = dataDefault.specs!.reduce((sum, { groupSpecs }) => (sum + groupSpecs!.length), 0);
                    const countSpecs = dataSpecs.specs!.reduce((sum, { groupSpecs }) => (sum + groupSpecs!.length), 0);

                    expect(modalDefault.find('BeruSpecsItem').length).toBe(countDefault);
                    expect(modalSpec.find('BeruSpecsItem').length).toBe(countSpecs);
                });
            });
        });
    });
});
