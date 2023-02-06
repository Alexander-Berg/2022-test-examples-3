import jestFetchMock from 'jest-fetch-mock';
import fetchMock from 'fetch-mock-jest';

jestFetchMock.enableMocks();
afterEach(fetchMock.reset)

window.HTMLElement.prototype.scrollIntoView = function() {};
