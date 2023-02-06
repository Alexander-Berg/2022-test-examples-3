package ru.yandex.market.sberlog_tms.TestController;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Strakhov Artem <a href="mailto:dukeartem@yandex-team.ru"></a>
 * @date 11.11.19
 */
@SuppressWarnings("serial")
public class TestController extends HttpServlet {
    private static final String METHOD_PATCH = "PATCH";

    @Override
    protected void doPut(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {

        StringBuilder answer = new StringBuilder();

        if (request.getHeader("X-Ya-Service-Ticket") != null) {
            if (!request.getHeader("X-Ya-Service-Ticket").equals("AAAA:BBBB")) {
                answer.append("{ \"status put\": \"bad\"}");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } else {
                answer.append("{ \"status put\": \"ok\"}");
                response.setStatus(HttpServletResponse.SC_OK);
            }
        }

        System.out.println(answer.toString());

        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        response.getWriter().println(answer.toString());
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws IOException {
        String answer = "{ \"status post\": \"ok\"}";
        System.out.println(answer);

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding("utf-8");
        response.getWriter().println(answer);
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        StringBuilder answer = new StringBuilder();
        if (request.getRequestURI().startsWith("bad-request")) {
            answer.append("{ \"status get\": \"bad\"}");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        } else {
            answer.append("{ \"status get\": \"ok\"}");
            response.setStatus(HttpServletResponse.SC_OK);
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        response.getWriter().println(answer.toString());
        System.out.println(answer.toString());
    }

    private void doPatch(HttpServletRequest request,
                           HttpServletResponse response) throws IOException {
        StringBuilder answer = new StringBuilder();

        if (request.getHeader("X-Ya-Service-Ticket") != null) {
            if (!request.getHeader("X-Ya-Service-Ticket").equals("AAAA:BBBB")) {
                answer.append("{ \"status patch\": \"bad, tvm ticket not found\"}");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } else {
                answer.append("{ \"status patch\": \"ok, tvm ticket found\"}");
                response.setStatus(HttpServletResponse.SC_OK);
            }
        } else {
            answer.append("{ \"status patch\": \"ok, without tvm\"}");
            response.setStatus(HttpServletResponse.SC_OK);
        }
        System.out.println(answer);

        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        response.getWriter().println(answer);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String method = req.getMethod();

        if (method.equals(METHOD_PATCH)) {
            doPatch(req, resp);
        } else {
            super.service(req, resp);
        }
    }

}
