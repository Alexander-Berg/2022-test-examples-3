import React from 'react';
import ReactDOM from 'react-dom';
import {decl} from '../../../../i-bem/i-bem.react';
import Button from 'b:button2';

export default decl({
    block: 'test-rfs',
    willInit() {
        const doc = document;

        this._fullscreenElement = 'fullscreenElement' in doc
            ? 'fullscreenElement' : 'webkitFullscreenElement' in doc
                ? 'webkitFullscreenElement' : 'mozFullScreenElement' in doc
                    ? 'mozFullScreenElement' : 'msFullscreenElement' in doc
                        ? 'msFullscreenElement' : '',
        this._exitFullscreen = doc.exitFullscreen
            ? 'exitFullscreen' : doc.webkitExitFullscreen
                ? 'webkitExitFullscreen' : doc.mozCancelFullScreen
                    ? 'mozCancelFullScreen' : doc.msExitFullscreen
                        ? 'msExitFullscreen' : '',

        this.onFullScreenButtonClick = this.onFullScreenButtonClick.bind(this);
    },
    onFullScreenButtonClick() {
        const testNode = ReactDOM.findDOMNode(this);

        if(this._fullscreenElement && document[this._fullscreenElement]) {
            document[this._exitFullscreen]();
            return;
        }

        testNode._requestFullscreen = testNode.requestFullscreen
            ? 'requestFullscreen' : testNode.webkitRequestFullscreen
                ? 'webkitRequestFullscreen' : testNode.mozRequestFullScreen
                    ? 'mozRequestFullScreen' : testNode.msRequestFullscreen
                        ? 'msRequestFullscreen' : '';

        if(testNode._requestFullscreen) {
            testNode[testNode._requestFullscreen]();
        }

        this.props.onClick && this.props.onClick();
    },
    attrs() {
        return {
            style: {position: 'relative'}
        };
    },
    content({children, buttonRef}) {
        return [].concat(
            <Button
                {...{
                    theme: 'normal',
                    key: 'fullScreenButton',
                    size: 'm',
                    onClick: this.onFullScreenButtonClick,
                    attrs: {...buttonRef}
                }}>
                    toggle fullscreen
            </Button>,
            children);
    }
});
