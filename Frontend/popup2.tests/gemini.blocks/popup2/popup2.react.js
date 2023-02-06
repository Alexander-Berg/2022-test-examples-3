import React, {Children} from 'react';
import {findDOMNode} from 'react-dom';
import PopupTail from 'e:tail';
import {decl} from '../../../../i-bem/i-bem.react';

export default decl({
    block: 'popup2',
    // Прикрепить popup к b-page, а не к body
    // TODO: Оторвать после https://st.yandex-team.ru/ISL-4265
    didMount() {
        this.hasDOMNode = false;
        this._container = document.createElement('div');
        document.getElementsByClassName('b-page__body')[0].appendChild(this._container);
        this.redraw(this.props);
    },
    // Приходится переопределять весь метод, поскольку hasTail приходит из props
    // и используется здесь несколько раз
    // TODO: https://st.yandex-team.ru/ISL-4069
    redraw(props) {
        const {
            tailSize, children, visible
        } = props;

        let hasTail;

        let mappedChildren = Children.map(children, (child, i) => {
            if(child.type && child.type.displayName === 'popup2__tail') {
                hasTail = true;
            } else {
                return child;
            }
        });

        if(visible) {
            const {
                left, top, tail, direction
            } = this._calcBestDrawingParams(props);

            this._captureZIndex();

            props.calcPossible && this._calcPossibleDrawingParams(props);

            this._style.popup = {...this._style.popup, ...{left, top}, ...props.style};
            this._direction = direction;

            if(hasTail) {
                this._style.tail = {...tail, width: tailSize, height: tailSize};
            }
        }

        const className = this.__self.classBuilder.stringify(
            this.block,
            this.elem,
            this.mods(props),
            this.__self.classBuilder.joinMixes(this.mix(props)),
            this.cls(props)
        );

        const Tag = this.tag(props);
        const popup = (
            <Tag
                className={className}
                style={{...this._style.popup}}
                {...this.attrs(props)}
                ref={p => this.domElementPopup = findDOMNode(p)}>
                {hasTail && <PopupTail
                    style={this._style.tail}
                    ref={t => this.domElementTail = findDOMNode(t)} />}
                {mappedChildren}
            </Tag>
        );

        this.__self.inject(this, popup, this._container);
    }
});
