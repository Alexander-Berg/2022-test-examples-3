package ru.yandex.market.checkout.pushapi.service;

import ru.yandex.market.checkout.common.web.CustomDispatcherServlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by oroboros on 13.11.14.
 */
public class WrappingDispatcherServlet extends CustomDispatcherServlet {
    @Override
    protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        long start = System.currentTimeMillis();
        super.doDispatch(request, response);
        GraphiteReporterService.bean().getHistogram().update(System.currentTimeMillis() - start);
    }
}
