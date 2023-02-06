import { render, waitFor, screen, cleanup, fireEvent } from '@testing-library/react/pure';
import { setupServer } from 'msw/node';
import intersectionObserverMock from 'services/IntersectionWatcher/__mock__/intersectionObserverMock';
import { handlers } from './mocks/handlers';
import { Default } from './PersonalDashboard.stories';

window.IntersectionObserver = intersectionObserverMock;
jest.mock('./TabLogger', () => ({
  TabLogger: () => null,
}));
const server = setupServer(...handlers);

describe('Manager Page', () => {
  beforeAll(() => {
    server.listen();
    server.resetHandlers();

    render(Default());

    return waitFor(() => screen.findAllByText('Tab 1'));
  });

  afterAll(() => {
    server.close();
    cleanup();
  });

  it('displays tabs', () => {
    expect(screen.getByText('Tab 1')).toBeInTheDocument();
    expect(screen.getByText('Tab 2')).toBeInTheDocument();
  });

  it('displays blocks', () => {
    expect(screen.getByText('Block 1')).toBeInTheDocument();
    expect(screen.getByText('Block 2')).toBeInTheDocument();
  });

  it("doesn't display next page blocks", () => {
    expect(screen.queryByText('Block 3')).toBeNull();
    expect(screen.queryByText('Block 4')).toBeNull();
  });

  it('displays blocks on next tab', () => {
    fireEvent.click(screen.getByText('Tab 2'));

    expect(screen.getByText('Block 3')).toBeInTheDocument();
    expect(screen.getByText('Block 4')).toBeInTheDocument();
  });
});
