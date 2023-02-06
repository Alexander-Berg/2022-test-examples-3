import {BehaviorSubject, Observable, Subject} from 'rxjs'
import {SocketEvent} from 'core-notifications'
import {SocketEvents} from 'core-legacy/types/socket'
import {OrderStatus} from '../../core-legacy/models/Orders/Order'

export default class SocketEventsMock extends Observable<SocketEvent> {
  status$ = new BehaviorSubject(false)
  private event$ = new Subject<SocketEvent>()

  constructor(event$?: Observable<SocketEvent>) {
    super((observer) => {
      const sub = this.event$.subscribe(observer)

      if (event$) {
        sub.add(event$.subscribe(this.event$))
      }

      return sub
    })
  }

  send<K extends keyof SocketEvents>(event: SocketEvent<K>) {
    this.event$.next(event)
  }

  sendOrderChangedItems() {
    this.send({
      event: 'order_changed_items',
      orderId: 'abcdef-123456',
      orderNr: '123456-987654'
    })
  }

  sendOrderChangedStatus(orderStatus: OrderStatus = 'accepted') {
    this.send({
      event: 'order_changed_status',
      orderId: 'abcdef-123456',
      orderNr: '123456-987654',
      orderStatus
    })
  }

  sendOrderNew() {
    this.send({
      event: 'order_new',
      orderId: 'abcdef-123456',
      orderNr: '123456-987654'
    })
  }
}
