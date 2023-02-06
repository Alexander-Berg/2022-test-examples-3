package ru.yandex.market.logistics.les.objectmapper

import com.amazon.sqs.javamessaging.message.SQSTextMessage
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import javax.jms.Queue
import javax.jms.Session
import javax.jms.Topic

fun sessionMock(): Session {
    val ses: Session = Mockito.mock(Session::class.java)
    whenever(ses.createTopic(Mockito.anyString())).thenReturn(Mockito.mock(Topic::class.java))
    whenever(ses.createQueue(Mockito.anyString())).thenReturn(Mockito.mock(Queue::class.java))
    whenever(ses.createTextMessage(Mockito.anyString())).thenAnswer {
        SQSTextMessage(it.arguments[0] as String)
    }
    return ses
}
