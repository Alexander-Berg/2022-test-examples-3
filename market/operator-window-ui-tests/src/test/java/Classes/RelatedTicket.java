package ui_tests.src.test.java.Classes;

public class RelatedTicket {
    private String relationType;
    private String link;
    private String relatedObject;
    private String service;
    private String status;

    /**
     * Получить тип связи
     */
    public String getRelationType(){ return relationType; }

    /**
     * Установить тип связи
     */
    public RelatedTicket setRelationType(String relationType) {
        this.relationType = relationType;
        return this;
    }

    /**
     * Получить текст ссылки
     */
    public String getLink(){ return link; }

    /**
     * Установить текст ссылки
     */
    public RelatedTicket setLink(String link) {
        this.link = link;
        return this;
    }

    /**
     * Получить связанный объект
     */
    public String getRelatedObject(){ return relatedObject; }

    /**
     * Установить связанный объект
     */
    public RelatedTicket setRelatedObject(String relatedObject) {
        this.relatedObject = relatedObject;
        return this;
    }

    /**
     * Получить очередь
     */
    public String getService(){ return service; }

    /**
     * Установить очередь
     */
    public RelatedTicket setService(String service) {
        this.service = service;
        return this;
    }

    /**
     * Получить статус
     */
    public String getStatus(){ return status; }

    /**
     * Установить статус
     */
    public RelatedTicket setStatus(String status) {
        this.status = status;
        return this;
    }


    @Override
    public String toString() {
        return "RelatedTicket{" +
                "relationType='" + relationType + '\'' +
                ", link='" + link + '\'' +
                ", relatedObject='" + relatedObject + '\'' +
                ", service='" + service + '\'' +
                ", status='" + status +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RelatedTicket relatedTicket = (RelatedTicket) o;
        if (this.relationType != null) {
            if (!this.relationType.equals(relatedTicket.relationType)) {
                return false;
            }
        }
        if (this.link != null) {
            if (!this.link.equals(relatedTicket.link)) {
                return false;
            }
        }
        if (this.relatedObject != null) {
            if (!this.relatedObject.equals(relatedTicket.relatedObject)) {
                return false;
            }
        }
        if (this.service!=null){
            if (!this.service.equals(relatedTicket.service)){
                return false;
            }
        }
        if (this.status!=null){
            if(!this.status.equals(relatedTicket.status)){
                return false;
            }
        }
        return true;
    }

}
