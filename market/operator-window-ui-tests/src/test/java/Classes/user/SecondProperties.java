package ui_tests.src.test.java.Classes.user;

public class SecondProperties {
    private String status;
    private String ticketNumberInWork;

    public String getTicketNumberInWork() {
        return ticketNumberInWork;
    }

    public SecondProperties setTicketNumberInWork(String ticketNumberInWork) {
        this.ticketNumberInWork = ticketNumberInWork;
        return this;
    }

    public String getStatus() {
        return status;
    }


    public SecondProperties setStatus(String status) {
        this.status = status;
        return this;
    }


    @Override
    public String toString() {
        return "SecondProperties{" +
                "status='" + status + '\'' +
                ", ticketNumberInWork='" + ticketNumberInWork + '\'' +
                '}';
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SecondProperties that = (SecondProperties) o;
        if (status != null) {
            if (!status.equals(that.status)) {
                return false;
            }
        }
        if (ticketNumberInWork != null) {
            if (!ticketNumberInWork.equals(that.ticketNumberInWork)) {
                return false;
            }
        }
        return true;
    }
}
