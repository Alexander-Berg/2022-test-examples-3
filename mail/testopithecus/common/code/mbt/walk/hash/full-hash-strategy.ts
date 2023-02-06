import { Int32, int64, Int64 } from '../../../../ys/ys';
import { Folder, FullMessage, MailboxModel } from '../../../mail/model/mail-model';
import { HashBuilder, HashStrategy } from './hash-builder';

export class FullHashStrategy implements HashStrategy {
  public getMailboxModelHash(model: MailboxModel): Int64 {
    const hashBuilder: HashBuilder = new HashBuilder()
      .addBoolean(model.rotatable.landscape)
      .addInt64(model.messageNavigator.openedMessage);

    const selectedMessages = model.groupMode.selectedOrders;
    if (selectedMessages !== null) {
      hashBuilder.addInt64(FullHashStrategy.getSelectedMessagesHash(selectedMessages));
    } else {
      hashBuilder.addBoolean(true);
    }

    hashBuilder.addInt(29);
    for (const message of model.readOnlyExpandableThreads.expanded.values()) {
      hashBuilder.addInt64(message);
    }

    hashBuilder.addInt(23);
    if (!model.mailAppModelHandler.hasCurrentAccount()) {
      return hashBuilder.build();
    }

    const account = model.messageListDisplay.accountDataHandler.getCurrentAccount();

    for (const thread of account.threads) {
      hashBuilder.addInt(19);
      for (const message of thread.values()) {
        hashBuilder.addInt64(message);
      }
    }

    hashBuilder.addInt(17);
    account.messages.forEach((message, id) => {
      hashBuilder.addInt64(id).addInt64(FullHashStrategy.getMessageHash(message))
    });

    hashBuilder.addInt(13);
    account.folderToMessages.forEach((ids, folder) => {
      for (const id of ids.values()) {
        hashBuilder.addInt64(id).addString(folder)
      }
    });

    hashBuilder.addString(model.folderNavigator.getCurrentFolder().name)

    return hashBuilder.build();
  }

  private static getFolderHash(folder: Folder): Int64 {
    return new HashBuilder().addString(folder.name).build();
  }

  private static getMessageHash(message: FullMessage): Int64 {
    const hashBuilder: HashBuilder = new HashBuilder()
      .addString(message.head.from)
      .addBoolean(message.head.read)
      .addString(message.head.subject)
      .addBoolean(message.head.important)
      .addInt64(message.mutableHead.timestamp);
    if (message.head.threadCounter !== null) {
      hashBuilder.addInt64(int64(message.head.threadCounter!));
    } else {
      hashBuilder.addBoolean(true);
    }
    return hashBuilder.build();
  }

  private static getSelectedMessagesHash(selectedMessages: Set<Int32>): Int64 {
    const hashBuilder: HashBuilder = new HashBuilder();
    hashBuilder.addInt(11);
    for (const v of selectedMessages.values()) {
      hashBuilder.addInt64(int64(v));
    }
    return hashBuilder.build();
  }

}
