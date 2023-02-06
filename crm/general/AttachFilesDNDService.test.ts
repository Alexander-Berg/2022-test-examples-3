import { zoneStub } from 'components/MultiOverlayDropZone';
import { AttachFilesDNDService } from './AttachFilesDNDService';

describe('AttachFilesDNDService', () => {
  it('adds zones', async () => {
    const attachFilesService = new AttachFilesDNDService();

    expect(attachFilesService.dropZones.length).toBe(0);

    attachFilesService.addZones([zoneStub]);

    expect(attachFilesService.dropZones.length).toBe(1);
  });

  it('remove zones', async () => {
    const attachFilesService = new AttachFilesDNDService();

    attachFilesService.addZones([zoneStub]);
    expect(attachFilesService.dropZones.length).toBe(1);

    attachFilesService.removeZones([zoneStub]);

    expect(attachFilesService.dropZones.length).toBe(0);
  });

  it('remove all zones', async () => {
    const attachFilesService = new AttachFilesDNDService();

    attachFilesService.addZones([zoneStub]);
    expect(attachFilesService.dropZones.length).toBe(1);

    attachFilesService.clearZones();

    expect(attachFilesService.dropZones.length).toBe(0);
  });
});
