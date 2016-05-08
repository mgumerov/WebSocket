package ru.slicer;

import ru.slicer.model.Token;
import sun.rmi.runtime.Log;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceContext;
import javax.websocket.Session;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

@ApplicationScoped
public class LoginRequestHandler implements RequestHandler {
    //Just for fun's sake, make processing asynchronous, simply because we can!
    //In HTTP 1.1, we no longer have to keep a thread associated with a connection while we process a request;
    //the same goes for WebSockets
    final private ArrayBlockingQueue<LoginRequest> queue = new ArrayBlockingQueue<>(3);
    private List<Thread> workers; //immutable

    @Override
    public void processRequest(final String sequenceId, final JsonObject data, final Session session,
                               final Emitter emitter) throws ExpectedException {
        queue.add(new LoginRequest(sequenceId, data, session, emitter));
        //throw new ExpectedException("Not implemented", "customer.NotImplemented");
    }

    private static class LoginRequest {
        public final String sequenceId;
        public final JsonObject data;
        public final Session session;
        public final Emitter emitter;
        public LoginRequest(final String sequenceId, final JsonObject data, final Session session,
                            final Emitter emitter) {
            this.sequenceId = sequenceId;
            this.data = data;
            this.session = session;
            this.emitter = emitter;
        }
    }

    @Inject
    private Service service;

    @PostConstruct
    private void startThreads() {
        final Runnable runnable = (() -> {
            try {
                while (true) {
                    final LoginRequest rq = queue.take();
                    //неэффективно но я все равно собираюсь переделывать на нормальный логгер
                    System.out.println(Thread.currentThread() + ": " + rq.data);

                    final Token token;
                    try {
                        token = service.login(rq.data.getString("email"), rq.data.getString("password"));
                    } catch (ExpectedException e) {
                        rq.emitter.sendErrorResponse(rq.session, rq.data.toString() /* пока подход такой,
                        что мы выдаем в лог в случае неуспеха отправки ответа, и выдаем исходное сообщение - ну,
                        в данной точке совсем уж исходного нет, но есть его исходное поле data. */,
                                rq.sequenceId, e.getDescription(), e.getErrorCode());
                        continue;
                    }

                    rq.emitter.sendData(rq.session, rq.sequenceId, "CUSTOMER_API_TOKEN",
                            Json.createObjectBuilder()
                                    .add("api_token", token.getId())
                                    .add("api_token_expiration_date",
                                            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(token.getExpirationDate()))
                                    .build());
                }
            } catch (InterruptedException e) {
                System.out.println(Thread.currentThread() + ": quitting");
            }
        });

        {
            final List<Thread> list = new ArrayList<>();
            //I believe we better have 2 of those guys, 'cause our Endpoint should be really fast, leaving
            //all the work to them
            list.add(new Thread(runnable));
            list.add(new Thread(runnable));
            workers = Collections.unmodifiableList(list);
        }
        for (final Thread worker : workers) {
            worker.start();
        }
    }

    @PreDestroy
    private void stopThreads() {
        for (final Thread worker : workers) {
            worker.interrupt();
        }
    }
}
