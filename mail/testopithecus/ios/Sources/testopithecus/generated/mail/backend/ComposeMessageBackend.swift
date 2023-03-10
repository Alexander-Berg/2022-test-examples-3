// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/backend/compose-message-backend.ts >>>

import Foundation

open class ComposeMessageBackend: ComposeMessage {
  private var clientsHandler: MailboxClientHandler
  public init(_ clientsHandler: MailboxClientHandler) {
    self.clientsHandler = clientsHandler
  }

  open func goToMessageReply() {}

  open func setBody(_: String) {}

  open func setSubject(_: String) {}

  open func addTo(_: String) {}

  open func addToUsingSuggest(_: String) {}

  open func clearBody() {}

  open func clearSubject() {}

  @discardableResult
  open func getDraft() -> DraftView {
    return Draft(WysiwygModel())
  }

  @discardableResult
  open func getTo() -> YSSet<String> {
    return YSSet<String>()
  }

  open func removeTo(_: Int32) {}
}
