package ru.yandex.direct.core.testing.info;

public class SupportForClientInfo {

    private ClientInfo subjectClientInfo;
    private UserInfo operatorInfo;
    private Long relationId;

    public ClientInfo getSubjectClientInfo() {
        return subjectClientInfo;
    }

    public SupportForClientInfo withSubjectClientInfo(ClientInfo subjectClientInfo) {
        this.subjectClientInfo = subjectClientInfo;
        return this;
    }

    public UserInfo getOperatorInfo() {
        return operatorInfo;
    }

    public SupportForClientInfo withOperatorInfo(UserInfo managerInfo) {
        this.operatorInfo = managerInfo;
        return this;
    }

    public Long getRelationId() {
        return relationId;
    }

    public SupportForClientInfo withRelationId(Long relationId) {
        this.relationId = relationId;
        return this;
    }

    public Long getSubjectUid() {
        return subjectClientInfo.getUid();
    }

}
