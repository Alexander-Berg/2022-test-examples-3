import React from 'react';
import { FavoritesIcon } from '../../desktop';

const styles = `
.Wrapper {
  padding: 20px;
  background-color: #1c1d28;
}
`;

export const White = () => (
  <div className="Wrapper">
    <style>{styles}</style>
    <FavoritesIcon white />
  </div>
);
