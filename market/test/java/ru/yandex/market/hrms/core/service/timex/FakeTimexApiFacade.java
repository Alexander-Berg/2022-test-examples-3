package ru.yandex.market.hrms.core.service.timex;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.AccessLevel;
import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.Company;
import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.Employee;
import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.EnterAreaEvent;
import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.Post;
import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.WorkingArea;

import ru.yandex.market.hrms.core.service.timex.dto.TimexResponseDto;
import ru.yandex.market.hrms.core.service.timex.dto.TimexUserDto;

@Getter
@RequiredArgsConstructor
public class FakeTimexApiFacade extends TimexApiFacade {

    private int logonUserCalled = 0;
    private int logoutUserCalled = 0;
    private int getPackEnterAreaEventsCalled = 0;
    private int removeEmployeeCalled = 0;

    private final EnterAreaEvent[] fakeElements;
    private boolean elementsSent = false;

    private WorkingArea[] workingAreas;

    private Company[] companies;
    private Post[] posts;
    private AccessLevel[] accessLevels;
    private Employee employee;
    private TimexResponseDto timexResponseDto;

    public void withWorkingArea(WorkingArea[] workingAreas) {
        this.workingAreas = workingAreas;
    }

    public void withCompanies(Company[] companies) {
        this.companies = companies;
    }

    public void withPosts(Post[] posts) {
        this.posts = posts;
    }

    public void withAccessLevels(AccessLevel[] accessLevels) {
        this.accessLevels = accessLevels;
    }

    public void withEmployee(Employee employee) {
        this.employee = employee;
    }

    public void withTimexResponseDto(TimexResponseDto responseDto) {
        this.timexResponseDto = responseDto;
    }

    public void clearCounters() {
        logonUserCalled = 0;
        logoutUserCalled = 0;
        getPackEnterAreaEventsCalled = 0;
        removeEmployeeCalled = 0;
    }

    @Override
    public String logonUser(String endpoint, String timexUser, String timexPassword) {
        logonUserCalled++;
        return "";
    }

    @Override
    public void logoutUser(String session) {
        logoutUserCalled++;
    }

    @Override
    public EnterAreaEvent[] getPackEnterAreaEvents(String session, GregorianCalendar from, GregorianCalendar to,
                                                   int returnObjects, int skipObjects) {
        getPackEnterAreaEventsCalled++;

        if (!elementsSent) {
            elementsSent = true;
            return fakeElements;
        } else {
            return null;
        }
    }

    @Override
    public List<Company> getAllCompanies(String session) {
        return new ArrayList<>(List.of(companies));
    }

    @Override
    public List<Post> getAllPosts(String session) {
        return new ArrayList<>(List.of(posts));
    }

    @Override
    public List<AccessLevel> getAllAccessLevels(String session) {
        return new ArrayList<>(List.of(accessLevels));
    }

    @Override
    public Optional<Employee> getEmployee(String session, String oid) {
        return Optional.of(employee);
    }

    @Override
    public Optional<Company> createCompany(String session, String companyName) {
        var company = new Company();
        company.setName(companyName);
        company.setOid("companyTestOid_" + companyName);
        return Optional.of(company);
    }

    @Override
    public Optional<Post> createPost(String session, String postName) {
        var post = new Post();
        post.setName(postName);
        post.setOid("postTestOid_" + postName);
        return Optional.of(post);
    }

    @Override
    public TimexResponseDto createEmployee(String session, TimexUserDto timexDto) {
        return timexResponseDto;
    }

    @Override
    public TimexResponseDto updateEmployee(String session, Employee employee, TimexUserDto timexDto) {
        return timexResponseDto;
    }

    @Override
    public void removeEmployee(String session, String timexOid) {
        removeEmployeeCalled++;
    }

    @Override
    public List<WorkingArea> getWorkingAreas(String session, String filter) {
        return List.of(workingAreas);
    }
}
