package ru.yandex.personal.mailimport.service;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

import com.sun.mail.imap.IMAPFolder;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import ru.yandex.personal.mailimport.model.MailFolder;
import ru.yandex.personal.mailimport.model.MailMessage;
import ru.yandex.personal.mailimport.model.MailPerson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImapMailboxTest {


    @Test
    void listFolders() throws MessagingException {
        ImapConnection connection = mock(ImapConnection.class, RETURNS_DEEP_STUBS);

        when(connection.getStore().getDefaultFolder()).thenAnswer(
                (Answer<Folder>) invocation -> mockFolder("root",
                        mockFolder("a"),
                        mockFolder("b"),
                        mockFolder("c")
                ));

        ImapMailbox mailbox = new ImapMailbox(connection);
        List<String> actual = mailbox.listFolders();

        Set<String> expected = new HashSet<>(Arrays.asList("root", "a", "b", "c"));

        assertEquals(expected, new HashSet<>(actual));
    }

    @Test
    void clear() throws MessagingException {
        ImapConnection connection = mock(ImapConnection.class, RETURNS_DEEP_STUBS);

        Message msg1 = mockMessage("msg 1");
        Message msg2 = mockMessage("msg 2");

        Set<Message> msgs = new HashSet<>(Arrays.asList(msg1, msg2));

        doAnswer((Answer<Void>) invocation -> {
            msgs.remove(invocation.getMock());
            return null;
        }).when(msg1).setFlag(eq(Flags.Flag.DELETED), eq(true));
        doAnswer((Answer<Void>) invocation -> {
            msgs.remove(invocation.getMock());
            return null;
        }).when(msg2).setFlag(eq(Flags.Flag.DELETED), eq(true));

        Folder a = mockFolder("a");
        when(a.getMessages()).thenAnswer((Answer<Message[]>) invocation -> msgs.toArray(new Message[0]));
        when(a.delete(eq(true))).thenAnswer((Answer<Boolean>) invocation -> msgs.isEmpty());
        Folder b = mockFolder("b");
        when(b.getMessages()).thenReturn(new Message[0]);
        when(a.delete(eq(true))).thenReturn(true);

        when(connection.getStore().getDefaultFolder()).thenAnswer(
                (Answer<Folder>) invocation -> mockFolder("root", a, b));

        ImapMailbox mailbox = new ImapMailbox(connection);
        mailbox.clear();

        verify(a).delete(eq(true));
        verify(b).delete(eq(true));
    }

    @Test
    void addFolder() throws MessagingException {
        ImapConnection connection = mock(ImapConnection.class, RETURNS_DEEP_STUBS);
        IMAPFolder created = mock(IMAPFolder.class);
        when(created.exists()).thenReturn(false);

        Folder root = mockFolder("root");
        when(root.getFolder("a|b")).thenReturn(created);

        when(connection.getStore().getDefaultFolder()).thenReturn(root);

        ImapMailbox mailbox = new ImapMailbox(connection);

        List<MailMessage> messages = List.of(new MailMessage("", new MailPerson("", ""),
                List.of(), List.of(), List.of(),
                Instant.now(), null, ""));

        MailFolder mf = new MailFolder(messages, "a|b");
        mailbox.addFolder(mf);

        verify(created).create(eq(Folder.HOLDS_MESSAGES));
        verify(created).addMessages(any());
    }

    private Folder mockFolder(String name, Folder... subfolders) throws MessagingException {
        Folder folder = mock(Folder.class);
        when(folder.list()).thenReturn(subfolders);
        when(folder.getFullName()).thenReturn(name);
        return folder;
    }

    private Message mockMessage(String subject) throws MessagingException {
        Message msg = mock(Message.class);
        when(msg.getSubject()).thenReturn(subject);

        return msg;
    }
}
