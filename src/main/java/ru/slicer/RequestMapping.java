package ru.slicer;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class RequestMapping {
    @Inject
    private LoginRequestHandler loginRequestHandler;

    private Map<String, RequestHandler> mapping;

    @PostConstruct
    private void populate() {
        mapping = Collections.singletonMap("LOGIN_CUSTOMER", loginRequestHandler);
    }

    public RequestHandler findHandler(final String requestType) {
        return mapping.get(requestType);
    }
}
