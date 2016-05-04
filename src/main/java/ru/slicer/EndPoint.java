package ru.slicer;

import java.io.IOException;
import java.io.StringReader;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@javax.enterprise.context.ApplicationScoped
@ServerEndpoint("/echo")
public class EndPoint {

    //Now here is another odd problem, endpoint instances get their dependency-injection
    //wired, but resource-injection not wired. So we will have another middleman
    //to hold the resource, and @inject that middleman.
    @Inject
    private RequestMapping requestMapping;

    private Emitter emitter = new Emitter();

    @OnMessage
    public void onMessage(final String message, final Session session) {
        //В постановке не сказано, как отвечать на запрос, НЕ являющийся запросом аутентификации
        //(успешным или нет), или на неверно построенный запрос в т.ч.
        //Будем придумывать сами...
        final JsonObject jsonObject;
        try {
            jsonObject = Json.createReader(new StringReader(message)).readObject();
        } catch (Exception e) {
            //на такое ответ вообще слать не будем, а в лог положим текст.
            //лучше было б положить только первые N символов, но так нагляднее:
            System.out.println(message);
            e.printStackTrace();
            return;
        }

        final String requestType = jsonObject.getString("type");
        if (requestType == null) { //считаем частным случаем неверного формата
            System.out.println(message);
            return;
        }

        final String sid = jsonObject.getString("sequence_id");
        final RequestHandler requestHandler = requestMapping.findHandler(requestType);
        if (requestHandler == null) {
            emitter.sendErrorResponse(session, message, sid,
                    "Unknown request type", "customer.invalidRequestType");
        } else {
            try {
                requestHandler.processRequest(sid, jsonObject.getJsonObject("data"), session, emitter);
            } catch (ExpectedException e) {
                emitter.sendErrorResponse(session, message, sid,
                        e.getDescription(), e.getErrorCode());
            } catch (Exception e) {
                emitter.sendErrorResponse(session, message, sid,
                        "Unexpected error", "customer.unexpectedError");
            }
        }
    }
}
