const API_HOST = process.env.REACT_APP_API_HOST || 'wss://checkout-test-service.tap-tst.yandex.ru';
export const WS_SERVER_HOST = window.location.hostname === 'localhost' ? 'ws://localhost:8080/ws' : `${API_HOST}/ws`;
