import React from 'react';
import { render } from '@testing-library/react';
import { zoneStub } from 'components/MultiOverlayDropZone';
import { AttachFilesDND } from './AttachFilesDND';
import { AttachFilesDNDContext, AttachFilesDNDService } from './AttachFilesDNDService';

describe('AttachFilesDND', () => {
  describe('props.zones', () => {
    describe('when defined', () => {
      it('adds zones on mount', () => {
        const attachDNDService = new AttachFilesDNDService();

        render(
          <AttachFilesDNDContext.Provider value={attachDNDService}>
            <AttachFilesDND zones={[zoneStub]} />
          </AttachFilesDNDContext.Provider>,
        );

        expect(attachDNDService.dropZones.length).toBe(1);
      });

      describe('when changes', () => {
        it('rerenders zones', () => {
          const attachDNDService = new AttachFilesDNDService();

          const { rerender } = render(
            <AttachFilesDNDContext.Provider value={attachDNDService}>
              <AttachFilesDND zones={[zoneStub]} />
            </AttachFilesDNDContext.Provider>,
          );

          expect(attachDNDService.dropZones.length).toBe(1);

          rerender(
            <AttachFilesDNDContext.Provider value={attachDNDService}>
              <AttachFilesDND zones={[zoneStub, { ...zoneStub, text: 'zone text 2' }]} />
            </AttachFilesDNDContext.Provider>,
          );

          expect(attachDNDService.dropZones.length).toBe(2);
        });
      });

      it('removes zones on unmount', () => {
        const attachDNDService = new AttachFilesDNDService();

        const { rerender } = render(
          <AttachFilesDNDContext.Provider value={attachDNDService}>
            <AttachFilesDND zones={[zoneStub]} />
          </AttachFilesDNDContext.Provider>,
        );

        expect(attachDNDService.dropZones.length).toBe(1);

        rerender(<AttachFilesDNDContext.Provider value={attachDNDService} />);

        expect(attachDNDService.dropZones.length).toBe(0);
      });
    });
  });
});
