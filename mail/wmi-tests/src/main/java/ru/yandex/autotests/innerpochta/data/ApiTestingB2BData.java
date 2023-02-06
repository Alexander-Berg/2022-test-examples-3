package ru.yandex.autotests.innerpochta.data;

import ru.yandex.autotests.innerpochta.wmi.core.filter.VDirectCut;
import ru.yandex.autotests.innerpochta.wmi.core.obj.EmptyObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessageObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.*;
import ru.yandex.autotests.innerpochta.wmi.core.oper.onlyapi.Barlist;

import java.util.ArrayList;
import java.util.List;

import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.*;

public class ApiTestingB2BData {
    public static final String THREAD_ID = "158470411888100155";

    public static List<Object[]> apiHandles() {
        List<Object[]> operList = new ArrayList<Object[]>();
        //mailbox_list
        operList.add(new Object[]{
                api(MailBoxList.class),
                new EmptyObj(),
                MailBoxList.class.getSimpleName()
        });

        //mailbox_list
        operList.add(new Object[]{
                api(MailBoxList.class),
                EmptyObj.xmlVerDaria2(),
                MailBoxList.class.getSimpleName() + "_daria2"
        });

        //account_information
        operList.add(new Object[]{
                api(AccountInformation.class),
                new EmptyObj(),
                AccountInformation.class.getSimpleName()
        });
        //compose_check
        operList.add(new Object[]{
                api(ComposeCheck.class),
                new EmptyObj(),
                ComposeCheck.class.getSimpleName()
        });

        //barlist
        operList.add(new Object[]{
                api(Barlist.class),
                new EmptyObj(),
                Barlist.class.getSimpleName()
        });
        //folder_list
        operList.add(new Object[]{
                api(FolderList.class),
                new EmptyObj(),
                FolderList.class.getSimpleName()
        });

        //folder_list
        operList.add(new Object[]{
                api(FolderList.class),
                EmptyObj.xmlVerDaria2(),
                FolderList.class.getSimpleName() + "_daria2"
        });

        //labels
        operList.add(new Object[]{
                api(Labels.class),
                new EmptyObj(),
                Labels.class.getSimpleName()
        });

        //settings_setup
        operList.add(new Object[]{
                api(SettingsSetup.class),
                new EmptyObj(),
                SettingsSetup.class.getSimpleName()
        });

        //посторонний мид - должен выдавать ошибку NO_SUCH_MESSAGE
        operList.add(new Object[]{
                api(Message.class).filters(new VDirectCut()),
                MessageObj.getMsg("1210000000806698990"),
                Message.class.getSimpleName()
        });

        return operList;
    }
}
