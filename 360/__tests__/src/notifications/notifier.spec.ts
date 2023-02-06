import { EventEmitter } from 'events'
import { Notifier, NotifyOptions } from '../../../src/notifications/notifier'

class MockNotification extends EventEmitter {

    public body: string
    public isVisible = false
    constructor({ body }: NotifyOptions) {
        super()
        this.body = body
    }
    public static isSupported() {
        return true
    }

    public show() {
        this.isVisible = true
    }

    public close() {
        this.isVisible = false
        this.emit('close')
    }
}

const getTracking = (e: any) => e.tracking
const mockConfig = () => ({ title: '__title__', body: '__body__' })

describe(Notifier, () => {
    beforeEach(() => {
        (Notifier as any).Constructor = MockNotification
    })

    describe('tracks notification references', () => {
        it('stores notification references', () => {
            const notifier = new Notifier(1)
            notifier.notify(mockConfig())
            expect(getTracking(notifier).length).toBe(1)
        })

        it('removes notification references on close', () => {
            const notifier = new Notifier(1)
            notifier.notify(mockConfig())!.close()
            expect(getTracking(notifier).length).toBe(0)
        })

        it('closes oldest notifications on MAX_COUNT overflow', () => {
            const cnt = 2
            const notifier = new Notifier(cnt)

            const first = notifier.notify(mockConfig()) as MockNotification
            notifier.notify(mockConfig())
            const last = notifier.notify(mockConfig()) as MockNotification

            expect(first.isVisible).toBe(false)
            expect(last.isVisible).toBe(true)
            expect(getTracking(notifier).length).toBe(cnt)
        })
    })

    describe('polyfills subtitle', () => {
        const subtitle = '__subtitle__'
        const subtitleOps = { ...mockConfig(), subtitle }
        const { body } = subtitleOps

        it('polyfills subtitle for non-macs', () => {
            const notifier = new Notifier(1);
            (notifier as any).isMac = false
            const notify = notifier.notify(subtitleOps) as MockNotification
            expect(notify.body).toMatch(new RegExp(`${subtitle}[\\s\\S]+${body}`))
        })

        it('leaves body as-is on macs', () => {
            const notifier = new Notifier(1);
            (notifier as any).isMac = true
            const notify = notifier.notify(subtitleOps) as MockNotification
            expect(notify.body).toBe(body)
        })
    })
})
