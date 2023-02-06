import ITransfer from '../../../../interfaces/transfer/ITransfer';
import ITransferSegment from '../../../../interfaces/transfer/ITransferSegment';
import ISegment from '../../../../interfaces/segment/ISegment';
import ITransferFromApi from '../../../../interfaces/transfer/ITransferFromApi';
import ITransferSegmentFromApi from '../../../../interfaces/transfer/ITransferSegmentFromApi';
import ISegmentFromApi from '../../../../interfaces/segment/ISegmentFromApi';
import ISubSegmentFromApi from '../../../../interfaces/segment/ISubSegmentFromApi';
import ISubSegment from '../../../../interfaces/segment/ISubSegment';

export const transferFromApi = {
    isTransfer: true,
} as ITransferFromApi;

export const transfer = {
    ...transferFromApi,
} as ITransfer;

export const transferSegmentFromApi = {
    isTransferSegment: true,
} as ITransferSegmentFromApi;

export const transferSegment = {
    ...transferSegmentFromApi,
} as ITransferSegment;

export const segmentFromApi = {} as ISegmentFromApi;

export const segment = {
    ...segmentFromApi,
} as ISegment;

export const subSegmentFromApi = {
    isSubSegment: true,
} as ISubSegmentFromApi;

export const subSegment = {
    ...subSegmentFromApi,
} as ISubSegment;
