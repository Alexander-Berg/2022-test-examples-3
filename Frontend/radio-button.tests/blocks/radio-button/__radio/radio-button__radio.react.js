import {decl} from '../../../../../i-bem/i-bem.react';

export default decl({
  block: 'radio-button',
  elem: 'radio',
  mods() {
    return this.__base.call(this, this._processArguments());
  },
  content() {
    return this.__base.call(this, this._processArguments());
  },
  _processArguments() {
    const {controlAttrs} = this.props;
    const value = controlAttrs ? controlAttrs.value : this.props.value;

    return Object.assign({}, this.props, {value});
  },

  // Оторвать после https://st.yandex-team.ru/ISL-4289
  _isChecked(value, mainValue) {
    const checked = this.props.checked || (value !== undefined && value === mainValue);

    return this.__self.bool2string(checked);
  }
});
