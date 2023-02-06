package ru.yandex.autotests.innerpochta.data;

import ru.yandex.autotests.innerpochta.wmi.core.filter.VDirectCut;
import ru.yandex.autotests.innerpochta.wmi.core.obj.EmptyObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.ThreadListObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.*;

import java.util.ArrayList;
import java.util.List;

import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 20.04.15
 * Time: 18:09
 */
public class JsxTestingB2BData {
    public static List<Object[]> jsxHandles() {
        List<Object[]> operList = new ArrayList<Object[]>();
        //mailbox_list
        operList.add(new Object[]{
                jsx(MailBoxList.class),
                new EmptyObj(),
                MailBoxList.class.getSimpleName()
        });

        //mailbox_list
        operList.add(new Object[]{
                jsx(MailBoxList.class),
                EmptyObj.xmlVerDaria2(),
                MailBoxList.class.getSimpleName() + "_daria2"
        });

        //mailbox_list_ids
        operList.add(new Object[]{
                jsx(MailboxListIds.class),
                new EmptyObj(),
                MailBoxList.class.getSimpleName()
        });

        //account_information
        operList.add(new Object[]{
                jsx(AccountInformation.class),
                new EmptyObj(),
                AccountInformation.class.getSimpleName()
        });
        //compose_check
        operList.add(new Object[]{
                jsx(ComposeCheck.class),
                new EmptyObj(),
                ComposeCheck.class.getSimpleName()
        });

        //mailbox_list_unread (он же barlist)
        operList.add(new Object[]{
                jsx(MailboxListUnread.class),
                new EmptyObj(),
                MailboxListUnread.class.getSimpleName()
        });
        //folder_list
        operList.add(new Object[]{
                jsx(FolderList.class),
                new EmptyObj(),
                FolderList.class.getSimpleName()
        });

        //folder_list
        operList.add(new Object[]{
                jsx(FolderList.class),
                EmptyObj.xmlVerDaria2(),
                FolderList.class.getSimpleName() + "_daria2"
        });
        //labels
        operList.add(new Object[]{
                jsx(Labels.class),
                new EmptyObj(),
                Labels.class.getSimpleName()
        });
        //settings_setup
        operList.add(new Object[]{
                jsx(SettingsSetup.class),
                new EmptyObj(),
                SettingsSetup.class.getSimpleName()
        });
        //thread_list
        operList.add(new Object[]{
                jsx(ThreadList.class),
                ThreadListObj.getThread("2360000001252374963"),
                ThreadList.class.getSimpleName()
        });
        //threads_view
        operList.add(new Object[]{
                jsx(ThreadsView.class),
                new EmptyObj().set("sort_type", "date1"),
                ThreadsView.class.getSimpleName()
        });
        //проверяет ошибку метода предзагрузки аттача
        operList.add(new Object[]{
                jsx(UploadAttachmentXml.class).filters(new VDirectCut()),
                new EmptyObj(),
                UploadAttachmentXml.class.getSimpleName()
        });

        //DARIA-52775  current фид обязательный стал
//        operList.add(new Object[]{
//                jsx(GetFirstEnvelopeDate.class),
//                new EmptyObj(),
//                GetFirstEnvelopeDate.class.getSimpleName()
//        });
        return operList;
    }



}
