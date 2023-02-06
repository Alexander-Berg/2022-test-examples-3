package ui_tests.src.test.java.Classes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class Comment {
    private String text;
    private String nameAndEmail;
    private List<String> files = new ArrayList<>();
    private String type;

    public String getType() {
        return type;
    }

    public Comment setType(String type) {
        this.type = type;
        return this;
    }

    public String getText() {
        return text;
    }

    public Comment setText(String text) {
        this.text = text;
        return this;
    }

    public List<String> getFiles() {
        return files;
    }

    public Comment setFiles(List<String> files) {
        this.files = files;
        return this;
    }

    public String getNameAndEmail() {
        return nameAndEmail;
    }

    public Comment setNameAndEmail(String nameAndEmail) {
        this.nameAndEmail = nameAndEmail;
        return this;
    }

    @Override
    public boolean equals(Object actual) {
        if (this == actual) return true;
        if (actual == null || getClass() != actual.getClass()) return false;
        Comment actualComment = (Comment) actual;

        if (this.text != null) {
            if (!this.text.equals(actualComment.text)) {
                Pattern pattern = Pattern.compile(this.text);
                if (!pattern.matcher(actualComment.text).find()){
                    return false;
                }

            }
        }
        if (this.nameAndEmail != null) {
            if (!this.nameAndEmail.toLowerCase().equals(actualComment.nameAndEmail.toLowerCase())) {
                return false;
            }
        }
        if (this.type != null) {
            if (!this.type.equals(actualComment.type)) {
                return false;
            }
        }
        if (!this.files.containsAll(actualComment.files)){
            return false;
        }
        return true;
    }


    @Override
    public String toString() {
        return "Comment{" +
                "text='" + text + '\'' +
                ", nameAndEmail='" + nameAndEmail + '\'' +
                ", files=" + files +
                ", type='" + type + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, nameAndEmail, files, type);
    }
}
