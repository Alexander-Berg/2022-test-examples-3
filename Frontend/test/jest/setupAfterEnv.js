/* global expect */

import '@testing-library/jest-dom';
import customMatchers from './matchers';

expect.extend(customMatchers);
