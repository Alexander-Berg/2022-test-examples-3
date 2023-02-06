package ui_tests.src.test.java.Classes.order;

import java.util.Objects;

public class History {
    private String author;
    private String date;
    private String typeEntity;
    private String statusAfter;

    public String getAuthor() {
        return author;
    }

    public History setAuthor(String author) {
        this.author = author;
        return this;
    }

    public String getDate() {
        return date;
    }

    public History setDate(String date) {
        this.date = date;
        return this;
    }

    public String getTypeEntity() {
        return typeEntity;
    }

    public History setTypeEntity(String typeEntity) {
        this.typeEntity = typeEntity;
        return this;
    }

    public String getStatusAfter() {
        return statusAfter;
    }

    public History setStatusAfter(String statusAfter) {
        this.statusAfter = statusAfter;
        return this;
    }

    @Override
    public String toString() {
        return "History{" +
                "author='" + author + '\'' +
                ", date='" + date + '\'' +
                ", typeEntity='" + typeEntity + '\'' +
                ", statusAfter='" + statusAfter + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        History history = (History) o;
        if (this.author!=null){
            if (!this.author.equals(history.getAuthor())){
                return false;
            }
        }

        if (this.date!=null){
            if (!this.date.equals(history.getDate())){
                return false;
            }
        }

        if (this.statusAfter!=null){
            if (!this.statusAfter.equals(history.getStatusAfter())){
                return false;
            }
        }

        if (this.typeEntity!=null){
            if (!this.typeEntity.equals(history.getTypeEntity())){
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(author, date, typeEntity, statusAfter);
    }
}
