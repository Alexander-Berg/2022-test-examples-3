import React from 'react';
import { FavoritesIcon } from '../../desktop';

const styles = `
.Wrapper {
  padding: 20px;
}
`;

export const Simple = () => (
  <div className="Wrapper">
    <style>{styles}</style>
    <FavoritesIcon />
  </div>
);
