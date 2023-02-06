import React from 'react';
import { Signup } from '../..';

const styles = `
.Wrapper {
  padding: 20px;
}
`;

export const Simple = () => (
  <div className="Wrapper">
    <style>{styles}</style>
    <Signup />
  </div>
);
