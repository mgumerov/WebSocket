package ru.slicer;

import ru.slicer.model.Token;
import sun.rmi.runtime.Log;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.websocket.Session;
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

                    //for debugging; in log4j we could add condition "if isDebugEnabled"
                    {
                        final List<Token> dump = service.dump(rq.data.getString("email"));
                        System.out.println(">> Tokens for this user:");
                        for (final Token t2 : dump) {
                            System.out.print(t2.getId());
                            System.out.print(" till ");
                            System.out.println(t2.getExpirationDate());
                        }
                        System.out.println("<<");
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

        //Что касается количества потоков, хотел поднять еще один вопрос. Если система действительно высоконагруженная,
        //то мы можем упереться в то, что последовательные запросы в БД выполняются недостаточно быстро. К примеру,
        //недавно я проводил тестирование на HBase (конечно, тут HBase явно не к месту, но все же) и один поток мог
        //сделать лишь 120 или около того запросов в секунду. Т.е. если нам нужно 1000 логинов в секунду обрабатывать,
        //нужно 10 потоков отвести только под это, хотя конечно они часть времени будут простаивать в ожидании ответа
        //по сети; а ведь приложение не только запросы логина обслуживает, а развернуть сотню потоков на одной машине
        //это довольно жадно. То есть нужно либо масштабировать решение на две машины - очевидное решение, и хорошо,
        //что с этим вроде проблем быть не должно, - либо еще мне пришел в голову вот какой вариант. Можно накапливать
        //запросы на логин, и когда достигнут определенный размер блока (или прошел некий интервал времени), их
        //обрабатывать оптом (все сразу или по несколько штук). При этом они все пойдут через один connection, в
        //одной транзакции, и можно будет одним select вытащить user-ов сразу по нескольким запросам, и аналогично
        //сделать insert тоже сразу по нескольким запросам (правда, надо будет следить за возможностью того, что будут
        //запросы по одному и тому же юзеру - убедиться, что мы это обработаем корректно). Тогда если запросы приходят
        //очень часто, это ускорит среднее время их обработки (а если редко, то замедлит, но не более чем на указанный
        //интервал времени). Но сейчас я это реализовывать не стану.
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
