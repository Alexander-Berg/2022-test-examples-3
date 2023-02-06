import { WebphoneWidget } from '@yandex-telephony/ya-calls-webphone-sdk';
import { EventTarget } from 'event-target-shim';
import favicon from 'utils/favicon';
import { config } from 'services/Config';
import { waitFor } from '@testing-library/dom';
import { WebphoneOutgoingEventKind } from './WebphoneEventManager';
import { WebphoneFaviconService } from './WebphoneFaviconService';

const createAbsoluteFaviconUrl = (url: string) => {
  return `${location.href}${url}`;
};

describe('WebphoneFaviconService', () => {
  let webphone: EventTarget;
  let linkFavicon: HTMLLinkElement;

  beforeEach(() => {
    webphone = new EventTarget();

    let linkLikeFavicon = document.createElement('link');
    linkLikeFavicon.rel = 'apple-touch-icon';
    linkLikeFavicon.href = favicon.TYPES.DEFAULT;

    linkFavicon = document.createElement('link');
    linkFavicon.rel = 'icon';
    linkFavicon.href = favicon.TYPES.DEFAULT;

    document.head.append(linkLikeFavicon);
    document.head.append(linkFavicon);
  });

  afterEach(() => {
    linkFavicon.remove();
  });

  it('should correct change favicon', async () => {
    config.value.features.useYaCallsLB = true;

    const _webphoneFaviconService = new WebphoneFaviconService(webphone as WebphoneWidget);

    webphone.dispatchEvent(new CustomEvent(WebphoneOutgoingEventKind.EstablishedCall));

    await waitFor(() =>
      expect(linkFavicon.href).toBe(createAbsoluteFaviconUrl(favicon.TYPES.CALL_ACTIVE)),
    );

    webphone.dispatchEvent(new CustomEvent(WebphoneOutgoingEventKind.CallEnd));

    await waitFor(() =>
      expect(linkFavicon.href).toBe(createAbsoluteFaviconUrl(favicon.TYPES.DEFAULT)),
    );
  });

  it('should correct change favicon with useYaCallsLB equals false', async () => {
    config.value.features.useYaCallsLB = false;

    const _webphoneFaviconService = new WebphoneFaviconService(webphone as WebphoneWidget);

    webphone.dispatchEvent(new CustomEvent(WebphoneOutgoingEventKind.NewIncomingCall));

    await waitFor(() =>
      expect(linkFavicon.href).toBe(createAbsoluteFaviconUrl(favicon.TYPES.CALL_ACTIVE)),
    );

    webphone.dispatchEvent(new CustomEvent(WebphoneOutgoingEventKind.CallEnd));

    await waitFor(() =>
      expect(linkFavicon.href).toBe(createAbsoluteFaviconUrl(favicon.TYPES.DEFAULT)),
    );
  });

  it('should not change favicon after destroy', async () => {
    const webphoneFaviconService = new WebphoneFaviconService(webphone as WebphoneWidget);
    webphoneFaviconService.destroy();

    webphone.dispatchEvent(new CustomEvent(WebphoneOutgoingEventKind.NewIncomingCall));

    await waitFor(() =>
      expect(linkFavicon.href).toBe(createAbsoluteFaviconUrl(favicon.TYPES.DEFAULT)),
    );
  });
});
