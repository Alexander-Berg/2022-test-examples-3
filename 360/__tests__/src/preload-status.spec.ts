import { PreloadStatus, PreloadStatusService } from '../../src/preload-status'
import { DesktopSyncModelNotifier } from '../../src/xplat/common/code/busilogics/sync/desktop-sync-model-notifier'

describe(PreloadStatusService, () => {
    it('.set() changes status', () => {
        const s = new PreloadStatusService().set('loadingCritical')
        expect(s.status).toBe('loadingCritical')
    })

    it('status change emits events', () => {
        const s = new PreloadStatusService()
        const changeCb = jest.fn(() => undefined)
        s.on('loadingCritical', changeCb)
        s.set('loadingCritical')
        expect(changeCb).toBeCalledTimes(1)
    })

    describe('whenCriticalDone', () => {
        it('resolves on "done" & "loadingMessages"', () => {
            return Promise.all((['loadingMessages', 'done'] as PreloadStatus[]).map((status) => {
                const s = new PreloadStatusService()
                const wait = s.whenCriticalDone()
                s.set(status)
                return wait
            }))
        })
        it('rejects on "failed"', () => {
            const s = new PreloadStatusService()
            const wait = s.whenCriticalDone()
            s.set('failed')
            return expect(wait).rejects.toBe(undefined)
        })
        it('resolves immediately if already "done"', () => {
            const s = new PreloadStatusService().set('done')
            return s.whenCriticalDone()
        })
        it('rejects immediately if already "failed"', () => {
            const s = new PreloadStatusService().set('failed')
            return expect(s.whenCriticalDone()).rejects.toBe(undefined)
        })
    })

    describe('getDescriptor', () => {
        const statusDescriptor = (s: PreloadStatus) =>
            new PreloadStatusService().set(s).getDescriptor()
        it('tracks done', () => {
            expect(statusDescriptor('done'))
                .toMatchObject({ isDone: true, isFailed: false })
        })
        it('tracks failed', () => {
            expect(statusDescriptor('failed'))
                .toMatchObject({ isDone: false, isFailed: true })
        })
        it('tracks criticalDone', () => {
            expect(new PreloadStatusService().getDescriptor().isCriticalDone).toBe(false)
            expect(statusDescriptor('done').isCriticalDone).toBe(true)
            expect(statusDescriptor('loadingMessages').isCriticalDone).toBe(true)
        })
    })

    describe('folder tracking', () => {
        const empty = { fid: '1', total: 10, loaded: 0 }
        const oneLoaded = { ...empty, loaded: 1 }
        it('progresses folder', () => {
            const s = new PreloadStatusService()
            s.asNotifier().onFolderStarted(BigInt(1), 10)
            expect(s.getDescriptor().byFolder).toStrictEqual([empty])
            s.asNotifier().onMessageLoaded(BigInt(1), BigInt(1))
            expect(s.getDescriptor().byFolder).toStrictEqual([oneLoaded])
        })
        it('emits event on folder progress', () => {
            const s = new PreloadStatusService()
            const expectSingleEvent = (action: (n: DesktopSyncModelNotifier) => any, arg: any) => {
                const cb = jest.fn()
                s.on('folderProgress', cb)
                action(s.asNotifier())
                expect(cb).toBeCalledTimes(1)
                expect(cb).toBeCalledWith({
                    isCriticalDone: false,
                    isDone: false,
                    isFailed: false,
                    byFolder: [arg],
                })
            }

            expectSingleEvent((n) => n.onFolderStarted(BigInt(1), 10), empty)
            expectSingleEvent((n) => n.onMessageLoaded(BigInt(1), BigInt(1)), oneLoaded)
        })
        it('ignores duplicate mids', () => {
            const s = new PreloadStatusService()
            s.asNotifier().onFolderStarted(BigInt(1), 10)
            s.asNotifier().onMessageLoaded(BigInt(1), BigInt(1))
            s.asNotifier().onMessageLoaded(BigInt(1), BigInt(1))
            expect(s.getDescriptor().byFolder).toStrictEqual([oneLoaded])
        })
    })
})
