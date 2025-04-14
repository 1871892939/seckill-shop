package cn.wolfcode.ws;

import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/{token}")
@Component
public class OrderWSServer {
    public static ConcurrentHashMap<String, Session> clients = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        System.out.println("client link===>"+token);
        clients.put(token, session);

    }

    @OnClose
    public void onClose(@PathParam("token") String token) {
        System.out.println("browser close link with server"+token);
        clients.remove(token);
    }

    @OnError
    public void onError(Throwable error) {
        error.printStackTrace();
    }
}
