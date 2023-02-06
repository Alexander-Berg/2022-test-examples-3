import {
  GuruType,
  Parameter as ProtoParameter,
  SKUParameterMode,
  ValueType,
} from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';

import { RUSSIAN_LANG_ID } from 'src/shared/constants';

let nextId = 1;

export function testParameterProto(setup: Partial<ProtoParameter> = {}): ProtoParameter {
  const {
    id = nextId++,
    multivalue = false,
    value_type = ValueType.STRING,
    guru_type = GuruType.GURU_TYPE_STRING,
    sku_mode = SKUParameterMode.SKU_NONE,
    ...rest
  } = setup;

  return {
    id,
    name: [{ name: `test param ${id}`, lang_id: RUSSIAN_LANG_ID }],
    xsl_name: `test_${id}`,
    value_type,
    guru_type,
    sku_mode,
    mandatory: true,
    required_for_model_creation: true,
    multivalue,
    ...rest,
  };
}
