import React from 'react';
import PropTypes from 'prop-types';
import {findDOMNode} from 'react-dom';
import {decl} from '../../../../i-bem/i-bem.react';
import Button from 'b:button2 m:theme=normal|action|clear m:size=xs|s|m|l|n m:tone=default|grey|red|dark m:view=classic|default'; // eslint-disable-line
import Tooltip from 'b:tooltip m:theme=success|normal|error|promo m:size=xs|s|m|l|n m:tone=default|grey|red|dark m:view=classic|default'; // eslint-disable-line
import 'b:tooltip e:corner m:star=yes';

export default decl({
    block: 'example',
    willInit() {
        this.state = {
            tooltipVisible: false
        };

        this.onButtonClick = this.onButtonClick.bind(this);
        this.onOutsideClick = this.onOutsideClick.bind(this);
    },
    didMount() {
        // FIXME Не работает открытие Popup при загрузке, https://st.yandex-team.ru/ISL-4020
        var domNode = findDOMNode(this._b);
        domNode.click();
        domNode.blur();
    },
    mods({testcase, theme, tail, view}) {
        return {
            ...this.__base(...arguments),
            testcase,
            theme,
            tail,
            view
        };
    },
    content({theme, size, testcase, multiline, tail, view, tone}) {
        return [
            <Button
                view={view}
                tone={tone}
                key='button'
                ref={b => this._b = b}
                theme='normal'
                {...{size}}
                onClick={this.onButtonClick}>owner</Button>,
            <Tooltip
                view={view}
                tone={tone}
                key='tooltip'
                visible={this.state.tooltipVisible}
                anchor={this._b}
                autoclosable
                onOutsideClick={this.onOutsideClick}
                to={theme === 'promo' ? 'right-bottom' : 'right'}
                tail={tail !== 'without'}
                {...{theme, size, testcase}}>{
                multiline ? ['This is the tooltip', <br key='br' />, 'multiline example']
                    : theme === 'promo' ? [
                        size === 's' ? [
                            <Tooltip.Description key='description'>Добавьте СМИ в избранное</Tooltip.Description>,
                            <Tooltip.Corner key='corner' mods={{star: 'yes'}}/>
                        ] : [
                            <Tooltip.Description key='description'>
                                Добавляйте понравившиеся ролики в закладки
                            </Tooltip.Description>,
                            <Tooltip.Buttons key='buttons'>
                                <Button view='classic' tone='default' size='m'
                                    theme='clear'>Не хочу</Button>
                                <Button view='classic' tone='default' size='m'
                                    theme='action'>Добавить</Button>
                            </Tooltip.Buttons>
                        ]
                    ] : 'Tooltip'
            }</Tooltip>
        ];
    },
    onButtonClick() {
        this.setState({tooltipVisible: !this.state.tooltipVisible});
    },
    onOutsideClick() {
        this.setState({tooltipVisible: false});
    }
}, {
    propTypes: {
        theme: PropTypes.string,
        size: PropTypes.string,
        testcase: PropTypes.string,
        multiline: PropTypes.bool,
        tail: PropTypes.string
    },
    defaultProps: {
        size: 'm',
        theme: 'normal'
    }
});
