import { shallow } from 'enzyme';
import React from 'react';

import { createInterWarehouseExportedRecommendation } from 'src/test/data/interWarehouseExportedRecommendations';
import {
  MskuFormatter,
  SskuFormatter,
  PurchaseQuantityFormatter,
  AdjustedPurchaseQuantityFormatter,
  CorrectionReasonFormatter,
  TitleFormatter,
  ABCFormatter,
  DepartmentFormatter,
  LengthFormatter,
  WidthFormatter,
  HeightFormatter,
  VolumeFormatter,
  WeightFormatter,
  SupplierTypeFormatter,
} from './formatters';

const recommendation = createInterWarehouseExportedRecommendation();

describe('<InterWarehouseMovementsPage /> (formatters)', () => {
  it('should format MSKU', () => {
    expect(shallow(<MskuFormatter {...recommendation} />).html()).toEqual('<span title="550430">550430</span>');
  });

  it('should format SSKU', () => {
    expect(shallow(<SskuFormatter {...recommendation} />).html()).toEqual(
      '<span title="481645.550430">481645.550430</span>'
    );
  });

  it('should format purchase quantity', () => {
    expect(shallow(<PurchaseQuantityFormatter {...recommendation} />).html()).toEqual('<span title="1">1</span>');
  });

  it('should format adjusted purchase quantity', () => {
    expect(shallow(<AdjustedPurchaseQuantityFormatter {...recommendation} />).html()).toEqual(
      '<span title="1">1</span>'
    );
  });

  it('should format correction reason', () => {
    expect(shallow(<CorrectionReasonFormatter {...recommendation} />).html()).toEqual('<span>-</span>');
  });

  it('should format title', () => {
    expect(shallow(<TitleFormatter {...recommendation} />).html()).toEqual(
      '<span title="220718 Конвектор CNS 100 S Stiebel Eltron">220718 Конвектор CNS 100 S Stiebel Eltron</span>'
    );
  });

  it('should format ABC', () => {
    expect(shallow(<ABCFormatter {...recommendation} />).html()).toEqual('<span title="C">C</span>');
  });

  it('should format department', () => {
    expect(shallow(<DepartmentFormatter {...recommendation} />).html()).toEqual(
      '<span title="Дом и сад">Дом и сад</span>'
    );
  });

  it('should format length', () => {
    expect(shallow(<LengthFormatter {...recommendation} />).html()).toEqual('<span title="47">47</span>');
  });

  it('should format width', () => {
    expect(shallow(<WidthFormatter {...recommendation} />).html()).toEqual('<span title="14">14</span>');
  });

  it('should format height', () => {
    expect(shallow(<HeightFormatter {...recommendation} />).html()).toEqual('<span title="49">49</span>');
  });

  it('should format volume', () => {
    expect(shallow(<VolumeFormatter {...recommendation} />).html()).toEqual('<span title="32242">32242</span>');
  });

  it('should format weight', () => {
    expect(shallow(<WeightFormatter {...recommendation} />).html()).toEqual('<span title="5.10">5.10</span>');
  });

  it('should format supplier type', () => {
    expect(shallow(<SupplierTypeFormatter {...recommendation} />).html()).toEqual('<span title="1">1</span>');
  });
});
